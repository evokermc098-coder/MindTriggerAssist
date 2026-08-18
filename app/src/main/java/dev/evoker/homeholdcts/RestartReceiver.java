// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

public class RestartReceiver extends BroadcastReceiver {

    public static final String ACTION_RESTART =
            "dev.evoker.homeholdcts.RESTART";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = context
                .getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .getBoolean(MainActivity.PREF_ENABLED, false);

        if (!enabled) {
            return;
        }

        Intent service = new Intent(context, HomeHoldService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Throwable ignored) {
            // If ColorOS blocks this particular restart path, the next boot/user
            // start still restores the service. onTaskRemoved also schedules us.
        }
    }

    public static void scheduleRestart(Context context, long delayMs) {
        try {
            Intent i = new Intent(context, RestartReceiver.class);
            i.setAction(ACTION_RESTART);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, 22, i, flags);

            AlarmManager am =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            long when = SystemClock.elapsedRealtime() + delayMs;
            if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            } else {
                am.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            }
        } catch (Throwable ignored) {
        }
    }
}
