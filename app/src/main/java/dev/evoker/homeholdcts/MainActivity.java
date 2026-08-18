// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    static final String PREFS = "home_hold_cts";
    static final String PREF_ENABLED = "enabled";
    static final String PREF_TERMS_ACCEPTED = "terms_privacy_accepted_v5";
    static final String PREF_CTS_DELAY_MS = "cts_delay_ms";
    static final String PREF_VIBRATE_ON_CTS = "vibrate_on_cts";
    static final String PREF_SOUND_ON_ACTIVATION = "sound_on_activation";
    static final String PREF_POWER_GEMINI_EXPERIMENTAL =
            "power_gemini_experimental";

    static final boolean DEFAULT_VIBRATE_ON_CTS = true;
    static final boolean DEFAULT_SOUND_ON_ACTIVATION = true;
    static final boolean DEFAULT_POWER_GEMINI_EXPERIMENTAL = true;
    static final int DEFAULT_CTS_DELAY_MS = 200;
    static final int MAX_CTS_DELAY_MS = 1000;

    private static final String PREF_GESTURE_CONFIRMED = "guide_gesture_confirmed";
    private static final String PREF_RECENTS_LOCKED = "guide_recents_locked";
    private static final String PREF_GOOGLE_AUTOSTART = "guide_google_autostart";
    private static final String PREF_GEMINI_AUTOSTART = "guide_gemini_autostart";

    private static final int TAB_SETUP = 1;
    private static final int TAB_ADVANCED = 2;
    private static final int TAB_SUPPORT = 3;
    private static final int TAB_ABOUT = 4;
    private static final String STATE_SELECTED_TAB = "selected_tab";
    private static final String EXTRA_RECREATE_TAB =
            "dev.evoker.homeholdcts.extra.RECREATE_TAB";

    private int currentTab = TAB_SETUP;

    private final Handler ui = new Handler(Looper.getMainLooper());


private Messenger watcherMessenger;
private boolean watcherBound;
private boolean activityResumed;
private int watcherSessionState =
        WatcherIpc.STATE_STOPPED;

private final Messenger uiMessenger =
        new Messenger(
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what
                                == WatcherIpc.MSG_STATE_CHANGED) {
                            watcherSessionState = msg.arg1;
                            refreshAll();
                            return;
                        }

                        super.handleMessage(msg);
                    }
                });

private final ServiceConnection watcherConnection =
        new ServiceConnection() {
            @Override
            public void onServiceConnected(
                    ComponentName name,
                    IBinder binder) {

                watcherMessenger =
                        new Messenger(binder);
                watcherBound = true;

                sendWatcherMessage(
                        WatcherIpc.MSG_REGISTER_CLIENT,
                        null,
                        true);

                syncWatcherPrefs();

                sendWatcherMessage(
                        WatcherIpc.MSG_REQUEST_STATE,
                        null,
                        false);

                if (activityResumed) {
                    ui.postDelayed(
                            () -> requestLogSessionReconnect(false),
                            220L);
                }
            }

            @Override
            public void onServiceDisconnected(
                    ComponentName name) {

                watcherBound = false;
                watcherMessenger = null;
                watcherSessionState =
                        WatcherIpc.STATE_NEEDS_RECONNECT;
                refreshAll();
            }
        };

    private int primary;
    private int onPrimary;
    private int primaryContainer;
    private int onPrimaryContainer;
    private int surface;
    private int surfaceContainer;
    private int surfaceContainerHigh;
    private int onSurface;
    private int onSurfaceVariant;
    private int outline;
    private int success;
    private int warning;
    private int error;

    private FirstRunBootstrap bootstrap;

    private FrameLayout contentHost;
    private View setupPage;
    private View advancedPage;
    private View supportPage;
    private View aboutPage;

    private TextView heroTitle;
    private TextView heroSubtitle;

    private TextView step1Status;
    private TextView step2Status;
    private TextView step3Status;
    private TextView criticalStatus;
    private TextView gestureStatus;

    private LinearLayout gestureSetupBlock;
    private LinearLayout gestureConfirmedBlock;
    private LinearLayout recentsSetupBlock;
    private LinearLayout recentsConfirmedBlock;
    private LinearLayout autoLaunchSetupBlock;
    private LinearLayout autoLaunchConfirmedBlock;

    private MaterialButton shizukuButton;
    private MaterialButton pcButton;
    private LinearLayout step1Methods;

    private ActionRow readLogsRow;
    private ActionRow logSessionRow;
    private ActionRow overlayRow;
    private ActionRow selfBatteryRow;
    private ActionRow googleBatteryRow;
    private ActionRow geminiBatteryRow;
    private ActionRow googleAssistantRow;

    private MaterialButton recentsOpenButton;
    private MaterialButton recentsConfirmButton;
    private MaterialButton autoLaunchOpenButton;
    private MaterialButton autoLaunchConfirmButton;
    private MaterialButton runButton;

    private TextView commandLogView;
    private TextView commandTimeView;

    private final Shizuku.OnBinderReceivedListener binderListener = () -> {
        if (bootstrap != null && termsAccepted()) bootstrap.beginIfNeeded();
    };

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> {
                if (bootstrap != null) {
                    bootstrap.onPermissionResult(requestCode, grantResult);
                    runOnUiThread(this::refreshAll);
                }
            };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrap(ThemeManager.wrap(newBase)));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        int restoredTab = TAB_SETUP;
        Intent launchIntent = getIntent();
        if (launchIntent != null
                && launchIntent.hasExtra(EXTRA_RECREATE_TAB)) {
            restoredTab = launchIntent.getIntExtra(
                    EXTRA_RECREATE_TAB,
                    TAB_SETUP);
            // This marker is only for the current recreate() cycle.
            launchIntent.removeExtra(EXTRA_RECREATE_TAB);
        } else if (state != null) {
            restoredTab = state.getInt(STATE_SELECTED_TAB, TAB_SETUP);
        }
        if (restoredTab >= TAB_SETUP && restoredTab <= TAB_ABOUT) {
            currentTab = restoredTab;
        }

        loadColors();
        configureWindow();

        Shizuku.addBinderReceivedListenerSticky(binderListener);
        Shizuku.addRequestPermissionResultListener(permissionListener);

        bootstrap = new FirstRunBootstrap(this, new FirstRunBootstrap.Callback() {
            @Override
            public void onState(String state) {
                runOnUiThread(() -> {
                    if (commandLogView != null) {
                        commandLogView.setText(bootstrap.getLastCommandLog());
                    }
                    updateLastCommandTime();
                    refreshAll();
                });
            }

            @Override
            public void onLog(String log) {
                runOnUiThread(() -> {
                    if (commandLogView != null) commandLogView.setText(log);
                    updateLastCommandTime();
                    refreshAll();
                });
            }
        });

        setContentView(buildRoot());
        showTab(currentTab);
        refreshAll();

        if (termsAccepted()) {
            ui.postDelayed(bootstrap::beginIfNeeded, 350L);
        } else {
            ui.postDelayed(this::showFirstRunTerms, 250L);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, currentTab);
        super.onSaveInstanceState(outState);
    }

@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);

    if (intent != null
            && intent.getBooleanExtra(
                    "homehold_auto_log_recovery",
                    false)) {

        ui.postDelayed(
                () -> requestLogSessionReconnect(true),
                220L);
    }
}

@Override
protected void onStart() {
    super.onStart();

    if (termsAccepted()
            && getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_ENABLED, false)) {
        bindWatcher();
    }
}

@Override
protected void onResume() {
    super.onResume();
    activityResumed = true;
    refreshAll();

    if (termsAccepted()
            && getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_ENABLED, false)) {

        startWatcher();
        bindWatcher();

        ui.postDelayed(
                () -> requestLogSessionReconnect(false),
                220L);
    }

    statusPollRemaining = 40;
    ui.removeCallbacks(pcDetectLoop);
    ui.post(pcDetectLoop);
}

@Override
protected void onPause() {
    activityResumed = false;
    ui.removeCallbacks(pcDetectLoop);
    super.onPause();
}

@Override
protected void onStop() {
    unbindWatcher();
    super.onStop();
}

    @Override
    protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }

    private int statusPollRemaining;

    private final Runnable pcDetectLoop = new Runnable() {
        @Override
        public void run() {
            refreshAll();

            int session =
                    watcherSessionState;

            boolean keepPolling =
                    !isReadLogsGranted()
                            || (isWatcherRunning()
                                && session != WatcherIpc.STATE_ACTIVE);

            if (watcherBound) {
                sendWatcherMessage(WatcherIpc.MSG_REQUEST_STATE, null, false);
            }

            if (statusPollRemaining-- > 0
                    && keepPolling) {
                ui.postDelayed(this, 500L);
            }
        }
    };

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surface);

        contentHost = new FrameLayout(this);

        setupPage = buildSetupPage();
        advancedPage = buildAdvancedPage();
        supportPage = buildSupportPage();
        aboutPage = buildAboutPage();

        contentHost.addView(setupPage, matchFrame());
        contentHost.addView(advancedPage, matchFrame());
        contentHost.addView(supportPage, matchFrame());
        contentHost.addView(aboutPage, matchFrame());

        root.addView(contentHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        BottomNavigationView nav = new BottomNavigationView(this);
        nav.setBackgroundColor(surfaceContainer);
        nav.setItemIconTintList(makeNavColorStateList());
        nav.setItemTextColor(makeNavColorStateList());
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        nav.setItemActiveIndicatorEnabled(true);
        nav.setItemActiveIndicatorColor(
                ColorStateList.valueOf(primaryContainer));
        nav.setElevation(0f);
        nav.setTranslationZ(0f);

        Menu menu = nav.getMenu();
        menu.add(Menu.NONE, TAB_SETUP, Menu.NONE, tr("Setup"))
                .setIcon(R.drawable.ic_setup);
        menu.add(Menu.NONE, TAB_ADVANCED, Menu.NONE, tr("Advanced"))
                .setIcon(R.drawable.ic_advanced);
        menu.add(Menu.NONE, TAB_SUPPORT, Menu.NONE, tr("Support me"))
                .setIcon(R.drawable.ic_support);
        menu.add(Menu.NONE, TAB_ABOUT, Menu.NONE, tr("About"))
                .setIcon(R.drawable.ic_about);

        nav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        nav.setSelectedItemId(currentTab);

        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private View buildSetupPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();

        addTopBar(root, "MindTrigger Assist", "Setup");
        addHero(root);
        addCriticalRequirements(root);

        TextView setupLabel = sectionEyebrow("SETUP");
        root.addView(setupLabel, margins(4, 10, 0, 10));

        addGestureRequirement(root);
        addStep1(root);
        addStep2(root);
        addStep3(root);
        addRunCard(root);

        scroll.addView(root);
        return scroll;
    }

    private View buildAdvancedPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();

        addTopBar(root, "Advanced", "Runtime & diagnostics");

        MaterialCardView runtime = sectionCard();
        LinearLayout runtimeBody = sectionBody();

        TextView title = text("Runtime", 20, onSurface, Typeface.BOLD);
        runtimeBody.addView(title);

        TextView desc = supporting(
                "Adjust activation delay, haptic feedback and the local success sound used by " +
                "Circle to Search and the Assistant session. Changes apply immediately.");
        runtimeBody.addView(desc, margins(0, 4, 0, 12));

        int current = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_CTS_DELAY_MS, DEFAULT_CTS_DELAY_MS);
        current = Math.max(0, Math.min(MAX_CTS_DELAY_MS, current));

        TextView delayTitle = miniTitle("Activation delay");
        runtimeBody.addView(delayTitle);

        TextView delayValue = supporting(current + " ms");
        runtimeBody.addView(delayValue, margins(0, 3, 0, 2));

        Slider slider = new Slider(this);
        slider.setValueFrom(0f);
        slider.setValueTo(MAX_CTS_DELAY_MS);
        slider.setStepSize(25f);
        slider.setValue(current);
        slider.addOnChangeListener((s, value, fromUser) -> {
            int v = Math.round(value);
            delayValue.setText(v + " ms");
            if (fromUser) {
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit().putInt(PREF_CTS_DELAY_MS, v).apply();
                syncWatcherPrefs();
            }
        });
        runtimeBody.addView(slider, margins(0, 2, 0, 8));

        MaterialSwitch vibration = new MaterialSwitch(this);
        vibration.setText(tr("Vibrate on activation"));
        vibration.setTextColor(onSurface);
        vibration.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_VIBRATE_ON_CTS, DEFAULT_VIBRATE_ON_CTS));
        vibration.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_VIBRATE_ON_CTS, checked)
                    .apply();
            syncWatcherPrefs();
        });
        runtimeBody.addView(vibration);

        MaterialSwitch activationSound = new MaterialSwitch(this);
        activationSound.setText(tr("Activation sound"));
        activationSound.setTextColor(onSurface);
        activationSound.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_SOUND_ON_ACTIVATION, DEFAULT_SOUND_ON_ACTIVATION));
        activationSound.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_SOUND_ON_ACTIVATION, checked)
                    .apply();
            syncWatcherPrefs();
        });
        runtimeBody.addView(activationSound, margins(0, 4, 0, 0));

        runtime.addView(runtimeBody);
        root.addView(runtime, margins(0, 0, 0, 14));

        MaterialCardView triggers = sectionCard();
        LinearLayout triggersBody = sectionBody();

        triggersBody.addView(text(
                "Trigger routing (experimental)",
                20,
                onSurface,
                Typeface.BOLD));

        TextView triggerDesc = supporting(
                "The isolated watcher uses one activation pipeline and routes by trigger: " +
                "Home/gesture → Circle to Search; Power → Assistant voice session.");
        triggersBody.addView(triggerDesc, margins(0, 4, 0, 12));

        MaterialSwitch powerGemini = new MaterialSwitch(this);
        powerGemini.setText(tr("Power long press → Assistant voice session"));
        powerGemini.setTextColor(onSurface);
        powerGemini.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(
                        PREF_POWER_GEMINI_EXPERIMENTAL,
                        DEFAULT_POWER_GEMINI_EXPERIMENTAL));
        powerGemini.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_POWER_GEMINI_EXPERIMENTAL, checked)
                    .apply();
            syncWatcherPrefs();
        });
        triggersBody.addView(powerGemini);

        TextView recoveryBuiltIn = supporting(
                "Automatic recovery uses a transparent bridge Activity. If an active privileged logcat session is lost, the bridge briefly places the app UID in a foreground state so Android can present its own device-log access confirmation.");
        recoveryBuiltIn.setTextColor(success);
        triggersBody.addView(recoveryBuiltIn, margins(0, 10, 0, 10));

        TextView assistantTransportDesc = supporting(
                "Both branches use the same delay and haptic settings, then call the Android " +
                "VoiceInteractionManager binder interface. Power requests the active Assistant " +
                "voice session; Home/gesture sends the CTS-specific invocation bundle.");
        triggersBody.addView(assistantTransportDesc, margins(0, 6, 0, 10));

        TextView auraDesc = supporting(
                "Activation feedback uses bundled local PCM audio cues after a successful " +
                "request. No activation audio is downloaded or streamed.");
        triggersBody.addView(auraDesc, margins(0, 0, 0, 10));

        TextView markerWindow = supporting(
                "POWER classification window: 500 ms before SpeechAssist failure.");
        triggersBody.addView(markerWindow, margins(0, 10, 0, 0));

        triggers.addView(triggersBody);
        root.addView(triggers, margins(0, 0, 0, 14));

        MaterialCardView shell = sectionCard();
        LinearLayout shellBody = sectionBody();

        shellBody.addView(text("Privileged setup", 20, onSurface, Typeface.BOLD));

        TextView shellDesc = supporting(
                "Re-run privileged setup to restore READ_LOGS and reapply the configured AppOps/background-execution settings.");
        shellBody.addView(shellDesc, margins(0, 4, 0, 12));

        MaterialButton rerun = filledTonalButton("Run again with Shizuku");
        rerun.setOnClickListener(v -> bootstrap.runNow());
        shellBody.addView(rerun);

        MaterialButton copyPc = outlinedButton("Copy PC one-shot");
        copyPc.setOnClickListener(v -> copy(
                "MindTrigger Assist one-shot",
                SetupCommands.pcOneShot(getPackageName())));
        shellBody.addView(copyPc, margins(0, 8, 0, 0));

        shellBody.addView(miniTitle("Last command execution"), margins(0, 16, 0, 4));

        commandTimeView = supporting(formatLastExecutionTime());
        commandTimeView.setTextColor(onSurface);
        shellBody.addView(commandTimeView, margins(0, 0, 0, 8));

        shellBody.addView(miniTitle("Last shell result"), margins(0, 4, 0, 4));

        commandLogView = text(
                bootstrap.getLastCommandLog(),
                11,
                onSurfaceVariant,
                Typeface.NORMAL);
        commandLogView.setTypeface(Typeface.MONOSPACE);
        commandLogView.setTextIsSelectable(true);
        commandLogView.setPadding(dp(14), dp(12), dp(14), dp(12));
        commandLogView.setBackground(roundRect(surfaceContainerHigh, 18));
        shellBody.addView(commandLogView);

        MaterialButton copyLog = outlinedButton("Copy result");
        copyLog.setOnClickListener(v -> copy(
                "MindTrigger Assist shell result",
                commandLogView.getText().toString()));
        shellBody.addView(copyLog, margins(0, 8, 0, 0));

        shell.addView(shellBody);
        root.addView(shell, margins(0, 0, 0, 14));

        MaterialCardView maintenance = sectionCard();
        LinearLayout maintenanceBody = sectionBody();

        maintenanceBody.addView(text("Maintenance", 20, onSurface, Typeface.BOLD));

        TextView mDesc = supporting(
                "Reset only the OEM settings confirmations stored by MindTrigger Assist.");
        maintenanceBody.addView(mDesc, margins(0, 4, 0, 12));

        MaterialButton reset = outlinedButton("Reset manual confirmations");
        reset.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(PREF_GESTURE_CONFIRMED)
                    .remove(PREF_RECENTS_LOCKED)
                    .remove(PREF_GOOGLE_AUTOSTART)
                    .remove(PREF_GEMINI_AUTOSTART)
                    .apply();
            refreshAll();
            showTab(TAB_SETUP);
        });
        maintenanceBody.addView(reset);

        maintenance.addView(maintenanceBody);
        root.addView(maintenance);

        scroll.addView(root);
        return scroll;
    }


private View buildSupportPage() {
    ScrollView scroll = pageScroll();
    LinearLayout root = pageRoot();

    addTopBar(root, "Support me", "Support development");

    MaterialCardView hero = sectionCard();
    LinearLayout heroBody = sectionBody();

    heroBody.addView(text(
            "Support development",
            22,
            onSurface,
            Typeface.BOLD));

    TextView desc = supporting(
            "MindTrigger Assist is free and open source. The most useful support is testing, reproducible bug reports, translations, sharing the project, and code contributions.");
    heroBody.addView(desc, margins(0, 6, 0, 14));

    MaterialButton profile =
            filledTonalButton("Open project repository");
    profile.setOnClickListener(v -> {
        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/evokermc098-coder/MindTriggerAssist")));
        } catch (Throwable ignored) {}
    });
    heroBody.addView(profile);

    MaterialButton share =
            outlinedButton("Share project");
    share.setOnClickListener(v -> shareProject());
    heroBody.addView(share, margins(0, 8, 0, 0));

    MaterialButton copyHandle =
            textButton("Copy developer handle");
    copyHandle.setOnClickListener(v -> {
        copy("Developer", "@EvokerUniverse");
        Toast.makeText(
                this,
                "@EvokerUniverse",
                Toast.LENGTH_SHORT).show();
    });
    heroBody.addView(copyHandle, margins(0, 6, 0, 0));

    hero.addView(heroBody);
    root.addView(hero, margins(0, 0, 0, 14));

    MaterialCardView contribution = sectionCard();
    LinearLayout contributionBody = sectionBody();
    contributionBody.addView(text(
            "Ways to help",
            20,
            onSurface,
            Typeface.BOLD));

    contributionBody.addView(
            supporting(
                    "• Test ColorOS updates and attach reproducible logs.\n" +
                    "• Improve translations and accessibility wording.\n" +
                    "• Review GPL / third-party notices before redistribution.\n" +
                    "• Share fixes without removing upstream attribution."),
            margins(0, 8, 0, 0));

    contribution.addView(contributionBody);
    root.addView(contribution, margins(0, 0, 0, 14));

    MaterialCardView financial = sectionCard();
    LinearLayout financialBody = sectionBody();

    financialBody.addView(text(
            "Support on Ko-fi",
            22,
            onSurface,
            Typeface.BOLD));

    TextView financialDesc = supporting(
            "If MindTrigger Assist is useful to you, you can support the student maintaining it through Ko-fi.");
    financialBody.addView(
            financialDesc,
            margins(0, 6, 0, 12));

    MaterialButton kofiButton =
            filledButton("Open Ko-fi · evokeruniverse");

    kofiButton.setOnClickListener(v -> {
        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://ko-fi.com/evokeruniverse")));
        } catch (Throwable e) {
            Toast.makeText(
                    this,
                    tr("Unable to open Ko-fi."),
                    Toast.LENGTH_LONG).show();
        }
    });

    financialBody.addView(kofiButton);

    TextView kofiUrl = supporting(
            "ko-fi.com/evokeruniverse");
    kofiUrl.setTextColor(primary);
    financialBody.addView(
            kofiUrl,
            margins(0, 8, 0, 0));

    financial.addView(financialBody);
    root.addView(financial);

    scroll.addView(root);
    return scroll;
}


private View buildAboutPage() {
    ScrollView scroll = pageScroll();
    LinearLayout root = pageRoot();

    addTopBar(root, "About", "Project information");

    MaterialCardView overview = elevatedCard(primaryContainer, 28, 0f);
    LinearLayout body = column();
    body.setPadding(dp(22), dp(22), dp(22), dp(22));

    body.addView(text("MindTrigger Assist", 24, onPrimaryContainer, Typeface.BOLD));

    TextView version = text(
            tr("Version") + " " + BuildConfig.VERSION_NAME,
            13,
            onPrimaryContainer,
            Typeface.BOLD);
    version.setPadding(0, dp(4), 0, dp(12));
    body.addView(version);

    TextView desc = text(
            "Open-source ColorOS compatibility utility that maps the configured Home/gesture " +
            "long press to Circle to Search on supported ColorOS CN builds.",
            14,
            onPrimaryContainer,
            Typeface.NORMAL);
    desc.setLineSpacing(0, 1.10f);
    body.addView(desc);

    overview.addView(body);
    root.addView(overview, margins(0, 0, 0, 14));

    MaterialCardView language = sectionCard();
    LinearLayout languageBody = sectionBody();

    languageBody.addView(text("Language", 20, onSurface, Typeface.BOLD));

    TextView languageDesc = supporting(
            "Select the app interface language. Only fully maintained release languages are listed.");
    languageBody.addView(languageDesc, margins(0, 6, 0, 10));

    MaterialButton languageButton = filledTonalButton(
            LanguageManager.displayName(this, LanguageManager.get(this)));
    languageButton.setOnClickListener(v -> showLanguagePicker());
    languageBody.addView(languageButton);

    TextView translationNote = supporting(
            "Translations are maintained for the supported release languages. Technical identifiers and Android API names remain untranslated where appropriate.");
    translationNote.setTextColor(warning);
    languageBody.addView(translationNote, margins(0, 10, 0, 0));

    language.addView(languageBody);
    root.addView(language, margins(0, 0, 0, 14));

    MaterialCardView appearance = sectionCard();
    LinearLayout appearanceBody = sectionBody();
    appearanceBody.addView(text("Appearance", 20, onSurface, Typeface.BOLD));

    TextView appearanceDesc = supporting(
            "Choose the Material 3 accent and display mode. The current tab is preserved when the Activity recreates.");
    appearanceBody.addView(appearanceDesc, margins(0, 6, 0, 12));

    appearanceBody.addView(miniTitle("Theme color"));
    addChromeLikeColorPicker(appearanceBody);
    appearanceBody.addView(miniTitle("Display mode"), margins(0, 16, 0, 0));


    LinearLayout modeRow1 = weightedButtonRow();
    modeRow1.addView(themeModeButton("White", ThemeManager.MODE_LIGHT), weightedButtonParams(0));
    modeRow1.addView(themeModeButton("System", ThemeManager.MODE_SYSTEM), weightedButtonParams(8));
    appearanceBody.addView(modeRow1, margins(0, 8, 0, 0));

    LinearLayout modeRow2 = weightedButtonRow();
    modeRow2.addView(themeModeButton("Dark", ThemeManager.MODE_DARK), weightedButtonParams(0));
    modeRow2.addView(themeModeButton("Night", ThemeManager.MODE_NIGHT), weightedButtonParams(8));
    appearanceBody.addView(modeRow2, margins(0, 8, 0, 8));

    TextView modeDesc = supporting(
            "White is the default. System follows the device. Dark uses Material dark surfaces; Night uses deeper near-black surfaces.");
    appearanceBody.addView(modeDesc);

    appearance.addView(appearanceBody);
    root.addView(appearance, margins(0, 0, 0, 14));

    MaterialCardView materialNotice = sectionCard();
    LinearLayout materialBody = sectionBody();

    materialBody.addView(text(
            "Material Design attribution",
            20,
            onSurface,
            Typeface.BOLD));

    materialBody.addView(
            supporting(
                    "Google Material Components and Material Icons are used under Apache License 2.0. Their license is bundled with the source and viewable in the app."),
            margins(0, 6, 0, 0));

    materialNotice.addView(materialBody);
    root.addView(materialNotice, margins(0, 0, 0, 14));

    MaterialCardView ossLicenses = sectionCard();
    LinearLayout ossBody = sectionBody();
    ossBody.addView(text("Open-source licenses", 20, onSurface, Typeface.BOLD));
    ossBody.addView(
            supporting(
                    "View the project license and bundled notices for MiCTS, Material Components, Material Icons, Shizuku API, and AndroidHiddenApiBypass."),
            margins(0, 6, 0, 10));
    MaterialButton licensesButton = filledTonalButton("View licenses");
    licensesButton.setOnClickListener(v -> showOpenSourceLicenses());
    ossBody.addView(licensesButton);
    ossLicenses.addView(ossBody);
    root.addView(ossLicenses, margins(0, 0, 0, 14));

    MaterialCardView termsCard = sectionCard();
    LinearLayout termsCardBody = sectionBody();
    termsCardBody.addView(text("Terms & Privacy", 20, onSurface, Typeface.BOLD));
    TextView termsDesc = supporting(
            "Review local log handling, privileged setup changes, Google-side data processing, redistribution notice, and warranty disclaimer.");
    termsCardBody.addView(termsDesc, margins(0, 6, 0, 10));
    MaterialButton termsButton = filledTonalButton("Review Terms & Privacy");
    termsButton.setOnClickListener(v -> showTermsDialog(false));
    termsCardBody.addView(termsButton);
    termsCard.addView(termsCardBody);
    root.addView(termsCard, margins(0, 0, 0, 14));

    MaterialCardView credits = sectionCard();
    LinearLayout creditsBody = sectionBody();

    creditsBody.addView(text("Credits", 20, onSurface, Typeface.BOLD));

    creditsBody.addView(infoPair(
            "Developer",
            "@EvokerUniverse"
    ), margins(0, 10, 0, 0));

    creditsBody.addView(infoPair(
            "Maintainer",
            "@EvokerUniverse"
    ), margins(0, 8, 0, 0));

    creditsBody.addView(infoPair(
            "AI coding assistance",
            "Chat GPT"
    ), margins(0, 8, 0, 0));

    creditsBody.addView(infoPair(
            "Package",
            BuildConfig.APPLICATION_ID
    ), margins(0, 8, 0, 0));

    credits.addView(creditsBody);
    root.addView(credits, margins(0, 0, 0, 14));

    MaterialCardView license = sectionCard();
    LinearLayout licenseBody = sectionBody();

    licenseBody.addView(text("License", 20, onSurface, Typeface.BOLD));

    TextView licenseName = text(
            "GNU General Public License v3.0",
            15,
            onSurface,
            Typeface.BOLD);
    licenseBody.addView(licenseName, margins(0, 10, 0, 3));

    TextView licenseDesc = supporting(
            "MindTrigger Assist is free and open-source software distributed under GPL-3.0-only. " +
            "Distribution of modified or unmodified copies must comply with the GPL, including " +
            "applicable notice and corresponding-source obligations.");
    licenseBody.addView(licenseDesc);

    TextView associationNotice = supporting(
            "Content, branding, commentary, screenshots, links, or other material added by a redistributor is solely that redistributor's responsibility and does not represent the original MindTrigger Assist author. The original author assumes no responsibility for such added material, including controversial or unrelated content. This statement is informational only and does not limit rights granted by GPL-3.0-only.");
    associationNotice.setTextColor(warning);
    licenseBody.addView(associationNotice, margins(0, 10, 0, 0));

    TextView copyright = supporting(
            "MindTrigger Assist modifications © 2026 EvokerUniverse");
    copyright.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    licenseBody.addView(copyright, margins(0, 10, 0, 0));

    TextView licenseFile = supporting(
            "The complete GPL-3.0 license text is bundled with the source and is also viewable in the app.");
    licenseBody.addView(licenseFile, margins(0, 8, 0, 0));

    license.addView(licenseBody);
    root.addView(license, margins(0, 0, 0, 14));

    MaterialCardView upstream = sectionCard();
    LinearLayout upstreamBody = sectionBody();

    upstreamBody.addView(text("Upstream", 20, onSurface, Typeface.BOLD));

    upstreamBody.addView(infoPair(
            "CTS source",
            "MiCTS"
    ), margins(0, 10, 0, 0));

    upstreamBody.addView(infoPair(
            "Upstream",
            "parallelcc"
    ), margins(0, 8, 0, 0));

    upstreamBody.addView(infoPair(
            "Upstream license",
            "GPL-3.0"
    ), margins(0, 8, 0, 0));

    TextView upstreamNote = supporting(
            "The Circle to Search invocation path was implemented with reference to MiCTS. " +
            "MindTrigger Assist adds its own ColorOS trigger detection, setup, process lifecycle, " +
            "recovery flow and user interface.");
    upstreamBody.addView(upstreamNote, margins(0, 10, 0, 0));

    upstream.addView(upstreamBody);
    root.addView(upstream, margins(0, 0, 0, 14));

    MaterialCardView architecture = sectionCard();
    LinearLayout archBody = sectionBody();

    archBody.addView(text("Architecture", 20, onSurface, Typeface.BOLD));

    TextView flow = supporting(
            "Home/gesture long press → ColorOS SpeechAssist start failure → :watcher classifier → Circle to Search");
    flow.setTypeface(Typeface.MONOSPACE);
    archBody.addView(flow, margins(0, 8, 0, 10));

    archBody.addView(supporting(
            "Shizuku or a one-time PC shell is required only for privileged setup. " +
            "Normal CTS activation runs independently afterward."));

    architecture.addView(archBody);
    root.addView(architecture, margins(0, 0, 0, 14));

    MaterialCardView notes = sectionCard();
    LinearLayout notesBody = sectionBody();

    notesBody.addView(text("Platform notes", 20, onSurface, Typeface.BOLD));

    TextView note = supporting(
            "Recent-task lock and Auto launch are OEM-managed ColorOS settings. " +
            "Their state is not exposed reliably to third-party applications and therefore " +
            "requires explicit user confirmation during setup.");
    notesBody.addView(note, margins(0, 8, 0, 0));

    notes.addView(notesBody);
    root.addView(notes);

    scroll.addView(root);
    return scroll;
}

private void showLanguagePicker() {
    String[] labels = LanguageManager.displayNames(this);
    int current = LanguageManager.indexOfCurrent(this);

    ScrollView scroll = new ScrollView(this);

    RadioGroup group = new RadioGroup(this);
    group.setOrientation(RadioGroup.VERTICAL);
    group.setPadding(dp(8), dp(4), dp(8), dp(8));

    TextView section = text(
            "Release languages",
            13,
            onSurfaceVariant,
            Typeface.BOLD);
    section.setPadding(dp(12), dp(6), dp(12), dp(6));
    group.addView(section);

    for (int i = 0; i < labels.length; i++) {
        final int index = i;

        MaterialRadioButton radio =
                new MaterialRadioButton(this);

        radio.setId(View.generateViewId());
        radio.setText(labels[i]);
        radio.setTextColor(onSurface);
        radio.setTextSize(16);
        radio.setMinHeight(dp(56));
        radio.setPadding(dp(8), 0, dp(8), 0);
        radio.setChecked(i == current);
        radio.setButtonTintList(makeRadioColorStateList());

        radio.setOnClickListener(v -> {
            LanguageManager.set(
                    this,
                    LanguageManager.codeAt(index));
            recreatePreservingTab();
        });

        group.addView(
                radio,
                new RadioGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    TextView note = supporting(
            "Only fully maintained release languages are listed.");
    note.setTextColor(warning);
    note.setPadding(dp(12), dp(10), dp(12), dp(4));
    group.addView(note);

    scroll.addView(group);

    new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Select language"))
            .setView(scroll)
            .setNegativeButton(tr("Cancel"), null)
            .show();
}


private void showOpenSourceLicenses() {
    final String[] labels = {
            tr("MindTrigger Assist — GPL-3.0-only"),
            tr("MiCTS — GPL-3.0"),
            tr("Material Components / Material Icons — Apache-2.0"),
            tr("Shizuku API — MIT"),
            tr("AndroidHiddenApiBypass — Apache-2.0"),
            tr("Audio asset provenance")
    };

    new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Open-source licenses"))
            .setItems(labels, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showBundledNotice(
                                "MindTrigger Assist — GPL-3.0-only",
                                R.raw.license_gpl_3_0);
                        break;
                    case 1:
                        showBundledNotice(
                                "MiCTS — GPL-3.0",
                                R.raw.notice_micts);
                        break;
                    case 2:
                        showBundledNotice(
                                "Apache License 2.0",
                                R.raw.license_apache_2_0);
                        break;
                    case 3:
                        showBundledNotice(
                                "Shizuku API — MIT",
                                R.raw.license_shizuku_api_mit);
                        break;
                    case 4:
                        showBundledNotice(
                                "AndroidHiddenApiBypass — Apache-2.0",
                                R.raw.notice_hidden_api_bypass);
                        break;
                    case 5:
                        showBundledNotice(
                                tr("Audio asset provenance"),
                                R.raw.notice_audio_provenance);
                        break;
                    default:
                        break;
                }
            })
            .setNegativeButton(tr("Close"), null)
            .show();
}

private void showBundledNotice(String title, int rawResId) {
    TextView content = text(readRawText(rawResId), 13, onSurface, Typeface.NORMAL);
    content.setTypeface(Typeface.MONOSPACE);
    content.setTextIsSelectable(true);
    content.setPadding(dp(18), dp(10), dp(18), dp(18));

    ScrollView scroll = new ScrollView(this);
    scroll.addView(content);

    new MaterialAlertDialogBuilder(this)
            .setTitle(tr(title))
            .setView(scroll)
            .setPositiveButton(tr("Close"), null)
            .show();
}

private String readRawText(int rawResId) {
    StringBuilder out = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(getResources().openRawResource(rawResId)))) {
        String line;
        while ((line = reader.readLine()) != null) {
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
    } catch (Throwable e) {
        return tr("Unable to read bundled notice.");
    }
    return out.toString();
}

private View infoPair(String label, String value) {
    MaterialCardView shell = nestedCard();

    LinearLayout row = column();
    row.setPadding(dp(16), dp(13), dp(16), dp(13));

    TextView left = text(
            label,
            13,
            onSurfaceVariant,
            Typeface.NORMAL);

    TextView right = text(
            value,
            15,
            onSurface,
            Typeface.BOLD);
    right.setPadding(0, dp(4), 0, 0);

    row.addView(left);
    row.addView(right);

    shell.addView(row);
    return shell;
}


private boolean termsAccepted() {
    return getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_TERMS_ACCEPTED, false);
}

private String termsBody() {
    return tr("MindTrigger Assist itself does not collect, store, sell, or upload personal data, analytics, telemetry, screenshots, queries, or system logs to a developer-operated server.")
            + "\n\n"
            + tr("The app reads selected local Android/ColorOS log events only to detect the configured long-press trigger. Those log events are processed locally by the watcher.")
            + "\n\n"
            + tr("READ_LOGS is a package-level permission retained across reboot, while privileged logcat access is session-scoped. When a session must be recreated, Android may require its own device-log access confirmation. MindTrigger Assist does not bypass that system confirmation.")
            + "\n\n"
            + tr("When Circle to Search or an Assistant session is invoked, Google software and services may process screen context, account information, queries, or other data under Google's own terms and privacy policies. That processing is outside MindTrigger Assist.")
            + "\n\n"
            + tr("Shizuku setup changes local device state. On its first setup path, MindTrigger Assist can grant READ_LOGS, adjust background policy, and run pm uninstall --user 0 for com.heytap.speechassist and com.coloros.colordirectservice.")
            + "\n\n"
            + tr("The watcher runs in the private :watcher process. Display over other apps is used by the non-interactive overlay and the transparent Log Session Bridge. If an active privileged logcat session is lost, Android may present its own device-log access confirmation; MindTrigger Assist does not bypass it.")
            + "\n\n"
            + tr("MindTrigger Assist is an unofficial compatibility utility. ColorOS or Google updates can change these behaviors. Privileged setup changes local device state; review the listed commands before applying them.")
            + "\n\n"
            + tr("Redistribution must comply with GPL-3.0-only and all applicable copyright, notice, and corresponding-source requirements.")
            + "\n\n"
            + tr("Content, branding, commentary, screenshots, links, or other material added by a redistributor is solely that redistributor's responsibility and does not represent the original MindTrigger Assist author. The original author assumes no responsibility for such added material, including controversial or unrelated content. This statement is informational only and does not limit rights granted by GPL-3.0-only.")
            + "\n\n"
            + tr("By continuing, you agree to these terms and acknowledge that Google-side data processing is governed by Google, not MindTrigger Assist.");
}

private void showFirstRunTerms() {
    showTermsDialog(true);
}

private void showTermsDialog(boolean firstRun) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Terms & Privacy"))
            .setMessage(termsBody())
            .setCancelable(!firstRun);

    if (firstRun) {
        builder.setNegativeButton(tr("Exit"), (dialog, which) -> {
            dialog.dismiss();
            try {
                finishAndRemoveTask();
            } catch (Throwable ignored) {
                finish();
            }
        });
        builder.setPositiveButton(tr("Agree & continue"), (dialog, which) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_TERMS_ACCEPTED, true)
                    .commit();
            dialog.dismiss();
            refreshAll();
            if (bootstrap != null) bootstrap.beginIfNeeded();
        });
    } else {
        builder.setPositiveButton(tr("Close"), null);
    }

    builder.show();
}



private void addGestureRequirement(LinearLayout root) {
    MaterialCardView shell = priorityCard();
    LinearLayout body = sectionBody();

    LinearLayout header = column();

    header.addView(text(
            "ColorOS gesture entry point",
            22,
            onSurface,
            Typeface.BOLD));

    TextView sub = supporting(
            "Required · Home/gesture activation depends on this ColorOS setting.");
    sub.setTextColor(warning);
    header.addView(sub, margins(0, 4, 0, 8));

    LinearLayout badgeRow = new LinearLayout(this);
    badgeRow.setOrientation(LinearLayout.HORIZONTAL);
    badgeRow.addView(
            pill(
                    "REQUIRED",
                    primaryContainer,
                    onPrimaryContainer));
    header.addView(badgeRow);

    body.addView(header);

    gestureStatus = statusText();
    body.addView(
            gestureStatus,
            margins(0, 12, 0, 8));

    gestureSetupBlock = column();

    TextView required = supporting(
            "Enable “Touch and hold gesture guide bar to wake Breeno”. ColorOS blocks MindTrigger Assist from opening the real System navigation page directly, so this step must be completed manually.");
    required.setTextColor(onSurface);
    gestureSetupBlock.addView(
            required,
            margins(0, 2, 0, 10));

    TextView manual = supporting(
            "Follow the three screenshots below. The old direct System navigation button has been removed because it only opens an unrelated AOSP navigation UI on this ColorOS build.");
    manual.setTextColor(onSurfaceVariant);
    gestureSetupBlock.addView(
            manual,
            margins(0, 0, 0, 10));

    addGestureWalkthrough(gestureSetupBlock);

    TextView removal = supporting(
            "First-run Shizuku setup removes com.heytap.speechassist and com.coloros.colordirectservice from user 0 with pm uninstall --user 0.");
    removal.setTextColor(warning);
    gestureSetupBlock.addView(
            removal,
            margins(0, 16, 0, 8));

    TextView missing = supporting(
            "If the highlighted switch is missing, temporarily restore SpeechAssist, enable the switch using the guide above, then uninstall SpeechAssist again.");
    gestureSetupBlock.addView(
            missing,
            margins(0, 0, 0, 8));

    gestureSetupBlock.addView(
            miniTitle("Recovery · Restore SpeechAssist"),
            margins(0, 6, 0, 4));

    TextView restoreCommand =
            supporting(
                    SetupCommands.speechAssistRestoreCommands());
    restoreCommand.setTypeface(Typeface.MONOSPACE);
    restoreCommand.setTextColor(onSurface);
    gestureSetupBlock.addView(restoreCommand);

    MaterialButton copyRestore =
            outlinedButton("Copy restore commands");
    copyRestore.setOnClickListener(v -> {
        copy(
                "Restore SpeechAssist",
                SetupCommands.speechAssistRestoreCommands());
        Toast.makeText(
                this,
                tr("Restore commands copied."),
                Toast.LENGTH_SHORT).show();
    });
    gestureSetupBlock.addView(
            copyRestore,
            margins(0, 8, 0, 8));

    TextView middle = supporting(
            "Then follow the three screenshots again and enable the highlighted ColorOS switch.");
    middle.setTextColor(onSurface);
    gestureSetupBlock.addView(
            middle,
            margins(0, 4, 0, 8));

    gestureSetupBlock.addView(
            miniTitle("Recovery · Remove SpeechAssist again"),
            margins(0, 4, 0, 4));

    TextView removeCommand =
            supporting(
                    SetupCommands.speechAssistRemoveCommand());
    removeCommand.setTypeface(Typeface.MONOSPACE);
    removeCommand.setTextColor(onSurface);
    gestureSetupBlock.addView(removeCommand);

    MaterialButton copyRemove =
            outlinedButton("Copy uninstall command");
    copyRemove.setOnClickListener(v -> {
        copy(
                "Uninstall SpeechAssist",
                SetupCommands.speechAssistRemoveCommand());
        Toast.makeText(
                this,
                tr("Uninstall command copied."),
                Toast.LENGTH_SHORT).show();
    });
    gestureSetupBlock.addView(
            copyRemove,
            margins(0, 8, 0, 0));

    TextView alreadyEnabled = supporting(
            "If a slight zoom animation already appears when you long-press the gesture guide bar, the entry point is probably active. You can skip recovery and confirm this step.");
    alreadyEnabled.setTextColor(success);
    gestureSetupBlock.addView(
            alreadyEnabled,
            margins(0, 14, 0, 0));

    MaterialButton confirmGesture =
            warningConfirmButton(
                    "Confirm gesture is enabled");

    confirmGesture.setOnClickListener(v ->
            confirmManualStep(
                    "Confirm ColorOS gesture",
                    "Only confirm after the highlighted ColorOS switch is enabled. A slight zoom animation when long-pressing the gesture guide bar is a useful sign that the entry point is active. MindTrigger Assist cannot reliably query this OEM setting.",
                    () -> getSharedPreferences(
                            PREFS,
                            MODE_PRIVATE)
                            .edit()
                            .putBoolean(
                                    PREF_GESTURE_CONFIRMED,
                                    true)
                            .apply()));

    gestureSetupBlock.addView(
            confirmGesture,
            margins(0, 12, 0, 0));

    body.addView(gestureSetupBlock);

    gestureConfirmedBlock =
            confirmedManualRow(
                    "ColorOS gesture confirmed",
                    "The setup guide is collapsed because you already confirmed this step.",
                    "Review guide",
                    v -> showGestureGuideDialog());

    body.addView(
            gestureConfirmedBlock,
            margins(0, 10, 0, 0));

    shell.addView(body);
    root.addView(
            shell,
            margins(0, 0, 0, 16));
}

private void addGestureWalkthrough(LinearLayout parent) {
    MaterialCardView walkthrough = nestedCard();
    LinearLayout content = column();
    content.setPadding(
            dp(14),
            dp(14),
            dp(14),
            dp(14));

    TextView title = text(
            "Manual ColorOS path",
            16,
            onSurface,
            Typeface.BOLD);
    content.addView(title);

    TextView desc = supporting(
            "Use these screenshots as the source of truth for this ColorOS build.");
    content.addView(
            desc,
            margins(0, 3, 0, 8));

    addGestureGuideStep(
            content,
            "1",
            "Open Home screen settings",
            R.drawable.guide_gesture_step1_home_screen);

    addGestureGuideStep(
            content,
            "2",
            "Open System navigation",
            R.drawable.guide_gesture_step2_system_navigation);

    addGestureGuideStep(
            content,
            "3",
            "Enable “Touch and hold gesture guide bar to wake Breeno”",
            R.drawable.guide_gesture_step3_breeno_hold);

    walkthrough.addView(content);
    parent.addView(
            walkthrough,
            margins(0, 4, 0, 0));
}

private void addGestureGuideStep(
        LinearLayout parent,
        String number,
        String caption,
        int drawableId) {

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);

    header.addView(
            pill(
                    number,
                    primaryContainer,
                    onPrimaryContainer));

    TextView label = text(
            caption,
            14,
            onSurface,
            Typeface.BOLD);
    label.setPadding(
            dp(10),
            0,
            0,
            0);

    header.addView(
            label,
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f));

    parent.addView(
            header,
            margins(0, 10, 0, 6));

    MaterialCardView frame = elevatedCard(
            surfaceContainerHigh,
            20,
            0f);
    frame.setStrokeColor(
            Color.argb(
                    100,
                    Color.red(outline),
                    Color.green(outline),
                    Color.blue(outline)));
    frame.setStrokeWidth(dp(1));
    frame.setContentPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4));

    ImageView image = new ImageView(this);
    image.setImageResource(drawableId);
    image.setAdjustViewBounds(true);
    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
    image.setContentDescription(tr(caption));

    frame.addView(image);
    parent.addView(frame);
}

private void showGestureGuideDialog() {
    ScrollView scroll = new ScrollView(this);
    LinearLayout content = column();
    content.setPadding(
            dp(4),
            dp(2),
            dp(4),
            dp(10));

    addGestureWalkthrough(content);
    scroll.addView(content);

    new MaterialAlertDialogBuilder(this)
            .setTitle(tr("ColorOS gesture guide"))
            .setView(scroll)
            .setPositiveButton(tr("Close"), null)
            .show();
}


private void confirmShizukuSetup() {
    new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Confirm Shizuku setup"))
            .setMessage(tr("Shizuku first-run will grant privileged setup access and remove com.heytap.speechassist plus com.coloros.colordirectservice from user 0 using pm uninstall --user 0. The system-partition APKs are not erased. Continue only if you understand these changes."))
            .setNegativeButton(tr("Cancel"), null)
            .setPositiveButton(tr("Continue"), (dialog, which) -> { dialog.dismiss(); if (bootstrap != null) bootstrap.runNow(); })
            .show();
}

private void confirmManualStep(String title, String warningText, Runnable onConfirmed) {
    new MaterialAlertDialogBuilder(this)
            .setTitle(tr(title))
            .setMessage(tr(warningText))
            .setNegativeButton(tr("Cancel"), null)
            .setPositiveButton(tr("I verified this setting"), (dialog, which) -> { dialog.dismiss(); onConfirmed.run(); refreshAll(); })
            .show();
}

private LinearLayout confirmedManualRow(
        String title,
        String description,
        String action,
        View.OnClickListener listener) {

    LinearLayout row = column();
    row.setPadding(dp(16), dp(14), dp(16), dp(14));
    row.setBackground(roundRect(surfaceContainerHigh, 22));

    row.addView(text(
            "✓ " + tr(title),
            15,
            success,
            Typeface.BOLD));

    TextView desc = supporting(description);
    row.addView(desc, margins(0, 4, 0, 8));

    MaterialButton redo = compactButton(action);
    redo.setOnClickListener(listener);

    LinearLayout actionRow = new LinearLayout(this);
    actionRow.setOrientation(LinearLayout.HORIZONTAL);
    actionRow.setGravity(Gravity.END);
    actionRow.addView(redo);

    row.addView(actionRow);

    return row;
}

private MaterialButton warningConfirmButton(String label) {
    MaterialButton b = filledTonalButton(label);
    int warningContainer = Color.argb(
            ThemeManager.isDark(this) ? 42 : 24,
            Color.red(warning),
            Color.green(warning),
            Color.blue(warning));
    b.setBackgroundTintList(flatTint(warningContainer));
    b.setTextColor(warning);
    b.setStrokeColor(ColorStateList.valueOf(warning));
    b.setStrokeWidth(dp(1));
    b.setElevation(0f);
    return b;
}

private void applyPressDepth(View view, float restZDp, float pressedZDp) {
    view.setTranslationZ(0f);
    view.setElevation(0f);
}

private MaterialCardView priorityCard() {
    MaterialCardView card = elevatedCard(surfaceContainer, 26, 0f);
    card.setStrokeColor(Color.argb(
            120,
            Color.red(primary),
            Color.green(primary),
            Color.blue(primary)));
    card.setStrokeWidth(dp(1));
    return card;
}

private LinearLayout weightedButtonRow() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    return row;
}

private LinearLayout.LayoutParams weightedButtonParams(int leftMarginDp) {
    LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f);
    lp.setMargins(dp(leftMarginDp), 0, 0, 0);
    return lp;
}

private MaterialButton themeColorButton(String label, String colorCode) {
    MaterialButton b = compactButton(label);
    int swatch = ThemeManager.previewPrimary(this, colorCode);
    int onSwatch = ThemeManager.previewOnPrimary(this, colorCode);
    boolean selected = colorCode.equals(ThemeManager.getColor(this));

    b.setBackgroundTintList(ColorStateList.valueOf(swatch));
    b.setTextColor(onSwatch);
    b.setStrokeWidth(selected ? dp(2) : 0);
    b.setStrokeColor(ColorStateList.valueOf(
            selected ? onSwatch : Color.TRANSPARENT));
    b.setElevation(0f);

    b.setOnClickListener(v -> {
        ThemeManager.setColor(this, colorCode);
        recreatePreservingTab();
    });
    return b;
}

private MaterialButton themeModeButton(String label, String mode) {
    boolean selected = mode.equals(ThemeManager.getMode(this));
    MaterialButton b = compactButton(label);
    b.setMinHeight(dp(50));
    b.setBackgroundTintList(ColorStateList.valueOf(
            selected ? primary : surface));
    b.setTextColor(selected ? onPrimary : onSurface);
    b.setStrokeColor(ColorStateList.valueOf(
            selected ? primary : outline));
    b.setStrokeWidth(dp(1));
    b.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
    b.setElevation(0f);
    b.setOnClickListener(v -> {
        ThemeManager.setMode(this, mode);
        recreatePreservingTab();
    });
    return b;
}

    private void addTopBar(LinearLayout root, String titleText, String subtitleText) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), dp(12), dp(4), dp(18));

        LinearLayout titles = column();
        titles.addView(text(titleText, 27, onSurface, Typeface.BOLD));

        TextView subtitle = text(
                subtitleText,
                14,
                primary,
                Typeface.BOLD);
        subtitle.setPadding(0, dp(3), 0, 0);
        titles.addView(subtitle);

        bar.addView(titles, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if ("MindTrigger Assist".equals(titleText)) {
            bar.addView(pill("V16 RC1", primaryContainer, onPrimaryContainer));
        }

        root.addView(bar, matchWrap());
    }

    private void addHero(LinearLayout root) {
        MaterialCardView hero = elevatedCard(surfaceContainerHigh, 30, 4f);
        hero.setStrokeColor(
                Color.argb(
                        145,
                        Color.red(primary),
                        Color.green(primary),
                        Color.blue(primary)));
        hero.setStrokeWidth(dp(1));

        LinearLayout body = column();
        body.setPadding(dp(22), dp(20), dp(22), dp(20));

        TextView eyebrow = text(
                "CIRCLE TO SEARCH",
                11,
                primary,
                Typeface.BOLD);
        eyebrow.setLetterSpacing(0.14f);
        body.addView(eyebrow);

        heroTitle = text(
                "Checking setup…",
                27,
                onSurface,
                Typeface.BOLD);
        heroTitle.setPadding(0, dp(9), 0, dp(5));
        body.addView(heroTitle);

        heroSubtitle = text(
                "Complete the required ColorOS and Google settings before starting the watcher.",
                14,
                onSurfaceVariant,
                Typeface.NORMAL);
        heroSubtitle.setLineSpacing(0, 1.10f);
        body.addView(heroSubtitle);

        TextView rebootLogHint = text(
                tr("After reboot, if Android does not show the device-log access confirmation, open MindTrigger Assist once."),
                12,
                onSurfaceVariant,
                Typeface.NORMAL);
        rebootLogHint.setLineSpacing(0, 1.08f);
        rebootLogHint.setAlpha(0.86f);
        body.addView(rebootLogHint, margins(0, 12, 0, 0));

        hero.addView(body);
        root.addView(hero, margins(0, 0, 0, 18));
    }



private void addCriticalRequirements(LinearLayout root) {
    MaterialCardView card =
            elevatedCard(
                    surfaceContainer,
                    28,
                    3f);

    card.setStrokeWidth(dp(1));
    card.setStrokeColor(
            Color.argb(
                    130,
                    Color.red(primary),
                    Color.green(primary),
                    Color.blue(primary)));

    LinearLayout body = column();
    body.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(18));

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);

    LinearLayout copy = column();
    copy.addView(text(
            "Required before Run",
            20,
            onSurface,
            Typeface.BOLD));

    TextView desc = supporting(
            "Run stays disabled until every required item below is ready.");
    copy.addView(
            desc,
            margins(0, 3, 0, 0));

    header.addView(
            copy,
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f));

    header.addView(
            pill(
                    "REQUIRED",
                    primaryContainer,
                    onPrimaryContainer));

    body.addView(header);

    criticalStatus = text(
            "Checking…",
            14,
            onSurface,
            Typeface.BOLD);
    criticalStatus.setLineSpacing(
            dp(4),
            1.05f);

    body.addView(
            criticalStatus,
            margins(0, 14, 0, 0));

    card.addView(body);
    root.addView(
            card,
            margins(0, 2, 0, 16));
}


    private void addStep1(LinearLayout root) {
        MaterialCardView shell = sectionCard();
        LinearLayout body = sectionBody();

        addSectionHeader(
                body,
                "1",
                "Privileged setup",
                "Grant the READ_LOGS package permission and apply the required device-side settings. Shizuku can perform this one-time setup without root.");

        step1Status = statusText();
        body.addView(step1Status, margins(0, 12, 0, 6));

        readLogsRow = actionRow(
                "READ_LOGS permission",
                "Package-level permission granted once and retained across reboot.",
                "Check",
                v -> refreshAll());
        body.addView(readLogsRow.root, margins(0, 6, 0, 0));

        logSessionRow = actionRow(
                "Device log access session",
                "Privileged logcat access is session-scoped. A new session may require Android's device-log access confirmation.",
                "Reconnect",
                v -> requestLogSessionReconnect(true));
        body.addView(logSessionRow.root, margins(0, 8, 0, 0));

        step1Methods = new LinearLayout(this);
        step1Methods.setOrientation(LinearLayout.HORIZONTAL);
        step1Methods.setGravity(Gravity.CENTER_VERTICAL);

        shizukuButton = filledTonalButton("Shizuku");
        shizukuButton.setOnClickListener(v -> confirmShizukuSetup());

        pcButton = outlinedButton("PC one-shot");
        pcButton.setOnClickListener(v -> {
            String cmd = SetupCommands.pcOneShot(getPackageName());
            copy("MindTrigger Assist one-shot", cmd);
            Toast.makeText(this,
                    tr("Command copied. Run it once from a computer; the app will reopen after setup."),
                    Toast.LENGTH_LONG).show();
        });

        step1Methods.addView(shizukuButton, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pcLp.setMargins(dp(8), 0, 0, 0);
        step1Methods.addView(pcButton, pcLp);

        body.addView(step1Methods, margins(0, 10, 0, 0));

        shell.addView(body);
        root.addView(shell, margins(0, 0, 0, 14));
    }


private void addStep2(LinearLayout root) {
    MaterialCardView shell = sectionCard();
    LinearLayout body = sectionBody();
    addSectionHeader(body, "2", "Background reliability", "Keep the isolated watcher available. ColorOS Recent-task lock reduces process reclamation; Display over other apps enables the automatic Log Session Bridge path.");
    step2Status = statusText(); body.addView(step2Status, margins(0, 12, 0, 6));

    overlayRow = actionRow("Display over other apps", "Required for the transparent Log Session Bridge and the non-interactive watcher overlay.", "Open settings", v -> SettingsNavigator.overlay(this));
    body.addView(overlayRow.root, margins(0, 6, 0, 0));
    selfBatteryRow = actionRow("Battery optimization", "Recommended so ColorOS is less likely to restrict the isolated watcher in background.", "Open settings", v -> SettingsNavigator.requestBatteryExemption(this, getPackageName()));
    body.addView(selfBatteryRow.root, margins(0, 8, 0, 0));
    addGuide(body, "Background activity guide", R.drawable.guide_self_background);

    recentsSetupBlock = column();
    recentsSetupBlock.addView(miniTitle("Lock in Recent tasks"), margins(2, 18, 0, 4));
    recentsSetupBlock.addView(supporting("Required. Lock MindTrigger Assist in ColorOS Recent Tasks Manager so Clear All does not stop the watcher."));
    recentsOpenButton = filledTonalButton("Open Recent Tasks Manager");
    recentsOpenButton.setOnClickListener(v -> SettingsNavigator.homeScreenSettings(this));
    recentsSetupBlock.addView(recentsOpenButton, margins(0, 10, 0, 0));
    addGuide(recentsSetupBlock, "Recent-task lock guide", R.drawable.guide_recents_lock);
    recentsConfirmButton = warningConfirmButton("Confirm MindTrigger Assist is locked");
    recentsConfirmButton.setOnClickListener(v -> confirmManualStep(
            "Confirm Recent-task lock",
            "Only confirm after you have actually locked MindTrigger Assist in ColorOS Recent Tasks Manager. MindTrigger Assist cannot query this OEM state. A false confirmation can make Clear All stop the watcher.",
            () -> getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_RECENTS_LOCKED, true).apply()));
    recentsSetupBlock.addView(recentsConfirmButton, margins(0, 8, 0, 0));
    body.addView(recentsSetupBlock);

    recentsConfirmedBlock = confirmedManualRow("Recent-task lock confirmed", "The detailed guide is hidden because you already confirmed this step.", "Open again", v -> SettingsNavigator.homeScreenSettings(this));
    body.addView(recentsConfirmedBlock, margins(0, 14, 0, 0));
    shell.addView(body); root.addView(shell, margins(0, 0, 0, 14));
}


private void addStep3(LinearLayout root) {
    MaterialCardView shell = sectionCard(); LinearLayout body = sectionBody();
    addSectionHeader(body, "3", "Google integration", "Keep the Google app and Gemini available when ColorOS reclaims background processes.");
    step3Status = statusText(); body.addView(step3Status, margins(0, 12, 0, 6));

    googleAssistantRow = actionRow(
            "Default assistant · Google",
            "Set Google as the default Android assistant. MindTrigger Assist applies both assistant and voice_interaction_service secure settings during privileged setup.",
            "Apply with Shizuku",
            v -> {
                if (bootstrap != null) bootstrap.runNow();
            });
    body.addView(googleAssistantRow.root, margins(0, 6, 0, 0));

    googleBatteryRow = actionRow("Google · Battery unrestricted", "Required for reliable Circle to Search activation.", "Fix", v -> SettingsNavigator.requestBatteryExemption(this, SetupCommands.GOOGLE));
    body.addView(googleBatteryRow.root, margins(0, 6, 0, 0));
    geminiBatteryRow = actionRow("Gemini · Battery unrestricted", "Required when Gemini is installed.", "Fix", v -> SettingsNavigator.requestBatteryExemption(this, SetupCommands.GEMINI));
    body.addView(geminiBatteryRow.root, margins(0, 8, 0, 0));
    addGuide(body, "Battery settings guide", R.drawable.guide_google_background, R.drawable.guide_gemini_background);

    autoLaunchSetupBlock = column();
    autoLaunchSetupBlock.addView(miniTitle("Allow automatic launch"), margins(2, 18, 0, 4));
    autoLaunchSetupBlock.addView(supporting("Allow Google and Gemini to launch automatically in ColorOS. This OEM state cannot be queried reliably, so confirmation remains manual."));
    autoLaunchOpenButton = filledTonalButton("Open Settings"); autoLaunchOpenButton.setOnClickListener(v -> SettingsNavigator.autoLaunch(this));
    autoLaunchSetupBlock.addView(autoLaunchOpenButton, margins(0, 10, 0, 0));
    addGuide(autoLaunchSetupBlock, "Auto launch guide", R.drawable.guide_auto_launch);
    autoLaunchConfirmButton = warningConfirmButton("Confirm Auto launch is enabled");
    autoLaunchConfirmButton.setOnClickListener(v -> confirmManualStep(
            "Confirm Auto launch",
            "Only confirm after Auto launch is enabled for the installed Google/Gemini packages. MindTrigger Assist cannot query this ColorOS OEM state. A false confirmation can make CTS or Gemini fail after the apps are killed.",
            () -> getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_GOOGLE_AUTOSTART, true).putBoolean(PREF_GEMINI_AUTOSTART, true).apply()));
    autoLaunchSetupBlock.addView(autoLaunchConfirmButton, margins(0, 8, 0, 0)); body.addView(autoLaunchSetupBlock);

    autoLaunchConfirmedBlock = confirmedManualRow("Auto launch confirmed", "The detailed guide is hidden because you already confirmed this step.", "Open again", v -> SettingsNavigator.autoLaunch(this));
    body.addView(autoLaunchConfirmedBlock, margins(0, 14, 0, 0));
    shell.addView(body); root.addView(shell, margins(0, 0, 0, 14));
}

    private void addRunCard(LinearLayout root) {
        MaterialCardView shell = elevatedCard(surfaceContainerHigh, 28, 6f);
        LinearLayout body = column();
        body.setPadding(dp(22), dp(20), dp(22), dp(20));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = column();
        copy.addView(text("Watcher", 20, onSurface, Typeface.BOLD));

        TextView desc = supporting(
                "Starts the foreground-service watcher after all required checks pass.");
        desc.setPadding(0, dp(3), 0, 0);
        copy.addView(desc);

        header.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        header.addView(pill("FGS", primaryContainer, onPrimaryContainer));
        body.addView(header);

        runButton = filledButton("Run MindTrigger Assist");
        runButton.setOnClickListener(v -> {
            if (isWatcherRunning()
                    && watcherSessionState
                    != WatcherIpc.STATE_ACTIVE) {
                requestLogSessionReconnect(true);
            } else {
                runWatcherWithGate();
            }
        });
        body.addView(runButton, margins(0, 14, 0, 0));

        shell.addView(body);
        root.addView(shell, margins(0, 0, 0, 18));
    }

    private void runWatcherWithGate() {
        String missing = firstMissingRequiredStep();
        if (missing != null) {
            Toast.makeText(this, missing, Toast.LENGTH_LONG).show();
            refreshAll();
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putBoolean(PREF_ENABLED, true).commit();

        startWatcher();
        bindWatcher();
        syncWatcherPrefs();
        refreshAll();

        Toast.makeText(
                this,
                tr("Approve Android's device-log access confirmation to start the privileged logcat reader."),
                Toast.LENGTH_LONG).show();

        statusPollRemaining = 120;

        ui.postDelayed(
                () -> requestLogSessionReconnect(true),
                300L);
    }

    private String firstMissingRequiredStep() {
        if (!termsAccepted())
            return tr("Accept Terms & Privacy before running.");

        if (!isReadLogsGranted())
            return tr("Step 1: READ_LOGS has not been granted.");

        if (!pref(PREF_GESTURE_CONFIRMED))
            return tr("Required ColorOS gesture has not been confirmed.");

        if (!pref(PREF_RECENTS_LOCKED))
            return tr("Step 2: Recent-task lock has not been confirmed.");

        if (Build.VERSION.SDK_INT >= 23
                && !Settings.canDrawOverlays(this))
            return tr("Step 2: Display over other apps is required for automatic Log Session Bridge recovery.");

        boolean googleInstalled = packageExists(SetupCommands.GOOGLE);
        boolean geminiInstalled = packageExists(SetupCommands.GEMINI);

        if (googleInstalled && !isGoogleAssistantSelected())
            return tr("Step 3: Google is not selected as the default assistant.");

        if (googleInstalled && !isIgnoringBattery(SetupCommands.GOOGLE))
            return tr("Step 3: Google is still battery-optimized.");

        if (geminiInstalled && !isIgnoringBattery(SetupCommands.GEMINI))
            return tr("Step 3: Gemini is still battery-optimized.");

        boolean autoLaunchConfirmed =
                pref(PREF_GOOGLE_AUTOSTART) && pref(PREF_GEMINI_AUTOSTART);

        if ((googleInstalled || geminiInstalled) && !autoLaunchConfirmed)
            return tr("Step 3: Auto launch has not been confirmed.");

        return null;
    }

    private void refreshAll() {
        updateLastCommandTime();
        boolean readLogs = isReadLogsGranted();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean selfBattery = isIgnoringBattery(getPackageName());
        boolean gesture = pref(PREF_GESTURE_CONFIRMED);
        boolean recents = pref(PREF_RECENTS_LOCKED);

        boolean googleInstalled = packageExists(SetupCommands.GOOGLE);
        boolean geminiInstalled = packageExists(SetupCommands.GEMINI);

        boolean googleAssistant =
                !googleInstalled || isGoogleAssistantSelected();

        boolean googleBattery =
                !googleInstalled || isIgnoringBattery(SetupCommands.GOOGLE);

        boolean geminiBattery =
                !geminiInstalled || isIgnoringBattery(SetupCommands.GEMINI);

        boolean autoLaunch =
                (!googleInstalled && !geminiInstalled)
                        || (pref(PREF_GOOGLE_AUTOSTART)
                        && pref(PREF_GEMINI_AUTOSTART));

        boolean step1Ready = readLogs;
        boolean step2Ready = recents && overlay;
        boolean step3Ready =
                googleAssistant
                        && googleBattery
                        && geminiBattery
                        && autoLaunch;
        boolean ready = step1Ready && gesture && step2Ready && step3Ready;
        boolean running = isWatcherRunning();

        int logSession =
                watcherSessionState;

        boolean logSessionActive =
                running
                        && logSession
                        == WatcherIpc.STATE_ACTIVE;

        boolean logSessionConnecting =
                running
                        && logSession
                        == WatcherIpc.STATE_CONNECTING;

        if (criticalStatus != null) {
            criticalStatus.setText(
                    checklistLine(readLogs, "READ_LOGS permission granted") + "\n" +
                    checklistLine(logSessionActive, "Device-log access session active") + "\n" +
                    checklistLine(gesture, "ColorOS long-press gesture confirmed") + "\n" +
                    checklistLine(recents, "Recent-task lock confirmed") + "\n" +
                    checklistLine(overlay, "Display over other apps enabled") + "\n" +
                    checklistLine(googleAssistant, "Google default assistant configured") + "\n" +
                    checklistLine(googleBattery && geminiBattery && autoLaunch, "Google services background setup complete"));
        }
        if (gestureStatus != null) {
            gestureStatus.setText(gesture ? "✓ " + tr("ColorOS gesture confirmed") : "⚠ " + tr("ColorOS gesture confirmation required"));
            gestureStatus.setTextColor(gesture ? success : warning);
        }
        if (gestureSetupBlock != null) gestureSetupBlock.setVisibility(gesture ? View.GONE : View.VISIBLE);
        if (gestureConfirmedBlock != null) gestureConfirmedBlock.setVisibility(gesture ? View.VISIBLE : View.GONE);
        if (recentsSetupBlock != null) recentsSetupBlock.setVisibility(recents ? View.GONE : View.VISIBLE);
        if (recentsConfirmedBlock != null) recentsConfirmedBlock.setVisibility(recents ? View.VISIBLE : View.GONE);
        if (autoLaunchSetupBlock != null) autoLaunchSetupBlock.setVisibility(autoLaunch ? View.GONE : View.VISIBLE);
        if (autoLaunchConfirmedBlock != null) autoLaunchConfirmedBlock.setVisibility(autoLaunch ? View.VISIBLE : View.GONE);

        if (heroTitle != null) {
            heroTitle.setText(tr(
                    running && logSessionActive
                            ? "Watcher active"
                            : running
                            ? "Android log-access confirmation required"
                            : ready
                            ? "Ready"
                            : "Setup required"));
        }

        if (heroSubtitle != null) {
            heroSubtitle.setText(tr(
                    running && logSessionActive
                            ? "Long-press the configured Home/gesture entry point to invoke Circle to Search."
                            : running && logSessionConnecting
                            ? "Waiting for Android's device-log access confirmation."
                            : running
                            ? "Open MindTrigger Assist in the foreground to restore device log access."
                            : ready
                            ? "All required setup is complete. Run will request a privileged logcat access session."
                            : "Complete the remaining required items below."));
        }

        if (!readLogs) {
            setStepStatus(
                    step1Status,
                    false,
                    "Privileged setup required");
        } else if (!running) {
            setStepStatus(
                    step1Status,
                    true,
                    "READ_LOGS retained · logcat access starts on Run");
        } else if (logSessionActive) {
            setStepStatus(
                    step1Status,
                    true,
                    "READ_LOGS retained · privileged logcat access active");
        } else if (logSessionConnecting) {
            setStepStatus(
                    step1Status,
                    false,
                    "READ_LOGS retained · waiting for Android log-access confirmation");
        } else {
            setStepStatus(
                    step1Status,
                    false,
                    "READ_LOGS retained · logcat access must reconnect");
        }

        setActionState(
                readLogsRow,
                readLogs,
                readLogs
                        ? "READ_LOGS package permission granted"
                        : "READ_LOGS not granted");

        updateLogSessionRow(
                readLogs,
                running,
                logSession);

        if (step1Methods != null) {
            step1Methods.setVisibility(step1Ready ? View.GONE : View.VISIBLE);
        }
        shizukuButton.setEnabled(!step1Ready);
        pcButton.setEnabled(!step1Ready);
        shizukuButton.setText("Shizuku");
        pcButton.setText("PC one-shot");

        setStepStatus(
                step2Status,
                step2Ready,
                !recents
                        ? "Recent-task lock confirmation required"
                        : !overlay
                        ? "Display over other apps is required for the automatic Log Session Bridge"
                        : "Watcher retention and automatic recovery prerequisites complete");

        setActionState(
                overlayRow,
                overlay,
                overlay
                        ? "Overlay enabled · automatic Log Session Bridge available"
                        : "Required · enable the automatic Log Session Bridge");

        setActionState(
                selfBatteryRow,
                selfBattery,
                selfBattery ? "Background activity allowed"
                        : "Recommended · optional");

        if (googleAssistantRow != null) {
            if (!googleInstalled) {
                googleAssistantRow.status.setText(
                        tr("Google is not installed"));
                googleAssistantRow.status.setTextColor(onSurfaceVariant);
                googleAssistantRow.button.setVisibility(View.VISIBLE);
                googleAssistantRow.button.setEnabled(false);
                googleAssistantRow.button.setText(tr("Unavailable"));
            } else {
                setActionState(
                        googleAssistantRow,
                        googleAssistant,
                        googleAssistant
                                ? "Google is selected as the default assistant"
                                : "Google is not the default assistant");

                if (!googleAssistant) {
                    googleAssistantRow.button.setText(
                            tr("Apply with Shizuku"));
                }
            }
        }

        setPackageBatteryState(
                googleBatteryRow,
                googleInstalled,
                googleBattery,
                "Google");

        setPackageBatteryState(
                geminiBatteryRow,
                geminiInstalled,
                geminiBattery,
                "Gemini");


        setStepStatus(
                step3Status,
                step3Ready,
                !googleAssistant ? "Google default assistant required"
                        : !googleBattery ? "Google battery setting required"
                        : !geminiBattery ? "Gemini battery setting required"
                        : !autoLaunch ? "Auto launch confirmation required"
                        : "Google services configuration complete");

        if (running) {
            if (logSessionActive) {
                runButton.setEnabled(false);
                runButton.setText(tr("Running"));
            } else if (logSessionConnecting) {
                runButton.setEnabled(false);
                runButton.setText(tr("Waiting for Android log access"));
            } else {
                runButton.setEnabled(ready && readLogs);
                runButton.setText(tr("Reconnect device-log access"));
            }
        } else {
            runButton.setEnabled(ready);
            runButton.setText(tr(
                    ready
                            ? "Run MindTrigger Assist"
                            : "Complete setup first"));
        }
    }


    private boolean isGoogleAssistantSelected() {
        try {
            String assistant =
                    Settings.Secure.getString(
                            getContentResolver(),
                            "assistant");

            String voice =
                    Settings.Secure.getString(
                            getContentResolver(),
                            "voice_interaction_service");

            String expected =
                    SetupCommands.GOOGLE_ASSISTANT_SERVICE;

            return expected.equals(assistant)
                    && expected.equals(voice);

        } catch (Throwable ignored) {
            return false;
        }
    }

    private String formatLastExecutionTime() {
        if (bootstrap == null) {
            return tr("Never");
        }

        long when = bootstrap.getLastExecutionTimeMs();
        if (when <= 0L) {
            return tr("Never");
        }

        try {
            Locale locale = Locale.forLanguageTag(LanguageManager.get(this));
            DateFormat exact = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.MEDIUM,
                    locale);

            CharSequence relative = android.text.format.DateUtils.getRelativeTimeSpanString(
                    when,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS,
                    android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE);

            return exact.format(new Date(when)) + "  ·  " + relative;
        } catch (Throwable ignored) {
            return Long.toString(when);
        }
    }

    private void updateLastCommandTime() {
        if (commandTimeView != null) {
            commandTimeView.setText(
                    formatLastExecutionTime());
        }
    }

    private void shareProject() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(
                Intent.EXTRA_SUBJECT,
                "MindTrigger Assist");
        send.putExtra(
                Intent.EXTRA_TEXT,
                tr("MindTrigger Assist — open-source ColorOS Circle to Search utility by @EvokerUniverse."));

        try {
            startActivity(
                    Intent.createChooser(
                            send,
                            tr("Share project")));
        } catch (Throwable ignored) {}
    }

    private ColorStateList makeRadioColorStateList() {
        return new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] {}
                },
                new int[] {
                        primary,
                        onSurfaceVariant
                });
    }

    private void addChromeLikeColorPicker(
            LinearLayout parent) {

        String[] colors = {
                ThemeManager.COLOR_BLUE,
                ThemeManager.COLOR_TEAL,
                ThemeManager.COLOR_GREEN,
                ThemeManager.COLOR_SAGE,
                ThemeManager.COLOR_YELLOW,
                ThemeManager.COLOR_ORANGE,
                ThemeManager.COLOR_PINK,
                ThemeManager.COLOR_LAVENDER,
                ThemeManager.COLOR_CORAL
        };

        int columns = 4;
        for (int rowIndex = 0; rowIndex * columns < colors.length; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.START);

            for (int column = 0; column < columns; column++) {
                int index = rowIndex * columns + column;
                if (index >= colors.length) {
                    break;
                }

                View swatch = chromeColorSwatch(colors[index]);

                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(
                                dp(60),
                                dp(60));
                lp.setMargins(column == 0 ? 0 : dp(12), rowIndex == 0 ? dp(8) : dp(12), 0, 0);
                row.addView(swatch, lp);
            }

            parent.addView(row);
        }
    }

    private View chromeColorSwatch(
            String colorCode) {

        boolean selected =
                colorCode.equals(
                        ThemeManager.getColor(this));

        MaterialCardView outer =
                new MaterialCardView(this);

        outer.setRadius(dp(30));
        outer.setCardElevation(0f);
        outer.setUseCompatPadding(false);
        outer.setPreventCornerOverlap(true);
        outer.setCardBackgroundColor(surface);
        outer.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
        outer.setStrokeWidth(selected ? dp(3) : dp(1));
        outer.setStrokeColor(selected ? primary : outline);

        FrameLayout frame = new FrameLayout(this);

        MaterialCardView inner = new MaterialCardView(this);
        inner.setRadius(dp(24));
        inner.setCardElevation(0f);
        inner.setCardBackgroundColor(ThemeManager.previewPrimary(this, colorCode));
        inner.setStrokeWidth(0);

        View lower = new View(this);
        lower.setBackground(roundRect(ThemeManager.previewContainer(this, colorCode), 24));
        FrameLayout.LayoutParams lowerLp = new FrameLayout.LayoutParams(dp(48), dp(24));
        lowerLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        frame.addView(inner, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER));
        frame.addView(lower, lowerLp);

        if (selected) {
            TextView check = text("✓", 16, onPrimary, Typeface.BOLD);
            check.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams checkLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            frame.addView(check, checkLp);
        }

        outer.addView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        outer.setContentDescription(colorCode);
        outer.setOnClickListener(v -> {
            ThemeManager.setColor(this, colorCode);
            recreatePreservingTab();
        });

        return outer;
    }

    private void recreatePreservingTab() {
        Intent intent = getIntent();
        if (intent != null) {
            intent.putExtra(EXTRA_RECREATE_TAB, currentTab);
        }
        recreate();
    }

    private void showTab(int tab) {
        if (tab < TAB_SETUP || tab > TAB_ABOUT) {
            tab = TAB_SETUP;
        }
        currentTab = tab;

        if (setupPage == null) return;

        setupPage.setVisibility(tab == TAB_SETUP ? View.VISIBLE : View.GONE);
        advancedPage.setVisibility(tab == TAB_ADVANCED ? View.VISIBLE : View.GONE);
        supportPage.setVisibility(tab == TAB_SUPPORT ? View.VISIBLE : View.GONE);
        aboutPage.setVisibility(tab == TAB_ABOUT ? View.VISIBLE : View.GONE);
    }

    private String checklistLine(boolean done, String label) {
        return (done ? "✓ " : "⚠ ") + tr(label);
    }

    private void setActionState(
            ActionRow row,
            boolean done,
            String state) {

        if (row == null) return;

        row.status.setText(tr(state));
        row.status.setTextColor(done ? success : onSurfaceVariant);

        // Completed items should read as status, not as a wall of disabled buttons.
        row.button.setVisibility(done ? View.GONE : View.VISIBLE);
        row.button.setEnabled(!done);
    }

    private void setOptionalActionState(
            ActionRow row,
            boolean enabled,
            String state) {

        if (row == null) return;

        row.status.setText(tr(state));
        row.status.setTextColor(enabled ? success : onSurfaceVariant);

        // Optional diagnostics remain actionable when disabled.
        row.button.setVisibility(enabled ? View.GONE : View.VISIBLE);
        row.button.setEnabled(!enabled);
        if (!enabled) row.button.setText(tr("Enable"));
    }

    private void setPackageBatteryState(
            ActionRow row,
            boolean installed,
            boolean done,
            String name) {

        if (row == null) return;

        if (!installed) {
            row.status.setText(name + tr(" is not installed"));
            row.status.setTextColor(onSurfaceVariant);
            row.button.setVisibility(View.VISIBLE);
            row.button.setEnabled(false);
            row.button.setText(tr("Unavailable"));
            return;
        }

        setActionState(
                row,
                done,
                done ? name + tr(" is unrestricted")
                        : name + tr(" is still battery-optimized"));
    }

    private void setManualState(
            MaterialButton open,
            MaterialButton confirm,
            boolean done,
            String doneText,
            String openText) {

        if (open == null || confirm == null) return;

        open.setEnabled(!done);
        confirm.setEnabled(!done);

        open.setVisibility(done ? View.GONE : View.VISIBLE);
        confirm.setVisibility(done ? View.GONE : View.VISIBLE);

        if (!done) {
            open.setText(tr(openText));
        }
    }

    private void setStepStatus(
            TextView view,
            boolean ok,
            String message) {

        if (view == null) return;
        view.setText((ok ? "✓ " : "• ") + tr(message));
        view.setTextColor(ok ? success : warning);
    }

    private ActionRow actionRow(
            String title,
            String description,
            String action,
            View.OnClickListener listener) {

        MaterialCardView shell = nestedCard();

        LinearLayout row = column();
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        row.addView(text(
                title,
                15,
                onSurface,
                Typeface.BOLD));

        TextView desc = supporting(description);
        row.addView(desc, margins(0, 3, 0, 0));

        TextView status = text(
                "Checking…",
                12,
                onSurfaceVariant,
                Typeface.BOLD);
        row.addView(status, margins(0, 6, 0, 8));

        MaterialButton button = compactButton(action);
        button.setOnClickListener(listener);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.addView(button);

        row.addView(actions);

        shell.addView(row);
        return new ActionRow(shell, status, button);
    }


    private void addSectionHeader(
            LinearLayout parent,
            String number,
            String title,
            String description) {

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.TOP);

        TextView numberText = text(
                number,
                14,
                onPrimaryContainer,
                Typeface.BOLD);
        numberText.setGravity(Gravity.CENTER);
        numberText.setMinWidth(dp(36));
        numberText.setMinHeight(dp(36));
        numberText.setPadding(0, 0, 0, 0);
        numberText.setBackground(
                roundRect(primaryContainer, 18));

        LinearLayout.LayoutParams numberLp =
                new LinearLayout.LayoutParams(
                        dp(36),
                        dp(36));
        numberLp.setMargins(0, dp(2), dp(12), 0);
        line.addView(numberText, numberLp);

        LinearLayout copy = column();

        TextView titleView = text(
                title,
                20,
                onSurface,
                Typeface.BOLD);
        titleView.setMaxLines(3);
        copy.addView(titleView);

        TextView d = supporting(description);
        d.setPadding(0, dp(4), 0, 0);
        copy.addView(d);

        line.addView(
                copy,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));

        parent.addView(line);
    }


    private void addGuide(
            LinearLayout parent,
            String buttonText,
            int... drawableIds) {

        MaterialButton toggle = textButton(buttonText);
        toggle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        LinearLayout images = column();
        images.setVisibility(View.GONE);

        toggle.setOnClickListener(v -> {
            boolean show = images.getVisibility() != View.VISIBLE;
            images.setVisibility(show ? View.VISIBLE : View.GONE);
            toggle.setText(show ? tr("Hide guide") : tr(buttonText));
        });

        parent.addView(toggle, margins(0, 6, 0, 0));

        for (int id : drawableIds) {
            MaterialCardView frame = nestedCard();
            frame.setContentPadding(dp(6), dp(6), dp(6), dp(6));

            ImageView image = new ImageView(this);
            image.setImageResource(id);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setContentDescription(tr("ColorOS setup guide"));

            frame.addView(image);
            images.addView(frame, margins(0, 6, 0, 0));
        }

        parent.addView(images);
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { surface, surface, surfaceContainer });
        scroll.setBackground(bg);
        scroll.setClipToPadding(false);
        return scroll;
    }

    private LinearLayout pageRoot() {
        LinearLayout root = column();
        root.setPadding(dp(20), dp(10), dp(20), dp(32));
        return root;
    }

    private FrameLayout.LayoutParams matchFrame() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private MaterialCardView sectionCard() {
        MaterialCardView card = elevatedCard(surfaceContainer, 24, 0f);
        card.setStrokeWidth(0);
        return card;
    }

    private MaterialCardView nestedCard() {
        MaterialCardView card = elevatedCard(surfaceContainerHigh, 20, 0f);
        card.setStrokeWidth(0);
        return card;
    }

    private MaterialCardView elevatedCard(
            int color,
            float radiusDp,
            float elevationDp) {

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(color);
        card.setRadius(dp(radiusDp));
        card.setCardElevation(dp(Math.min(1f, Math.max(0f, elevationDp))));
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setTranslationZ(0f);
        return card;
    }

    private LinearLayout sectionBody() {
        LinearLayout body = column();
        body.setPadding(dp(22), dp(20), dp(22), dp(20));
        return body;
    }

    private TextView sectionEyebrow(String value) {
        TextView t = text(value, 11, onSurfaceVariant, Typeface.BOLD);
        t.setLetterSpacing(0.12f);
        return t;
    }

    private TextView statusText() {
        return text("Checking…", 14, warning, Typeface.BOLD);
    }

    private TextView miniTitle(String value) {
        return text(value, 14, onSurface, Typeface.BOLD);
    }

    private TextView supporting(String value) {
        TextView v = text(value, 15, onSurfaceVariant, Typeface.NORMAL);
        v.setLineSpacing(0, 1.12f);
        return v;
    }

    private MaterialButton filledButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(primary));
        b.setTextColor(onPrimary);
        b.setElevation(0f);
        return b;
    }

    private MaterialButton filledTonalButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(primaryContainer));
        b.setTextColor(onPrimaryContainer);
        b.setElevation(0f);
        return b;
    }

    private MaterialButton outlinedButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(Color.TRANSPARENT));
        b.setTextColor(primary);
        b.setStrokeColor(ColorStateList.valueOf(outline));
        b.setStrokeWidth(dp(1));
        return b;
    }

    private MaterialButton textButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(Color.TRANSPARENT));
        b.setTextColor(primary);
        b.setInsetTop(0);
        b.setInsetBottom(0);
        return b;
    }

    private MaterialButton compactButton(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setAllCaps(false);
        b.setText(tr(label));
        b.setTextSize(13);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setMinHeight(dp(40));
        b.setMinWidth(dp(72));
        b.setCornerRadius(dp(20));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setBackgroundTintList(flatTint(primaryContainer));
        b.setTextColor(onPrimaryContainer);
        b.setRippleColor(primaryStateLayer());
        b.setStateListAnimator(null);
        b.setElevation(0f);
        applyPressDepth(b, 0f, 0f);
        return b;
    }

    private MaterialButton baseButton(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setAllCaps(false);
        b.setText(tr(label));
        b.setTextSize(15);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setMinHeight(dp(48));
        b.setCornerRadius(dp(24));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setRippleColor(primaryStateLayer());
        b.setStateListAnimator(null);
        b.setElevation(0f);
        applyPressDepth(b, 0f, 0f);
        return b;
    }

    private ColorStateList flatTint(int color) {
        return new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_pressed },
                        new int[] { android.R.attr.state_focused },
                        new int[] { android.R.attr.state_hovered },
                        new int[] {}
                },
                new int[] { color, color, color, color });
    }

    private ColorStateList primaryStateLayer() {
        int alpha = ThemeManager.isDark(this) ? 52 : 34;
        int state = Color.argb(
                alpha,
                Color.red(primary),
                Color.green(primary),
                Color.blue(primary));
        return ColorStateList.valueOf(state);
    }

    private TextView pill(String value, int bg, int fg) {
        TextView t = text(value, 11, fg, Typeface.BOLD);
        t.setPadding(dp(12), dp(7), dp(12), dp(7));
        t.setBackground(roundRect(bg, 14));
        return t;
    }

    private TextView text(
            String value,
            float sp,
            int color,
            int style) {

        TextView t = new TextView(this);
        t.setText(tr(value));
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", style));
        if (Build.VERSION.SDK_INT >= 23) {
            t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
            t.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        }
        return t;
    }

    private android.graphics.drawable.GradientDrawable roundRect(
            int color,
            float radiusDp) {

        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams margins(
            int left,
            int top,
            int right,
            int bottom) {

        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private int dp(float value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }

    private String tr(String source) {
        return UiText.tr(this, source);
    }

    private void copy(String label, String value) {
        ClipboardManager cm =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, value));
    }



private void requestLogSessionReconnect(
        boolean force) {

    if (!isReadLogsGranted()) {
        Toast.makeText(
                this,
                tr("READ_LOGS is not granted. Complete privileged setup first."),
                Toast.LENGTH_LONG).show();
        return;
    }

    boolean enabled =
            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE)
                    .getBoolean(
                            PREF_ENABLED,
                            false);

    if (!enabled) {
        if (force) {
            Toast.makeText(
                    this,
                    tr("Start the watcher first. The privileged logcat session is created when the watcher starts."),
                    Toast.LENGTH_LONG).show();
        }
        return;
    }

    if (!force
            && (watcherSessionState
                    == WatcherIpc.STATE_ACTIVE
                    || watcherSessionState
                    == WatcherIpc.STATE_CONNECTING)) {
        return;
    }

    startWatcher();
    bindWatcher();
    syncWatcherPrefs();

    boolean sent =
            sendWatcherMessage(
                    WatcherIpc.MSG_RECONNECT_LOGCAT,
                    null,
                    false);

    if (!sent) {
        Intent reconnect =
                new Intent(
                        this,
                        HomeHoldService.class);

        reconnect.setAction(
                HomeHoldService.ACTION_RECONNECT_LOGCAT);

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(reconnect);
            } else {
                startService(reconnect);
            }
        } catch (Throwable e) {
            Toast.makeText(
                    this,
                    tr("Unable to request a new device-log access session."),
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    statusPollRemaining = 40;
    ui.removeCallbacks(pcDetectLoop);
    ui.post(pcDetectLoop);

    if (force) {
        Toast.makeText(
                this,
                tr("Android may require device-log access confirmation before a new privileged logcat session can start."),
                Toast.LENGTH_LONG).show();
    }
}

private void updateLogSessionRow(
        boolean readLogs,
        boolean running,
        int state) {

    if (logSessionRow == null) {
        return;
    }

    if (!readLogs) {
        logSessionRow.status.setText(
                tr("READ_LOGS permission is required first"));
        logSessionRow.status.setTextColor(warning);
        logSessionRow.button.setVisibility(View.GONE);
        return;
    }

    if (!running) {
        logSessionRow.status.setText(
                tr("Not started · Run will request privileged logcat access"));
        logSessionRow.status.setTextColor(onSurfaceVariant);
        logSessionRow.button.setVisibility(View.GONE);
        return;
    }

    if (state
            == WatcherIpc.STATE_ACTIVE) {
        logSessionRow.status.setText(
                tr("Device-log access session active"));
        logSessionRow.status.setTextColor(success);
        logSessionRow.button.setVisibility(View.GONE);
        return;
    }

    logSessionRow.button.setVisibility(View.VISIBLE);

    if (state
            == WatcherIpc.STATE_CONNECTING) {
        logSessionRow.status.setText(
                tr("Waiting for Android log-access confirmation dialog"));
        logSessionRow.status.setTextColor(warning);
        logSessionRow.button.setEnabled(false);
        logSessionRow.button.setText(tr("Waiting"));
        return;
    }

    if (state
            == WatcherIpc.STATE_NO_PERMISSION) {
        logSessionRow.status.setText(
                tr("READ_LOGS permission unavailable"));
        logSessionRow.status.setTextColor(error);
        logSessionRow.button.setEnabled(false);
        logSessionRow.button.setText(tr("Unavailable"));
        return;
    }

    logSessionRow.status.setText(
            tr("Foreground ready · requesting a new logcat access session"));
    logSessionRow.status.setTextColor(warning);
    logSessionRow.button.setEnabled(true);
    logSessionRow.button.setText(tr("Reconnect"));
}


private void bindWatcher() {
    if (watcherBound) {
        return;
    }

    boolean enabled =
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getBoolean(PREF_ENABLED, false);

    if (!enabled) {
        return;
    }

    try {
        Intent i =
                new Intent(
                        this,
                        HomeHoldService.class);

        bindService(
                i,
                watcherConnection,
                Context.BIND_AUTO_CREATE);
    } catch (Throwable ignored) {
    }
}

private void unbindWatcher() {
    if (!watcherBound) {
        return;
    }

    sendWatcherMessage(
            WatcherIpc.MSG_UNREGISTER_CLIENT,
            null,
            true);

    try {
        unbindService(watcherConnection);
    } catch (Throwable ignored) {
    }

    watcherBound = false;
    watcherMessenger = null;
}

private boolean sendWatcherMessage(
        int what,
        Bundle data,
        boolean includeReplyTo) {

    Messenger target =
            watcherMessenger;

    if (!watcherBound || target == null) {
        return false;
    }

    Message msg =
            Message.obtain(
                    null,
                    what);

    if (data != null) {
        msg.setData(data);
    }

    if (includeReplyTo) {
        msg.replyTo = uiMessenger;
    }

    try {
        target.send(msg);
        return true;

    } catch (RemoteException e) {
        watcherBound = false;
        watcherMessenger = null;
        return false;
    }
}

private void syncWatcherPrefs() {
    if (!watcherBound) {
        return;
    }

    android.content.SharedPreferences prefs =
            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE);

    Bundle data =
            new Bundle();

    data.putInt(
            WatcherIpc.KEY_DELAY_MS,
            prefs.getInt(
                    PREF_CTS_DELAY_MS,
                    DEFAULT_CTS_DELAY_MS));

    data.putBoolean(
            WatcherIpc.KEY_VIBRATE,
            prefs.getBoolean(
                    PREF_VIBRATE_ON_CTS,
                    DEFAULT_VIBRATE_ON_CTS));

    data.putBoolean(
            WatcherIpc.KEY_SOUND,
            prefs.getBoolean(
                    PREF_SOUND_ON_ACTIVATION,
                    DEFAULT_SOUND_ON_ACTIVATION));

    data.putBoolean(
            WatcherIpc.KEY_POWER_GEMINI,
            prefs.getBoolean(
                    PREF_POWER_GEMINI_EXPERIMENTAL,
                    DEFAULT_POWER_GEMINI_EXPERIMENTAL));

    sendWatcherMessage(
            WatcherIpc.MSG_SYNC_PREFS,
            data,
            false);
}

    private void startWatcher() {
        Intent i = new Intent(this, HomeHoldService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private boolean isWatcherRunning() {
        if (watcherBound) {
            return true;
        }

        ActivityManager am =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) return false;

        try {
            List<ActivityManager.RunningServiceInfo> list =
                    am.getRunningServices(Integer.MAX_VALUE);

            for (ActivityManager.RunningServiceInfo s : list) {
                ComponentName c = s.service;

                if (c != null
                        && getPackageName().equals(c.getPackageName())
                        && HomeHoldService.class.getName()
                        .equals(c.getClassName())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private boolean isReadLogsGranted() {
        return checkSelfPermission("android.permission.READ_LOGS")
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isIgnoringBattery(String pkg) {
        try {
            PowerManager pm =
                    (PowerManager) getSystemService(POWER_SERVICE);
            return pm != null
                    && pm.isIgnoringBatteryOptimizations(pkg);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean packageExists(String pkg) {
        try {
            getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean pref(String key) {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(key, false);
    }

    private ColorStateList makeNavColorStateList() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };

        int[] colors = new int[] {
                primary,
                onSurfaceVariant
        };

        return new ColorStateList(states, colors);
    }

    private void loadColors() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        primary = p.primary;
        onPrimary = p.onPrimary;
        primaryContainer = p.primaryContainer;
        onPrimaryContainer = p.onPrimaryContainer;
        surface = p.surface;
        surfaceContainer = p.surfaceContainer;
        surfaceContainerHigh = p.surfaceContainerHigh;
        onSurface = p.onSurface;
        onSurfaceVariant = p.onSurfaceVariant;
        outline = p.outline;
        success = p.success;
        warning = p.warning;
        error = p.error;
    }

    private int color(int resId) {
        return ContextCompat.getColor(this, resId);
    }

    private void configureWindow() {
        Window w = getWindow();
        w.setStatusBarColor(surface);
        w.setNavigationBarColor(surface);

        View decor = w.getDecorView();
        int flags = decor.getSystemUiVisibility();

        if (!ThemeManager.isDark(this)) {
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }

        decor.setSystemUiVisibility(flags);
    }

    private static final class ActionRow {
        final MaterialCardView root;
        final TextView status;
        final MaterialButton button;

        ActionRow(
                MaterialCardView root,
                TextView status,
                MaterialButton button) {

            this.root = root;
            this.status = status;
            this.button = button;
        }
    }
}
