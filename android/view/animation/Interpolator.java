/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.view.animation;

import android.animation.TimeInterpolator;

/**
 * An interpolator defines the rate of change of an animation. This allows
 * the basic animation effects (alpha, scale, translate, rotate) to be 
 * accelerated, decelerated, repeated, etc.
 * 插值器定义动画的变化率。 这允许加速，减速，重复等基本动画效果（alpha，缩放，平移，旋转）。
 *
 */
public interface Interpolator extends TimeInterpolator {
    // A new interface, TimeInterpolator, was introduced for the new android.animation
    // package. This older Interpolator interface extends TimeInterpolator so that users of
    // the new Animator-based animations can use either the old Interpolator implementations or
    // new classes that implement TimeInterpolator directly.

    //为新的android.animation包引入了一个新的接口TimeInterpolator。
    //这个较旧的Interpolator接口扩展了TimeInterpolator，
    //因此新的基于Animator的动画的用户可以使用旧的Interpolator实现或直接实现TimeInterpolator的新类。
}
