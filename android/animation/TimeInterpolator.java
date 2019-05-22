/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.animation;

/**
 * A time interpolator defines the rate of change of an animation. This allows animations
 * to have non-linear motion, such as acceleration and deceleration.
 * 时间插值器定义动画的变化率。 这允许动画具有非线性运动，例如加速和减速。
 */
public interface TimeInterpolator {

    /**
     * Maps a value representing the elapsed fraction of an animation to a value that represents
     * the interpolated fraction. This interpolated value is then multiplied by the change in
     * value of an animation to derive the animated value at the current elapsed animation time.
     * 将表示动画的已用分数的值映射到表示插值分数的值。
     * 然后将该内插值乘以动画的值的变化，以导出当前经过的动画时间的动画值。
     *
     * @param input A value between 0 and 1.0 indicating our current point
     *        in the animation where 0 represents the start and 1.0 represents
     *        the end
     *
     *        输入介于0和1.0之间的值，表示动画中的当前点，其中0表示开始，1.0表示结束
     *
     * @return The interpolation value. This value can be more than 1.0 for
     *         interpolators which overshoot their targets, or less than 0 for
     *         interpolators that undershoot their targets.
     *         插值。 对于超出其目标的插值器，该值可以大于1.0;
     *         对于低于其目标的插值器，该值可以小于0。
     *
     */
    float getInterpolation(float input);
}
