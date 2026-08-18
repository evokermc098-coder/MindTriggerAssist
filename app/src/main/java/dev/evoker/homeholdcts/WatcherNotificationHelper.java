// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

final class WatcherNotificationHelper {

    static final String CHANNEL = "watcher";
    static final int WATCHER_NOTIFICATION_ID = 7042;

    private WatcherNotificationHelper() {}

    static Notification buildWatcherNotification(
            Context context,
            int logSessionState) {

        Intent open =
                new Intent(
                        context,
                        MainActivity.class);

        open.addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= 23) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        context,
                        1,
                        open,
                        piFlags);

        Notification.Builder b;

        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(context, CHANNEL);
        } else {
            b = new Notification.Builder(context);
        }

        String text;

        if (logSessionState == WatcherIpc.STATE_ACTIVE) {
            text =
                    "Privileged logcat active · watcher online";

        } else if (logSessionState == WatcherIpc.STATE_CONNECTING) {
            text =
                    "Approve Android device-log access to finish reconnecting";

        } else if (logSessionState == WatcherIpc.STATE_NO_PERMISSION) {
            text =
                    "READ_LOGS unavailable · open MindTrigger Assist";

        } else if (Build.VERSION.SDK_INT >= 23
                && !Settings.canDrawOverlays(context)) {
            text =
                    "Automatic recovery unavailable · Display over other apps required";

        } else {
            text =
                    "Open MindTrigger Assist to restore privileged logcat access";
        }

        b.setSmallIcon(R.drawable.ic_stat_watch)
                .setContentTitle(UiText.tr(context, "MindTrigger Assist watcher"))
                .setContentText(UiText.tr(context, text))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE);

        return b.build();
    }
}
