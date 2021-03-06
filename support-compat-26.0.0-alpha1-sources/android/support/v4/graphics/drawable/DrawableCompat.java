/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.InsetDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Drawable}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class DrawableCompat {
    /**
     * Interface implementation that doesn't use anything about v4 APIs.
     */
    static class DrawableCompatBaseImpl {
        public void jumpToCurrentState(Drawable drawable) {
            drawable.jumpToCurrentState();
        }

        public void setAutoMirrored(Drawable drawable, boolean mirrored) {
        }

        public boolean isAutoMirrored(Drawable drawable) {
            return false;
        }

        public void setHotspot(Drawable drawable, float x, float y) {
        }

        public void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom) {
        }

        public void setTint(Drawable drawable, int tint) {
            if (drawable instanceof TintAwareDrawable) {
                ((TintAwareDrawable) drawable).setTint(tint);
            }
        }

        public void setTintList(Drawable drawable, ColorStateList tint) {
            if (drawable instanceof TintAwareDrawable) {
                ((TintAwareDrawable) drawable).setTintList(tint);
            }
        }

        public void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
            if (drawable instanceof TintAwareDrawable) {
                ((TintAwareDrawable) drawable).setTintMode(tintMode);
            }
        }

        public Drawable wrap(Drawable drawable) {
            if (!(drawable instanceof TintAwareDrawable)) {
                return new DrawableWrapperApi14(drawable);
            }
            return drawable;
        }

        public boolean setLayoutDirection(Drawable drawable, int layoutDirection) {
            // No op for API < 23
            return false;
        }

        public int getLayoutDirection(Drawable drawable) {
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        }

        public int getAlpha(Drawable drawable) {
            return 0;
        }

        public void applyTheme(Drawable drawable, Resources.Theme t) {
        }

        public boolean canApplyTheme(Drawable drawable) {
            return false;
        }

        public ColorFilter getColorFilter(Drawable drawable) {
            return null;
        }

        public void clearColorFilter(Drawable drawable) {
            drawable.clearColorFilter();
        }

        public void inflate(Drawable drawable, Resources res, XmlPullParser parser,
                            AttributeSet attrs, Resources.Theme t)
                throws IOException, XmlPullParserException {
            drawable.inflate(res, parser, attrs);
        }
    }

    @RequiresApi(17)
    static class DrawableCompatApi17Impl extends DrawableCompatBaseImpl {
        private static final String TAG = "DrawableCompatApi17";

        private static Method sSetLayoutDirectionMethod;
        private static boolean sSetLayoutDirectionMethodFetched;

        private static Method sGetLayoutDirectionMethod;
        private static boolean sGetLayoutDirectionMethodFetched;

        @Override
        public boolean setLayoutDirection(Drawable drawable, int layoutDirection) {
            if (!sSetLayoutDirectionMethodFetched) {
                try {
                    sSetLayoutDirectionMethod =
                            Drawable.class.getDeclaredMethod("setLayoutDirection", int.class);
                    sSetLayoutDirectionMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    Log.i(TAG, "Failed to retrieve setLayoutDirection(int) method", e);
                }
                sSetLayoutDirectionMethodFetched = true;
            }

            if (sSetLayoutDirectionMethod != null) {
                try {
                    sSetLayoutDirectionMethod.invoke(drawable, layoutDirection);
                    return true;
                } catch (Exception e) {
                    Log.i(TAG, "Failed to invoke setLayoutDirection(int) via reflection", e);
                    sSetLayoutDirectionMethod = null;
                }
            }
            return false;
        }

        @Override
        public int getLayoutDirection(Drawable drawable) {
            if (!sGetLayoutDirectionMethodFetched) {
                try {
                    sGetLayoutDirectionMethod = Drawable.class.getDeclaredMethod("getLayoutDirection");
                    sGetLayoutDirectionMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    Log.i(TAG, "Failed to retrieve getLayoutDirection() method", e);
                }
                sGetLayoutDirectionMethodFetched = true;
            }

            if (sGetLayoutDirectionMethod != null) {
                try {
                    return (int) sGetLayoutDirectionMethod.invoke(drawable);
                } catch (Exception e) {
                    Log.i(TAG, "Failed to invoke getLayoutDirection() via reflection", e);
                    sGetLayoutDirectionMethod = null;
                }
            }
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        }
    }

    /**
     * Interface implementation for devices with at least KitKat APIs.
     */
    @RequiresApi(19)
    static class DrawableCompatApi19Impl extends DrawableCompatApi17Impl {
        @Override
        public void setAutoMirrored(Drawable drawable, boolean mirrored) {
            drawable.setAutoMirrored(mirrored);
        }

        @Override
        public boolean isAutoMirrored(Drawable drawable) {
            return drawable.isAutoMirrored();
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            if (!(drawable instanceof TintAwareDrawable)) {
                return new DrawableWrapperApi19(drawable);
            }
            return drawable;
        }

        @Override
        public int getAlpha(Drawable drawable) {
            return drawable.getAlpha();
        }
    }

    /**
     * Interface implementation for devices with at least L APIs.
     */
    @RequiresApi(21)
    static class DrawableCompatApi21Impl extends DrawableCompatApi19Impl {
        @Override
        public void setHotspot(Drawable drawable, float x, float y) {
            drawable.setHotspot(x, y);
        }

        @Override
        public void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom) {
            drawable.setHotspotBounds(left, top, right, bottom);
        }

        @Override
        public void setTint(Drawable drawable, int tint) {
            drawable.setTint(tint);
        }

        @Override
        public void setTintList(Drawable drawable, ColorStateList tint) {
            drawable.setTintList(tint);
        }

        @Override
        public void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
            drawable.setTintMode(tintMode);
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            if (!(drawable instanceof TintAwareDrawable)) {
                return new DrawableWrapperApi21(drawable);
            }
            return drawable;
        }

        @Override
        public void applyTheme(Drawable drawable, Resources.Theme t) {
            drawable.applyTheme(t);
        }

        @Override
        public boolean canApplyTheme(Drawable drawable) {
            return drawable.canApplyTheme();
        }

        @Override
        public ColorFilter getColorFilter(Drawable drawable) {
            return drawable.getColorFilter();
        }

        @Override
        public void clearColorFilter(Drawable drawable) {
            drawable.clearColorFilter();

            // API 21 + 22 have an issue where clearing a color filter on a DrawableContainer
            // will not propagate to all of its children. To workaround this we unwrap the drawable
            // to find any DrawableContainers, and then unwrap those to clear the filter on its
            // children manually
            if (drawable instanceof InsetDrawable) {
                clearColorFilter(((InsetDrawable) drawable).getDrawable());
            } else if (drawable instanceof DrawableWrapper) {
                clearColorFilter(((DrawableWrapper) drawable).getWrappedDrawable());
            } else if (drawable instanceof DrawableContainer) {
                final DrawableContainer container = (DrawableContainer) drawable;
                final DrawableContainer.DrawableContainerState state =
                        (DrawableContainer.DrawableContainerState) container.getConstantState();
                if (state != null) {
                    Drawable child;
                    for (int i = 0, count = state.getChildCount(); i < count; i++) {
                        child = state.getChild(i);
                        if (child != null) {
                            clearColorFilter(child);
                        }
                    }
                }
            }
        }

        @Override
        public void inflate(Drawable drawable, Resources res, XmlPullParser parser,
                            AttributeSet attrs, Resources.Theme t)
                throws IOException, XmlPullParserException {
            drawable.inflate(res, parser, attrs, t);
        }
    }

    /**
     * Interface implementation for devices with at least M APIs.
     */
    @RequiresApi(23)
    static class DrawableCompatApi23Impl extends DrawableCompatApi21Impl {
        @Override
        public boolean setLayoutDirection(Drawable drawable, int layoutDirection) {
            return drawable.setLayoutDirection(layoutDirection);
        }

        @Override
        public int getLayoutDirection(Drawable drawable) {
            return drawable.getLayoutDirection();
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            // No need to wrap on M+
            return drawable;
        }

        @Override
        public void clearColorFilter(Drawable drawable) {
            // We can use clearColorFilter() safely on M+
            drawable.clearColorFilter();
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final DrawableCompatBaseImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 23) {
            IMPL = new DrawableCompatApi23Impl();
        } else if (version >= 21) {
            IMPL = new DrawableCompatApi21Impl();
        } else if (version >= 19) {
            IMPL = new DrawableCompatApi19Impl();
        } else if (version >= 17) {
            IMPL = new DrawableCompatApi17Impl();
        } else {
            IMPL = new DrawableCompatBaseImpl();
        }
    }

    /**
     * Call {@link Drawable#jumpToCurrentState() Drawable.jumpToCurrentState()}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#HONEYCOMB}
     * device this method does nothing.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static void jumpToCurrentState(@NonNull Drawable drawable) {
        IMPL.jumpToCurrentState(drawable);
    }

    /**
     * Set whether this Drawable is automatically mirrored when its layout
     * direction is RTL (right-to left). See
     * {@link android.util.LayoutDirection}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#KITKAT} device
     * this method does nothing.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param mirrored Set to true if the Drawable should be mirrored, false if
     *            not.
     */
    public static void setAutoMirrored(@NonNull Drawable drawable, boolean mirrored) {
        IMPL.setAutoMirrored(drawable, mirrored);
    }

    /**
     * Tells if this Drawable will be automatically mirrored when its layout
     * direction is RTL right-to-left. See {@link android.util.LayoutDirection}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#KITKAT} device
     * this method returns false.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @return boolean Returns true if this Drawable will be automatically
     *         mirrored.
     */
    public static boolean isAutoMirrored(@NonNull Drawable drawable) {
        return IMPL.isAutoMirrored(drawable);
    }

    /**
     * Specifies the hotspot's location within the drawable.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param x The X coordinate of the center of the hotspot
     * @param y The Y coordinate of the center of the hotspot
     */
    public static void setHotspot(@NonNull Drawable drawable, float x, float y) {
        IMPL.setHotspot(drawable, x, y);
    }

    /**
     * Sets the bounds to which the hotspot is constrained, if they should be
     * different from the drawable bounds.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static void setHotspotBounds(@NonNull Drawable drawable, int left, int top,
            int right, int bottom) {
        IMPL.setHotspotBounds(drawable, left, top, right, bottom);
    }

    /**
     * Specifies a tint for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color to use for tinting this drawable
     */
    public static void setTint(@NonNull Drawable drawable, @ColorInt int tint) {
        IMPL.setTint(drawable, tint);
    }

    /**
     * Specifies a tint for {@code drawable} as a color state list.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color state list to use for tinting this drawable, or null to clear the tint
     */
    public static void setTintList(@NonNull Drawable drawable, @Nullable ColorStateList tint) {
        IMPL.setTintList(drawable, tint);
    }

    /**
     * Specifies a tint blending mode for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tintMode A Porter-Duff blending mode
     */
    public static void setTintMode(@NonNull Drawable drawable, @Nullable PorterDuff.Mode tintMode) {
        IMPL.setTintMode(drawable, tintMode);
    }

    /**
     * Get the alpha value of the {@code drawable}.
     * 0 means fully transparent, 255 means fully opaque.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static int getAlpha(@NonNull Drawable drawable) {
        return IMPL.getAlpha(drawable);
    }

    /**
     * Applies the specified theme to this Drawable and its children.
     */
    public static void applyTheme(@NonNull Drawable drawable, @NonNull Resources.Theme t) {
        IMPL.applyTheme(drawable, t);
    }

    /**
     * Whether a theme can be applied to this Drawable and its children.
     */
    public static boolean canApplyTheme(@NonNull Drawable drawable) {
        return IMPL.canApplyTheme(drawable);
    }

    /**
     * Returns the current color filter, or {@code null} if none set.
     *
     * @return the current color filter, or {@code null} if none set
     */
    public static ColorFilter getColorFilter(@NonNull Drawable drawable) {
        return IMPL.getColorFilter(drawable);
    }

    /**
     * Removes the color filter from the given drawable.
     */
    public static void clearColorFilter(@NonNull Drawable drawable) {
        IMPL.clearColorFilter(drawable);
    }

    /**
     * Inflate this Drawable from an XML resource optionally styled by a theme.
     *
     * @param res Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this Drawable
     * @param attrs Base set of attribute values
     * @param theme Theme to apply, may be null
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static void inflate(@NonNull Drawable drawable, @NonNull Resources res,
            @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
            @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        IMPL.inflate(drawable, res, parser, attrs, theme);
    }

    /**
     * Potentially wrap {@code drawable} so that it may be used for tinting across the
     * different API levels, via the tinting methods in this class.
     *
     * <p>If the given drawable is wrapped, we will copy over certain state over to the wrapped
     * drawable, such as its bounds, level, visibility and state.</p>
     *
     * <p>You must use the result of this call. If the given drawable is being used by a view
     * (as its background for instance), you must replace the original drawable with
     * the result of this call:</p>
     *
     * <pre>
     * Drawable bg = DrawableCompat.wrap(view.getBackground());
     * // Need to set the background with the wrapped drawable
     * view.setBackground(bg);
     *
     * // You can now tint the drawable
     * DrawableCompat.setTint(bg, ...);
     * </pre>
     *
     * <p>If you need to get hold of the original {@link android.graphics.drawable.Drawable} again,
     * you can use the value returned from {@link #unwrap(Drawable)}.</p>
     *
     * @param drawable The Drawable to process
     * @return A drawable capable of being tinted across all API levels.
     *
     * @see #setTint(Drawable, int)
     * @see #setTintList(Drawable, ColorStateList)
     * @see #setTintMode(Drawable, PorterDuff.Mode)
     * @see #unwrap(Drawable)
     */
    public static Drawable wrap(@NonNull Drawable drawable) {
        return IMPL.wrap(drawable);
    }

    /**
     * Unwrap {@code drawable} if it is the result of a call to {@link #wrap(Drawable)}. If
     * the {@code drawable} is not the result of a call to {@link #wrap(Drawable)} then
     * {@code drawable} is returned as-is.
     *
     * @param drawable The drawable to unwrap
     * @return the unwrapped {@link Drawable} or {@code drawable} if it hasn't been wrapped.
     *
     * @see #wrap(Drawable)
     */
    public static <T extends Drawable> T unwrap(@NonNull Drawable drawable) {
        if (drawable instanceof DrawableWrapper) {
            return (T) ((DrawableWrapper) drawable).getWrappedDrawable();
        }
        return (T) drawable;
    }

    /**
     * Set the layout direction for this drawable. Should be a resolved
     * layout direction, as the Drawable has no capacity to do the resolution on
     * its own.
     *
     * @param layoutDirection the resolved layout direction for the drawable,
     *                        either {@link ViewCompat#LAYOUT_DIRECTION_LTR}
     *                        or {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @return {@code true} if the layout direction change has caused the
     *         appearance of the drawable to change such that it needs to be
     *         re-drawn, {@code false} otherwise
     * @see #getLayoutDirection(Drawable)
     */
    public static boolean setLayoutDirection(@NonNull Drawable drawable, int layoutDirection) {
        return IMPL.setLayoutDirection(drawable, layoutDirection);
    }

    /**
     * Returns the resolved layout direction for this Drawable.
     *
     * @return One of {@link ViewCompat#LAYOUT_DIRECTION_LTR},
     *         {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @see #setLayoutDirection(Drawable, int)
     */
    public static int getLayoutDirection(@NonNull Drawable drawable) {
        return IMPL.getLayoutDirection(drawable);
    }

    private DrawableCompat() {}
}
