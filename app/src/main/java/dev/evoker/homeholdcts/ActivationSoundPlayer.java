// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

final class ActivationSoundPlayer {

    private static final String TAG = "HomeHoldCTS";

    private ActivationSoundPlayer() {}

    static void playCts(Context context) {
        play(context, R.raw.aura_cts);
    }

    static void playAssistant(Context context) {
        play(context, R.raw.aura_gemini);
    }

    private static void play(Context context, int rawResId) {
        MediaPlayer player = new MediaPlayer();

        try {
            AudioAttributes attributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();

            player.setAudioAttributes(attributes);

            android.content.res.AssetFileDescriptor afd =
                    context.getResources().openRawResourceFd(rawResId);

            if (afd == null) {
                player.release();
                return;
            }

            try {
                player.setDataSource(
                        afd.getFileDescriptor(),
                        afd.getStartOffset(),
                        afd.getLength());
            } finally {
                afd.close();
            }

            player.setOnCompletionListener(MediaPlayer::release);
            player.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                return true;
            });

            player.prepare();
            player.start();

        } catch (Throwable error) {
            try {
                player.release();
            } catch (Throwable ignored) {
            }
            Log.w(TAG, "Activation ringtone sound failed", error);
        }
    }
}
