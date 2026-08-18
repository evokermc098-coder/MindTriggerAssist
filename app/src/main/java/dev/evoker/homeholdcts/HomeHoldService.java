// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeHoldService extends Service {

    private static final String TAG = "HomeHoldCTS";

    public static final String ACTION_STOP =
            "dev.evoker.homeholdcts.STOP";
    public static final String ACTION_RECONNECT_LOGCAT =
            "dev.evoker.homeholdcts.RECONNECT_LOGCAT";

    private static final String CHANNEL =
            WatcherNotificationHelper.CHANNEL;
    private static final int NOTIFICATION_ID =
            WatcherNotificationHelper.WATCHER_NOTIFICATION_ID;

    private static final String ACTION_TEXT =
            "act=heytap.intent.action.ACTIVATE_SPEECH_ASSIST";
    private static final String COMPONENT_TEXT =
            "cmp=com.heytap.speechassist/.core.SpeechService";
    private static final String POWER_LONG_PRESS_TEXT =
            "Detect long press KEYCODE_POWER";
    private static final String POWER_MONITOR_TEXT =
            "PowerKey:onLongPress";
    private static final String SESSION_PROBE_PREFIX =
            "HOMEHOLD_LOG_SESSION_PROBE_";

    private static final long POWER_MARKER_WINDOW_MS = 500L;
    private static final long DEBOUNCE_MS = 2500L;
    private static final long AUTO_RECOVERY_SURFACE_CHECK_MS = 1400L;

    private final Object reconnectLock = new Object();
    private final Object logcatLock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean running;
    private volatile boolean reconnectRequested;
    private volatile boolean reconnectInFlight;
    private volatile boolean autoRecoveryAttemptedForCurrentLoss;

    private volatile int logSessionState =
            WatcherIpc.STATE_STOPPED;

    private volatile Process logcatProcess;
    private volatile long lastTriggerElapsed;
    private volatile long lastPowerLongPressElapsed;

    private volatile int runtimeDelayMs =
            MainActivity.DEFAULT_CTS_DELAY_MS;
    private volatile boolean runtimeVibrate =
            MainActivity.DEFAULT_VIBRATE_ON_CTS;
    private volatile boolean runtimeSound =
            MainActivity.DEFAULT_SOUND_ON_ACTIVATION;
    private volatile boolean runtimePowerGemini =
            MainActivity.DEFAULT_POWER_GEMINI_EXPERIMENTAL;

    private ExecutorService executor;
    private ActivationRunner activationRunner;
    private WindowManager windowManager;
    private View keepAliveOverlay;

    private Messenger clientMessenger;

    private final Messenger incomingMessenger =
            new Messenger(new IncomingHandler());

    private final class IncomingHandler extends Handler {
        IncomingHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WatcherIpc.MSG_REGISTER_CLIENT:
                    clientMessenger = msg.replyTo;
                    sendStateToClient();
                    break;

                case WatcherIpc.MSG_UNREGISTER_CLIENT:
                    clientMessenger = null;
                    break;

                case WatcherIpc.MSG_RECONNECT_LOGCAT:
                    requestLogSessionReconnect();
                    break;

                case WatcherIpc.MSG_STOP_WATCHER:
                    stopWatcherFromCommand();
                    break;

                case WatcherIpc.MSG_SYNC_PREFS:
                    applyRuntimePrefs(msg.getData());
                    break;

                case WatcherIpc.MSG_REQUEST_STATE:
                    sendStateToClient();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!isEnabled()) {
            stopSelf();
            return;
        }

        loadRuntimePrefsFromDisk();
        createNotificationChannel();

        setLogSessionState(
                WatcherIpc.STATE_NEEDS_RECONNECT);

        startForeground(
                NOTIFICATION_ID,
                WatcherNotificationHelper.buildWatcherNotification(
                        this,
                        WatcherIpc.STATE_NEEDS_RECONNECT));

        ensureKeepAliveOverlay();

        activationRunner = new ActivationRunner(this);
        executor = Executors.newSingleThreadExecutor();

        running = true;
        executor.execute(this::watchForever);

        // Recovery is integrated in V7: after reboot/process recreation,
        // make at most one foreground-surface attempt.
        mainHandler.postDelayed(
                () -> maybeAutoRecoverLogSession("service-start"),
                900L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null
                && ACTION_STOP.equals(intent.getAction())) {
            stopWatcherFromCommand();
            return START_NOT_STICKY;
        }

        if (!isEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        ensureKeepAliveOverlay();

        if (!running
                && executor != null
                && !executor.isShutdown()) {
            running = true;
            executor.execute(this::watchForever);
        }

        if (intent != null
                && ACTION_RECONNECT_LOGCAT.equals(
                        intent.getAction())) {
            requestLogSessionReconnect();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return incomingMessenger.getBinder();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (isEnabled()) {
            RestartReceiver.scheduleRestart(this, 1200L);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        running = false;

        synchronized (reconnectLock) {
            reconnectRequested = true;
            reconnectLock.notifyAll();
        }

        if (activationRunner != null) {
            activationRunner.shutdown();
            activationRunner = null;
        }

        destroyCurrentLogcatAndWait();

        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(
                        300,
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        removeKeepAliveOverlay();
        setLogSessionState(WatcherIpc.STATE_STOPPED);

        if (isEnabled()) {
            RestartReceiver.scheduleRestart(this, 1500L);
        }

        super.onDestroy();
    }

    private void stopWatcherFromCommand() {
        running = false;
        getSharedPreferences(
                MainActivity.PREFS,
                MODE_PRIVATE)
                .edit()
                .putBoolean(
                        MainActivity.PREF_ENABLED,
                        false)
                .commit();
        stopSelf();
    }

    private boolean isEnabled() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        MainActivity.PREFS,
                        MODE_PRIVATE);

        return prefs.getBoolean(
                        MainActivity.PREF_ENABLED,
                        false)
                && prefs.getBoolean(
                        MainActivity.PREF_TERMS_ACCEPTED,
                        false);
    }

    private boolean hasReadLogsPermission() {
        return checkSelfPermission(
                "android.permission.READ_LOGS")
                == android.content.pm.PackageManager
                        .PERMISSION_GRANTED;
    }

    private boolean isAutomaticRecoveryAvailable() {
        return Build.VERSION.SDK_INT < 23
                || Settings.canDrawOverlays(this);
    }

    private void loadRuntimePrefsFromDisk() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        MainActivity.PREFS,
                        MODE_PRIVATE);

        runtimeDelayMs =
                clampDelay(
                        prefs.getInt(
                                MainActivity.PREF_CTS_DELAY_MS,
                                MainActivity.DEFAULT_CTS_DELAY_MS));

        runtimeVibrate =
                prefs.getBoolean(
                        MainActivity.PREF_VIBRATE_ON_CTS,
                        MainActivity.DEFAULT_VIBRATE_ON_CTS);

        runtimeSound =
                prefs.getBoolean(
                        MainActivity.PREF_SOUND_ON_ACTIVATION,
                        MainActivity.DEFAULT_SOUND_ON_ACTIVATION);

        runtimePowerGemini =
                prefs.getBoolean(
                        MainActivity.PREF_POWER_GEMINI_EXPERIMENTAL,
                        MainActivity.DEFAULT_POWER_GEMINI_EXPERIMENTAL);
    }

    private void applyRuntimePrefs(Bundle data) {
        if (data == null) {
            return;
        }

        runtimeDelayMs =
                clampDelay(
                        data.getInt(
                                WatcherIpc.KEY_DELAY_MS,
                                runtimeDelayMs));

        runtimeVibrate =
                data.getBoolean(
                        WatcherIpc.KEY_VIBRATE,
                        runtimeVibrate);

        runtimeSound =
                data.getBoolean(
                        WatcherIpc.KEY_SOUND,
                        runtimeSound);

        runtimePowerGemini =
                data.getBoolean(
                        WatcherIpc.KEY_POWER_GEMINI,
                        runtimePowerGemini);
    }

    private int clampDelay(int value) {
        return Math.max(
                0,
                Math.min(
                        MainActivity.MAX_CTS_DELAY_MS,
                        value));
    }

    private void requestLogSessionReconnect() {
        if (!hasReadLogsPermission()) {
            reconnectInFlight = false;
            setLogSessionState(
                    WatcherIpc.STATE_NO_PERMISSION);
            return;
        }

        int current = logSessionState;

        if (current == WatcherIpc.STATE_ACTIVE
                || current == WatcherIpc.STATE_CONNECTING
                || reconnectInFlight) {
            return;
        }

        reconnectInFlight = true;
        destroyCurrentLogcatAndWait();

        synchronized (reconnectLock) {
            reconnectRequested = true;
            reconnectLock.notifyAll();
        }

        setLogSessionState(
                WatcherIpc.STATE_CONNECTING);
    }

    private void watchForever() {
        while (running && isEnabled()) {
            waitForReconnectRequest();

            if (!running || !isEnabled()) {
                break;
            }

            if (!hasReadLogsPermission()) {
                reconnectInFlight = false;
                setLogSessionState(
                        WatcherIpc.STATE_NO_PERMISSION);
                continue;
            }

            runOneLogSession();
        }

        if (!running || !isEnabled()) {
            setLogSessionState(
                    WatcherIpc.STATE_STOPPED);
        }
    }

    private void waitForReconnectRequest() {
        synchronized (reconnectLock) {
            while (running
                    && isEnabled()
                    && !reconnectRequested) {
                try {
                    reconnectLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            reconnectRequested = false;
        }
    }

    private void runOneLogSession() {
        String probe =
                SESSION_PROBE_PREFIX
                        + SystemClock.elapsedRealtime();

        boolean sessionWasActive = false;

        try {
            ProcessBuilder pb =
                    new ProcessBuilder(
                            "logcat",
                            "-b", "all",
                            "-v", "brief",
                            "-T", "1",
                            "ActivityManager:W",
                            "KEYLOG_SingleKeyGesture:I",
                            "KEYLOG_SinglePowerKeyMonitor:V",
                            "HomeHoldCTS:I",
                            "*:S"
                    );

            pb.redirectErrorStream(true);
            Process created = pb.start();

            synchronized (logcatLock) {
                logcatProcess = created;
            }

            long listenerStarted =
                    SystemClock.elapsedRealtime();

            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(
                                    created.getInputStream()));

            Log.i(TAG, probe);

            String line;

            while (running
                    && isEnabled()
                    && (line = br.readLine()) != null) {

                if (!sessionWasActive) {
                    sessionWasActive = true;
                    reconnectInFlight = false;
                    setLogSessionState(
                            WatcherIpc.STATE_ACTIVE);
                }

                if (line.contains(probe)) {
                    continue;
                }

                if (SystemClock.elapsedRealtime()
                        - listenerStarted < 700L) {
                    continue;
                }

                long now =
                        SystemClock.elapsedRealtime();

                if (line.contains(POWER_LONG_PRESS_TEXT)
                        || line.contains(POWER_MONITOR_TEXT)) {
                    lastPowerLongPressElapsed = now;
                    continue;
                }

                if (line.contains(ACTION_TEXT)
                        && line.contains(COMPONENT_TEXT)) {

                    if (now - lastTriggerElapsed
                            < DEBOUNCE_MS) {
                        continue;
                    }

                    lastTriggerElapsed = now;

                    boolean fromPower =
                            lastPowerLongPressElapsed > 0L
                                    && now
                                    - lastPowerLongPressElapsed >= 0L
                                    && now
                                    - lastPowerLongPressElapsed
                                    <= POWER_MARKER_WINDOW_MS;

                    if (fromPower
                            && runtimePowerGemini) {
                        if (activationRunner != null) {
                            activationRunner
                                    .activateAssistantSession(
                                            runtimeDelayMs,
                                            runtimeVibrate,
                                            runtimeSound);
                        }
                    } else {
                        if (activationRunner != null) {
                            activationRunner
                                    .activateCts(
                                            runtimeDelayMs,
                                            runtimeVibrate,
                                            runtimeSound);
                        }
                    }
                }
            }

            try {
                br.close();
            } catch (Throwable ignored) {
            }

        } catch (Throwable e) {
            Log.e(
                    TAG,
                    "Logcat watcher session failed",
                    e);

        } finally {
            destroyCurrentLogcatAndWait();
            reconnectInFlight = false;

            if (running && isEnabled()) {
                setLogSessionState(
                        WatcherIpc.STATE_NEEDS_RECONNECT);

                // Only a previously ACTIVE session gets automatic recovery.
                // A deny/timeout while CONNECTING must not loop.
                if (sessionWasActive) {
                    mainHandler.post(
                            () -> maybeAutoRecoverLogSession(
                                    "active-session-lost"));
                }
            }
        }
    }

    private void maybeAutoRecoverLogSession(String reason) {
        if (!running
                || !isEnabled()
                || !hasReadLogsPermission()
                || !isAutomaticRecoveryAvailable()
                || logSessionState
                        != WatcherIpc.STATE_NEEDS_RECONNECT
                || autoRecoveryAttemptedForCurrentLoss) {
            return;
        }

        autoRecoveryAttemptedForCurrentLoss = true;

        try {
            Intent open =
                    new Intent(
                            this,
                            LogSessionBridgeActivity.class);

            open.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(open);

            Log.i(
                    TAG,
                    "Automatic log recovery surfaced LogSessionBridgeActivity once. reason="
                            + reason);

        } catch (Throwable e) {
            Log.w(
                    TAG,
                    "Automatic recovery Activity start failed; "
                            + "notification fallback remains.",
                    e);
        }

        mainHandler.postDelayed(
                () -> {
                    if (running
                            && isEnabled()
                            && logSessionState
                                    == WatcherIpc.STATE_NEEDS_RECONNECT) {
                        updateWatcherNotification();
                    }
                },
                AUTO_RECOVERY_SURFACE_CHECK_MS);
    }

    private void destroyCurrentLogcatAndWait() {
        Process p;

        synchronized (logcatLock) {
            p = logcatProcess;
            logcatProcess = null;
        }

        if (p == null) {
            return;
        }

        try {
            p.destroy();
        } catch (Throwable ignored) {
        }

        try {
            if (!p.waitFor(
                    450L,
                    TimeUnit.MILLISECONDS)) {
                try {
                    p.destroyForcibly();
                } catch (Throwable ignored) {
                }

                try {
                    p.waitFor(
                            250L,
                            TimeUnit.MILLISECONDS);
                } catch (Throwable ignored) {
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable ignored) {
        }
    }

    private void setLogSessionState(int state) {
        if (logSessionState == state) {
            return;
        }

        logSessionState = state;

        if (state == WatcherIpc.STATE_ACTIVE) {
            autoRecoveryAttemptedForCurrentLoss = false;
        }

        sendStateToClient();
        updateWatcherNotification();
    }

    private void sendStateToClient() {
        Messenger client = clientMessenger;

        if (client == null) {
            return;
        }

        Message msg =
                Message.obtain(
                        null,
                        WatcherIpc.MSG_STATE_CHANGED);

        msg.arg1 = logSessionState;

        try {
            client.send(msg);
        } catch (RemoteException e) {
            clientMessenger = null;
        }
    }

    private void updateWatcherNotification() {
        try {
            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE);

            if (nm != null) {
                nm.notify(
                        NOTIFICATION_ID,
                        WatcherNotificationHelper
                                .buildWatcherNotification(
                                        this,
                                        logSessionState));
            }
        } catch (Throwable ignored) {
        }
    }

    private void ensureKeepAliveOverlay() {
        if (keepAliveOverlay != null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && !Settings.canDrawOverlays(this)) {
            return;
        }

        try {
            windowManager =
                    (WindowManager)
                            getSystemService(
                                    WINDOW_SERVICE);

            if (windowManager == null) {
                return;
            }

            View v = new View(this);
            v.setAlpha(0.01f);

            int type;

            if (Build.VERSION.SDK_INT >= 26) {
                type =
                        WindowManager.LayoutParams
                                .TYPE_APPLICATION_OVERLAY;
            } else {
                type =
                        WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams lp =
                    new WindowManager.LayoutParams(
                            1,
                            1,
                            type,
                            WindowManager.LayoutParams
                                            .FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams
                                            .FLAG_NOT_TOUCHABLE
                                    | WindowManager.LayoutParams
                                            .FLAG_LAYOUT_NO_LIMITS,
                            PixelFormat.TRANSLUCENT);

            lp.gravity =
                    Gravity.TOP | Gravity.START;

            windowManager.addView(v, lp);
            keepAliveOverlay = v;

        } catch (Throwable e) {
            Log.w(
                    TAG,
                    "Unable to attach keep-alive overlay",
                    e);
        }
    }

    private void removeKeepAliveOverlay() {
        View v = keepAliveOverlay;
        keepAliveOverlay = null;

        if (v != null
                && windowManager != null) {
            try {
                windowManager.removeView(v);
            } catch (Throwable ignored) {
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE);

            if (nm != null) {
                NotificationChannel ch =
                        new NotificationChannel(
                                CHANNEL,
                                UiText.tr(this, "MindTrigger Assist watcher"),
                                NotificationManager.IMPORTANCE_LOW);

                ch.setDescription(
                        UiText.tr(
                                this,
                                "Keeps the isolated foreground-service watcher available for trigger detection and log-session recovery."));
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }
}
