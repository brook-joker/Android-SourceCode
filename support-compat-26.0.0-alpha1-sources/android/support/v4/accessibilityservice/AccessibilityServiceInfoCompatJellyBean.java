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

package android.support.v4.accessibilityservice;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.PackageManager;
import android.support.annotation.RequiresApi;

/**
 * JB implementation of the new APIs in AccessibilityServiceInfo.
 */

@RequiresApi(16)
class AccessibilityServiceInfoCompatJellyBean {

    public static String loadDescription(AccessibilityServiceInfo info, PackageManager pm) {
        return info.loadDescription(pm);
    }
}
