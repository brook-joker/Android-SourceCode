/*
 * Copyright (C) 2011 The Android Open Source Project
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

package frameworks.base.core.java.android.view;

import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;

import java.util.Objects;

/** @hide */
public class DisplayAdjustments {
    public static final DisplayAdjustments DEFAULT_DISPLAY_ADJUSTMENTS = new DisplayAdjustments();

    private volatile CompatibilityInfo mCompatInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
    private Configuration mConfiguration;

    public DisplayAdjustments() {
    }

    public DisplayAdjustments(Configuration configuration) {
        mConfiguration = new Configuration(configuration != null
                ? configuration : Configuration.EMPTY);
    }

    public DisplayAdjustments(DisplayAdjustments daj) {
        setCompatibilityInfo(daj.mCompatInfo);
        mConfiguration = new Configuration(daj.mConfiguration != null
                ? daj.mConfiguration : Configuration.EMPTY);
    }

    public void setCompatibilityInfo(CompatibilityInfo compatInfo) {
        if (this == DEFAULT_DISPLAY_ADJUSTMENTS) {
            throw new IllegalArgumentException(
                    "setCompatbilityInfo: Cannot modify DEFAULT_DISPLAY_ADJUSTMENTS");
        }
        if (compatInfo != null && (compatInfo.isScalingRequired()
                || !compatInfo.supportsScreen())) {
            mCompatInfo = compatInfo;
        } else {
            mCompatInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
        }
    }

    public CompatibilityInfo getCompatibilityInfo() {
        return mCompatInfo;
    }

    public void setConfiguration(Configuration configuration) {
        if (this == DEFAULT_DISPLAY_ADJUSTMENTS) {
            throw new IllegalArgumentException(
                    "setConfiguration: Cannot modify DEFAULT_DISPLAY_ADJUSTMENTS");
        }
        mConfiguration.setTo(configuration != null ? configuration : Configuration.EMPTY);
    }

    public Configuration getConfiguration() {
        return mConfiguration;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Objects.hashCode(mCompatInfo);
        hash = hash * 31 + Objects.hashCode(mConfiguration);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DisplayAdjustments)) {
            return false;
        }
        DisplayAdjustments daj = (DisplayAdjustments)o;
        return Objects.equals(daj.mCompatInfo, mCompatInfo) &&
                Objects.equals(daj.mConfiguration, mConfiguration);
    }
}
