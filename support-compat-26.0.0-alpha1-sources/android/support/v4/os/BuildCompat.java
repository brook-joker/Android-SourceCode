/*
 * Copyright (C) 2016 The Android Open Source Project
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


package android.support.v4.os;

import android.os.Build.VERSION;

/**
 * BuildCompat contains additional platform version checking methods for
 * testing compatibility with new features.
 */
public class BuildCompat {
    private BuildCompat() {
    }
    /* Boilerplate for isAtLeast${PLATFORM}:
     * public static boolean isAtLeast*() {
     *     return !"REL".equals(VERSION.CODENAME)
     *             && ("${PLATFORM}".equals(VERSION.CODENAME)
     *                     || VERSION.CODENAME.startsWith("${PLATFORM}MR"));
     * }
     */

    /**
     * Check if the device is running on the Android N release or newer.
     *
     * @return {@code true} if N APIs are available for use
     */
    public static boolean isAtLeastN() {
        return VERSION.SDK_INT >= 24;
    }

    /**
     * Check if the device is running on the Android N MR1 release or newer.
     *
     * @return {@code true} if N MR1 APIs are available for use
     */
    public static boolean isAtLeastNMR1() {
        return VERSION.SDK_INT >= 25;
    }

    /**
     * Check if the device is running on the Android O release or newer.
     *
     * @return {@code true} if O APIs are available for use
     */
    public static boolean isAtLeastO() {
        return !"REL".equals(VERSION.CODENAME)
                && ("O".equals(VERSION.CODENAME) || VERSION.CODENAME.startsWith("OMR"));
    }
}
