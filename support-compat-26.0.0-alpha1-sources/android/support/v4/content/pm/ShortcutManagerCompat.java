/**
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v4.content.pm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.BuildCompat;
import android.text.TextUtils;

/**
 * Helper for accessing features in {@link android.content.pm.ShortcutManager}
 * in a backwards compatible fashion.
 */
public class ShortcutManagerCompat {

    @VisibleForTesting static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";
    @VisibleForTesting static final String INSTALL_SHORTCUT_PERMISSION =
            "com.android.launcher.permission.INSTALL_SHORTCUT";

    /**
     * @return {@code true} if the launcher supports {@link #requestPinShortcut},
     * {@code false} otherwise
     */
    public static boolean isRequestPinShortcutSupported(@NonNull Context context) {
        if (BuildCompat.isAtLeastO()) {
            return ShortcutManagerCompatApi26.isRequestPinShortcutSupported(context);
        }

        if (ContextCompat.checkSelfPermission(context, INSTALL_SHORTCUT_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        for (ResolveInfo info : context.getPackageManager().queryBroadcastReceivers(
                new Intent(ACTION_INSTALL_SHORTCUT), 0)) {
            String permission = info.activityInfo.permission;
            if (TextUtils.isEmpty(permission) || INSTALL_SHORTCUT_PERMISSION.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Request to create a pinned shortcut.
     * <p>On API <= 25 it creates a legacy shortcut with the provided icon, label and intent. For
     * newer APIs it will create a {@link android.content.pm.ShortcutInfo} object which can be
     * updated by the app.
     *
     * <p>Use {@link android.app.PendingIntent#getIntentSender()} to create a {@link IntentSender}.
     *
     * @param shortcut new shortcut to pin
     * @param callback if not null, this intent will be sent when the shortcut is pinned
     *
     * @return {@code true} if the launcher supports this feature
     *
     * @see #isRequestPinShortcutSupported
     * @see IntentSender
     * @see android.app.PendingIntent#getIntentSender()
     */
    public static boolean requestPinShortcut(@NonNull final Context context,
            @NonNull ShortcutInfoCompat shortcut, @Nullable final IntentSender callback) {
        if (BuildCompat.isAtLeastO()) {
            return ShortcutManagerCompatApi26.requestPinShortcut(context, shortcut, callback);
        }

        if (!isRequestPinShortcutSupported(context)) {
            return false;
        }
        Intent intent = shortcut.addToIntent(new Intent(ACTION_INSTALL_SHORTCUT));

        // If the callback is null, just send the broadcast
        if (callback == null) {
            context.sendBroadcast(intent);
            return true;
        }

        // Otherwise send the callback when the intent has successfully been dispatched.
        context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    callback.sendIntent(context, 0, null, null, null);
                } catch (IntentSender.SendIntentException e) {
                    // Ignore
                }
            }
        }, null, Activity.RESULT_OK, null, null);
        return true;
    }

    /**
     * Returns an Intent which can be used by the launcher to pin shortcut.
     * <p>This should be used by an Activity to set result in response to
     * {@link Intent#ACTION_CREATE_SHORTCUT}.
     *
     * @param shortcut new shortcut to pin
     * @return the intent that should be set as the result for the calling activity
     *
     * @see Intent#ACTION_CREATE_SHORTCUT
     */
    @NonNull
    public static Intent createShortcutResultIntent(@NonNull Context context,
            @NonNull ShortcutInfoCompat shortcut) {
        Intent result = null;
        if (BuildCompat.isAtLeastO()) {
            result = ShortcutManagerCompatApi26.createShortcutResultIntent(context, shortcut);
        }
        if (result == null) {
            result = new Intent();
        }
        return shortcut.addToIntent(result);
    }
}
