// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.util.Log;

/**
 * Per-activation Google warm-up.
 *
 * This does not launch Google's UI. It opportunistically acquires an exported
 * Google ContentProvider. Acquiring a provider asks ActivityManager to bring the
 * hosting Google process up if it was reclaimed while multitasking.
 */
final class GoogleReviver {

    private static final String TAG = "HomeHoldCTS";
    static final String GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox";

    // Interactor-hosted providers first, then a search-process fallback.
    private static final String[] CANDIDATE_AUTHORITIES = new String[] {
            "com.google.android.apps.gsa.voiceinteraction.dump.StateDumpProvider",
            "com.google.android.apps.gsa.voiceinteraction.hotword.HotwordAudioProvider",
            "com.google.android.googlequicksearchbox.GsaPublicContentProvider"
    };

    private GoogleReviver() {}

    static PrimeResult prime(Context context) {
        PackageManager pm = context.getPackageManager();
        int attempted = 0;
        int acquired = 0;

        for (String authority : CANDIDATE_AUTHORITIES) {
            ProviderInfo info;
            try {
                info = pm.resolveContentProvider(authority, PackageManager.GET_META_DATA);
            } catch (Throwable t) {
                continue;
            }

            if (info == null || !GOOGLE_PACKAGE.equals(info.packageName)) {
                continue;
            }

            // Never bypass provider permissions. Only touch a provider Android says
            // this UID is allowed to acquire.
            if (!info.exported) {
                continue;
            }
            if (info.readPermission != null
                    && context.checkSelfPermission(info.readPermission)
                    != PackageManager.PERMISSION_GRANTED) {
                continue;
            }

            attempted++;
            ContentProviderClient client = null;
            try {
                Uri uri = Uri.parse("content://" + authority + "/");
                client = context.getContentResolver()
                        .acquireUnstableContentProviderClient(uri);
                if (client != null) {
                    acquired++;
                    Log.d(TAG, "Google primer acquired " + authority
                            + " process=" + info.processName);
                    // One successful interactor/search process wake is enough.
                    break;
                }
            } catch (SecurityException ignored) {
                // Provider exists but runtime policy denied access; skip it.
            } catch (Throwable t) {
                Log.d(TAG, "Google primer skipped " + authority + ": " + t);
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Throwable ignored) {}
                }
            }
        }

        return new PrimeResult(attempted, acquired);
    }

    static final class PrimeResult {
        final int attempted;
        final int acquired;

        PrimeResult(int attempted, int acquired) {
            this.attempted = attempted;
            this.acquired = acquired;
        }

        boolean wokeSomething() {
            return acquired > 0;
        }
    }
}
