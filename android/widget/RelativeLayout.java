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

package android.widget;

import android.annotation.NonNull;
import android.util.ArrayMap;
import com.android.internal.R;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pools.SynchronizedPool;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews.RemoteView;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

/**
* A Layout where the positions of the children can be described in relation to each other or to the
* parent.
* <p>
* <p>
* Note that you cannot have a circular dependency between the size of the RelativeLayout and the
* position of its children. For example, you cannot have a RelativeLayout whose height is set to
* {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT WRAP_CONTENT} and a child set to
* {@link #ALIGN_PARENT_BOTTOM}.
* </p>
* <p>
* <p><strong>Note:</strong> In platform version 17 and lower, RelativeLayout was affected by
* a measurement bug that could cause child views to be measured with incorrect
* {@link android.view.View.MeasureSpec MeasureSpec} values. (See
* {@link android.view.View.MeasureSpec#makeMeasureSpec(int, int) MeasureSpec.makeMeasureSpec}
* for more details.) This was triggered when a RelativeLayout container was placed in
* a scrolling container, such as a ScrollView or HorizontalScrollView. If a custom view
* not equipped to properly measure with the MeasureSpec mode
* {@link android.view.View.MeasureSpec#UNSPECIFIED UNSPECIFIED} was placed in a RelativeLayout,
* this would silently work anyway as RelativeLayout would pass a very large
* {@link android.view.View.MeasureSpec#AT_MOST AT_MOST} MeasureSpec instead.</p>
* <p>
* <p>This behavior has been preserved for apps that set <code>android:targetSdkVersion="17"</code>
* or older in their manifest's <code>uses-sdk</code> tag for compatibility. Apps targeting SDK
* version 18 or newer will receive the correct behavior</p>
* <p>
* <p>See the <a href="{@docRoot}guide/topics/ui/layout/relative.html">Relative
* Layout</a> guide.</p>
* <p>
* <p>
* Also see {@link android.widget.RelativeLayout.LayoutParams RelativeLayout.LayoutParams} for
* layout attributes
* </p>
*
* @attr ref android.R.styleable#RelativeLayout_gravity
* @attr ref android.R.styleable#RelativeLayout_ignoreGravity
*/
@RemoteView
public class RelativeLayout extends ViewGroup {
public static final int TRUE = -1;

/**
 * Rule that aligns a child's right edge with another child's left edge.
 */
public static final int LEFT_OF = 0;
/**
 * Rule that aligns a child's left edge with another child's right edge.
 */
public static final int RIGHT_OF = 1;
/**
 * Rule that aligns a child's bottom edge with another child's top edge.
 */
public static final int ABOVE = 2;
/**
 * Rule that aligns a child's top edge with another child's bottom edge.
 */
public static final int BELOW = 3;

/**
 * Rule that aligns a child's baseline with another child's baseline.
 */
public static final int ALIGN_BASELINE = 4;
/**
 * Rule that aligns a child's left edge with another child's left edge.
 */
public static final int ALIGN_LEFT = 5;
/**
 * Rule that aligns a child's top edge with another child's top edge.
 */
public static final int ALIGN_TOP = 6;
/**
 * Rule that aligns a child's right edge with another child's right edge.
 */
public static final int ALIGN_RIGHT = 7;
/**
 * Rule that aligns a child's bottom edge with another child's bottom edge.
 */
public static final int ALIGN_BOTTOM = 8;

/**
 * Rule that aligns the child's left edge with its RelativeLayout
 * parent's left edge.
 */
public static final int ALIGN_PARENT_LEFT = 9;
/**
 * Rule that aligns the child's top edge with its RelativeLayout
 * parent's top edge.
 */
public static final int ALIGN_PARENT_TOP = 10;
/**
 * Rule that aligns the child's right edge with its RelativeLayout
 * parent's right edge.
 */
public static final int ALIGN_PARENT_RIGHT = 11;
/**
 * Rule that aligns the child's bottom edge with its RelativeLayout
 * parent's bottom edge.
 */
public static final int ALIGN_PARENT_BOTTOM = 12;

/**
 * Rule that centers the child with respect to the bounds of its
 * RelativeLayout parent.
 */
public static final int CENTER_IN_PARENT = 13;
/**
 * Rule that centers the child horizontally with respect to the
 * bounds of its RelativeLayout parent.
 */
public static final int CENTER_HORIZONTAL = 14;
/**
 * Rule that centers the child vertically with respect to the
 * bounds of its RelativeLayout parent.
 */
public static final int CENTER_VERTICAL = 15;
/**
 * Rule that aligns a child's end edge with another child's start edge.
 */
public static final int START_OF = 16;
/**
 * Rule that aligns a child's start edge with another child's end edge.
 */
public static final int END_OF = 17;
/**
 * Rule that aligns a child's start edge with another child's start edge.
 */
public static final int ALIGN_START = 18;
/**
 * Rule that aligns a child's end edge with another child's end edge.
 */
public static final int ALIGN_END = 19;
/**
 * Rule that aligns the child's start edge with its RelativeLayout
 * parent's start edge.
 */
public static final int ALIGN_PARENT_START = 20;
/**
 * Rule that aligns the child's end edge with its RelativeLayout
 * parent's end edge.
 */
public static final int ALIGN_PARENT_END = 21;

private static final int VERB_COUNT = 22;


//垂直方向的依赖关系
private static final int[] RULES_VERTICAL = {
        ABOVE, BELOW, ALIGN_BASELINE, ALIGN_TOP, ALIGN_BOTTOM
};

//水平方向的依赖关系
private static final int[] RULES_HORIZONTAL = {
        LEFT_OF, RIGHT_OF, ALIGN_LEFT, ALIGN_RIGHT, START_OF, END_OF, ALIGN_START, ALIGN_END
};

/**
 * Used to indicate left/right/top/bottom should be inferred from constraints
 */
private static final int VALUE_NOT_SET = Integer.MIN_VALUE;

//基准线
private View mBaselineView = null;

private int mGravity = Gravity.START | Gravity.TOP;
private final Rect mContentBounds = new Rect();
private final Rect mSelfBounds = new Rect();
private int mIgnoreGravity;

private SortedSet<View> mTopToBottomLeftToRightSet = null;

//这个值只有在RequestLayout中被更新,每次调用requestLayout都会被赋值为true
private boolean mDirtyHierarchy;
//存储水平关系排序后的子View
private View[] mSortedHorizontalChildren;
//存储垂直关系排序后的子View
private View[] mSortedVerticalChildren;
//依赖图
private final DependencyGraph mGraph = new DependencyGraph();

// Compatibility hack. Old versions of the platform had problems
// with MeasureSpec value overflow and RelativeLayout was one source of them.
// Some apps came to rely on them. :(

//兼容性处理。 在旧版本的平台上会有MeasureSpec值溢出的问题,而RelativeLayout则是来源之一
//部分程序要依它们

private boolean mAllowBrokenMeasureSpecs = false;

// Compatibility hack. Old versions of the platform would not take
// margins and padding into account when generating the height measure spec
// for children during the horizontal measure pass.

//兼容性处理。 在旧版本的平台上有在水平测量期间计算子view的高度时不会考虑margins和padding

private boolean mMeasureVerticalWithPaddingMargin = false;

// A default width used for RTL measure pass
/**
 * Value reduced so as not to interfere with View's measurement spec. flags. See:
 * {@link View#MEASURED_SIZE_MASK}.
 * {@link View#MEASURED_STATE_TOO_SMALL}.
 **/
private static final int DEFAULT_WIDTH = 0x00010000;

public RelativeLayout(Context context) {
    this(context, null);
}

public RelativeLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
}

public RelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
}

public RelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    queryCompatibilityModes(context);
}

/**
 * 初始化属性
 *
 * @param context
 * @param attrs
 * @param defStyleAttr
 * @param defStyleRes
 */
private void initFromAttributes(
        Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    final TypedArray a = context.obtainStyledAttributes(
            attrs, R.styleable.RelativeLayout, defStyleAttr, defStyleRes);
    mIgnoreGravity = a.getResourceId(R.styleable.RelativeLayout_ignoreGravity, View.NO_ID);
    mGravity = a.getInt(R.styleable.RelativeLayout_gravity, mGravity);
    a.recycle();
}

/**
 * 查询是否需要做兼容性处理
 * 当android版本小于4.3时 mAllowBrokenMeasureSpecs == true
 * 当android版本大于等于4.3时 mMeasureVerticalWithPaddingMargin == true
 *
 * @param context
 */
private void queryCompatibilityModes(Context context) {
    int version = context.getApplicationInfo().targetSdkVersion;
    mAllowBrokenMeasureSpecs = version <= Build.VERSION_CODES.JELLY_BEAN_MR1;
    mMeasureVerticalWithPaddingMargin = version >= Build.VERSION_CODES.JELLY_BEAN_MR2;
}

@Override
public boolean shouldDelayChildPressedState() {
    return false;
}

/**
 * Defines which View is ignored when the gravity is applied. This setting has no
 * effect if the gravity is <code>Gravity.START | Gravity.TOP</code>.
 * 定义当gravity被应用时哪个视图被忽略。 这个设置在gravity的值为Gravity.START | Gravity.TOP时无效
 *
 * @param viewId The id of the View to be ignored by gravity, or 0 if no View
 *               should be ignored.
 * @attr ref android.R.styleable#RelativeLayout_ignoreGravity
 * @see #setGravity(int)
 */
@android.view.RemotableViewMethod
public void setIgnoreGravity(int viewId) {
    mIgnoreGravity = viewId;
}

/**
 * Describes how the child views are positioned.
 * 描述子View的位置
 *
 * @return the gravity.
 * @attr ref android.R.styleable#RelativeLayout_gravity
 * @see #setGravity(int)
 * @see android.view.Gravity
 */
public int getGravity() {
    return mGravity;
}

/**
 * Describes how the child views are positioned. Defaults to
 * <code>Gravity.START | Gravity.TOP</code>.
 * <p>
 * <p>Note that since RelativeLayout considers the positioning of each child
 * relative to one another to be significant, setting gravity will affect
 * the positioning of all children as a single unit within the parent.
 * This happens after children have been relatively positioned.</p>
 *
 * @param gravity See {@link android.view.Gravity}
 * @attr ref android.R.styleable#RelativeLayout_gravity
 * @see #setHorizontalGravity(int)
 * @see #setVerticalGravity(int)
 */
@android.view.RemotableViewMethod
public void setGravity(int gravity) {
    if (mGravity != gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.START;
        }

        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.TOP;
        }

        mGravity = gravity;
        requestLayout();
    }
}

@android.view.RemotableViewMethod
public void setHorizontalGravity(int horizontalGravity) {
    final int gravity = horizontalGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
    if ((mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) != gravity) {
        mGravity = (mGravity & ~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) | gravity;
        requestLayout();
    }
}

@android.view.RemotableViewMethod
public void setVerticalGravity(int verticalGravity) {
    final int gravity = verticalGravity & Gravity.VERTICAL_GRAVITY_MASK;
    if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != gravity) {
        mGravity = (mGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | gravity;
        requestLayout();
    }
}

/**
 * 获取基准线
 * 如果当前的基准线不为null则直接返回,否则返回ViewGroup的基准线(value == -1)
 *
 * @return
 */
@Override
public int getBaseline() {
    return mBaselineView != null ? mBaselineView.getBaseline() : super.getBaseline();
}

/**
 * 每次调用requestLayout都会被赋值为true
 */
@Override
public void requestLayout() {
    super.requestLayout();
    mDirtyHierarchy = true;
}

/**
 * 排序子view
 */
private void sortChildren() {
    final int count = getChildCount();
    if (mSortedVerticalChildren == null || mSortedVerticalChildren.length != count) {
        mSortedVerticalChildren = new View[count];
    }

    if (mSortedHorizontalChildren == null || mSortedHorizontalChildren.length != count) {
        mSortedHorizontalChildren = new View[count];
    }

    final DependencyGraph graph = mGraph;
    graph.clear();

    for (int i = 0; i < count; i++) {
        graph.add(getChildAt(i));
    }

    //根据垂直方向的规则去排序垂直方向的子view
    graph.getSortedViews(mSortedVerticalChildren, RULES_VERTICAL);
    //根据水平方向的规则去排序水平方向的子view
    graph.getSortedViews(mSortedHorizontalChildren, RULES_HORIZONTAL);
}

@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    //先把子View根据纵向关系和横向关系排序
    if (mDirtyHierarchy) {
        mDirtyHierarchy = false;
        sortChildren();
    }

    int myWidth = -1;
    int myHeight = -1;

    int width = 0;
    int height = 0;

    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    // Record our dimensions if they are known;

    // 根据MeasureSpec去确定尺寸

    if (widthMode != MeasureSpec.UNSPECIFIED) {
        myWidth = widthSize;
    }

    if (heightMode != MeasureSpec.UNSPECIFIED) {
        myHeight = heightSize;
    }

    if (widthMode == MeasureSpec.EXACTLY) {
        width = myWidth;
    }

    if (heightMode == MeasureSpec.EXACTLY) {
        height = myHeight;
    }

    View ignore = null;
    //判断是否是Gravity.START和Gravity.TOP
    //目的是确定左上角的坐标
    int gravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
    final boolean horizontalGravity = gravity != Gravity.START && gravity != 0;
    gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
    final boolean verticalGravity = gravity != Gravity.TOP && gravity != 0;

    int left = Integer.MAX_VALUE;
    int top = Integer.MAX_VALUE;
    int right = Integer.MIN_VALUE;
    int bottom = Integer.MIN_VALUE;

    boolean offsetHorizontalAxis = false;
    boolean offsetVerticalAxis = false;

    //记录ignore的view
    if ((horizontalGravity || verticalGravity) && mIgnoreGravity != View.NO_ID) {
        ignore = findViewById(mIgnoreGravity);
    }

    //宽度和高度是否是wrap模式
    final boolean isWrapContentWidth = widthMode != MeasureSpec.EXACTLY;
    final boolean isWrapContentHeight = heightMode != MeasureSpec.EXACTLY;

    // We need to know our size for doing the correct computation of children positioning in RTL
    // mode but there is no practical way to get it instead of running the code below.
    // So, instead of running the code twice, we just set the width to a "default display width"
    // before the computation and then, as a last pass, we will update their real position with
    // an offset equals to "DEFAULT_WIDTH - width".

    // 我们需要知道我们的大小是为了正确计算在RTL模式下的孩子定位，但没有实际的方法来获取它，而不是运行下面的代码。
    // 因此，我们只是在计算之前将宽度设置为“默认显示宽度”，而不是运行代码，而是作为最后一次通过，
    // 将以“DEFAULT_WIDTH - width”的偏移量更新其实际位置。

    final int layoutDirection = getLayoutDirection();
    if (isLayoutRtl() && myWidth == -1) {
        myWidth = DEFAULT_WIDTH;
    }

    //水平子View的集合
    View[] views = mSortedHorizontalChildren;
    int count = views.length;

    for (int i = 0; i < count; i++) {
        View child = views[i];
        if (child.getVisibility() != GONE) {
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            //根据方向获得子view中设置的规则
            int[] rules = params.getRules(layoutDirection);
            //根据这些左右方向的规则转化成左右坐标
            applyHorizontalSizeRules(params, myWidth, rules);
            //测量水平方向子view的尺寸
            measureChildHorizontal(child, params, myWidth, myHeight);
            //确定水平方向子view位置
            if (positionChildHorizontal(child, params, myWidth, isWrapContentWidth)) {
                offsetHorizontalAxis = true;
            }
        }
    }

    //垂直子View的集合
    views = mSortedVerticalChildren;
    count = views.length;
    final int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;

    for (int i = 0; i < count; i++) {
        final View child = views[i];
        if (child.getVisibility() != GONE) {
            final LayoutParams params = (LayoutParams) child.getLayoutParams();
            //把垂直方向的关系转换成边界
            applyVerticalSizeRules(params, myHeight, child.getBaseline());
            //测量垂直方向的子View 与measureChildHorizontal不同
            measureChild(child, params, myWidth, myHeight);
            //确定垂直方向的子View
            if (positionChildVertical(child, params, myHeight, isWrapContentHeight)) {
                offsetVerticalAxis = true;
            }

            //当width为wrapContent时做特殊处理
            if (isWrapContentWidth) {
                //判断布局是RTL还是LTR模式
                if (isLayoutRtl()) {
                    //RTL 根据前面的兼容性处理 需要对4.3以后的版本进行margin处理
                    if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                        width = Math.max(width, myWidth - params.mLeft);
                    } else {
                        width = Math.max(width, myWidth - params.mLeft - params.leftMargin);
                    }
                } else {
                    //LTR
                    if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                        width = Math.max(width, params.mRight);
                    } else {
                        width = Math.max(width, params.mRight + params.rightMargin);
                    }
                }
            }

            //当height为wrapContent时做特殊处理
            if (isWrapContentHeight) {
                //根据前面的兼容性处理 需要对4.3以后的版本进行margin处理
                if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                    height = Math.max(height, params.mBottom);
                } else {
                    height = Math.max(height, params.mBottom + params.bottomMargin);
                }
            }

            //左上边界值计算
            if (child != ignore || verticalGravity) {
                left = Math.min(left, params.mLeft - params.leftMargin);
                top = Math.min(top, params.mTop - params.topMargin);
            }

            //右下边界值计算
            if (child != ignore || horizontalGravity) {
                right = Math.max(right, params.mRight + params.rightMargin);
                bottom = Math.max(bottom, params.mBottom + params.bottomMargin);
            }
        }
    }

    // Use the top-start-most laid out view as the baseline. RTL offsets are
    // applied later, so we can use the left-most edge as the starting edge.

    // baseline计算
    View baselineView = null;
    LayoutParams baselineParams = null;
    for (int i = 0; i < count; i++) {
        final View child = views[i];
        if (child.getVisibility() != GONE) {
            final LayoutParams childParams = (LayoutParams) child.getLayoutParams();
            if (baselineView == null || baselineParams == null
                    || compareLayoutPosition(childParams, baselineParams) < 0) {
                baselineView = child;
                baselineParams = childParams;
            }
        }
    }
    mBaselineView = baselineView;

    //如果width是wrap模式
    if (isWrapContentWidth) {
        // Width already has left padding in it since it was calculated by looking at
        // the right of each child view
        // 宽度已经根据每个子View的右边进行了填充
        width += mPaddingRight;

        if (mLayoutParams != null && mLayoutParams.width >= 0) {
            width = Math.max(width, mLayoutParams.width);
        }

        width = Math.max(width, getSuggestedMinimumWidth());
        width = resolveSize(width, widthMeasureSpec);

        //在得到最终width之后,就对依赖RelativeLayout的子view加上偏移量
        if (offsetHorizontalAxis) {
            for (int i = 0; i < count; i++) {
                final View child = views[i];
                //视图可见
                if (child.getVisibility() != GONE) {
                    //获取布局参数
                    final LayoutParams params = (LayoutParams) child.getLayoutParams();
                    //获取规则
                    final int[] rules = params.getRules(layoutDirection);
                    //如果设置了CENTER_IN_PARENT或者CENTER_HORIZONTAL属性
                    if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
                        centerHorizontal(child, params, width);
                    } else if (rules[ALIGN_PARENT_RIGHT] != 0) {
                        //如果设置了ALIGN_PARENT_RIGHT属性
                        final int childWidth = child.getMeasuredWidth();
                        params.mLeft = width - mPaddingRight - childWidth;
                        params.mRight = params.mLeft + childWidth;
                    }
                }
            }
        }
    }

    //如果height是wrap模式 同上
    if (isWrapContentHeight) {
        // Height already has top padding in it since it was calculated by looking at
        // the bottom of each child view
        height += mPaddingBottom;

        if (mLayoutParams != null && mLayoutParams.height >= 0) {
            height = Math.max(height, mLayoutParams.height);
        }

        height = Math.max(height, getSuggestedMinimumHeight());
        height = resolveSize(height, heightMeasureSpec);

        if (offsetVerticalAxis) {
            for (int i = 0; i < count; i++) {
                final View child = views[i];
                if (child.getVisibility() != GONE) {
                    final LayoutParams params = (LayoutParams) child.getLayoutParams();
                    final int[] rules = params.getRules(layoutDirection);
                    if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_VERTICAL] != 0) {
                        centerVertical(child, params, height);
                    } else if (rules[ALIGN_PARENT_BOTTOM] != 0) {
                        final int childHeight = child.getMeasuredHeight();
                        params.mTop = height - mPaddingBottom - childHeight;
                        params.mBottom = params.mTop + childHeight;
                    }
                }
            }
        }
    }

    //根据水平和垂直方向的gravity进行布局参数修正
    if (horizontalGravity || verticalGravity) {
        final Rect selfBounds = mSelfBounds;
        selfBounds.set(mPaddingLeft, mPaddingTop, width - mPaddingRight,
                height - mPaddingBottom);

        final Rect contentBounds = mContentBounds;
        Gravity.apply(mGravity, right - left, bottom - top, selfBounds, contentBounds,
                layoutDirection);

        final int horizontalOffset = contentBounds.left - left;
        final int verticalOffset = contentBounds.top - top;
        if (horizontalOffset != 0 || verticalOffset != 0) {
            for (int i = 0; i < count; i++) {
                final View child = views[i];
                if (child.getVisibility() != GONE && child != ignore) {
                    final LayoutParams params = (LayoutParams) child.getLayoutParams();
                    if (horizontalGravity) {
                        params.mLeft += horizontalOffset;
                        params.mRight += horizontalOffset;
                    }
                    if (verticalGravity) {
                        params.mTop += verticalOffset;
                        params.mBottom += verticalOffset;
                    }
                }
            }
        }
    }

    //RTL模式下对布局参数进行修正
    if (isLayoutRtl()) {
        final int offsetWidth = myWidth - width;
        for (int i = 0; i < count; i++) {
            final View child = views[i];
            if (child.getVisibility() != GONE) {
                final LayoutParams params = (LayoutParams) child.getLayoutParams();
                params.mLeft -= offsetWidth;
                params.mRight -= offsetWidth;
            }
        }
    }

    setMeasuredDimension(width, height);
}

/**
 * @return a negative number if the top of {@code p1} is above the top of
 * {@code p2} or if they have identical top values and the left of
 * {@code p1} is to the left of {@code p2}, or a positive number
 * otherwise
 */
private int compareLayoutPosition(LayoutParams p1, LayoutParams p2) {
    final int topDiff = p1.mTop - p2.mTop;
    if (topDiff != 0) {
        return topDiff;
    }
    return p1.mLeft - p2.mLeft;
}

/**
 * Measure a child. The child should have left, top, right and bottom information
 * stored in its LayoutParams. If any of these values is VALUE_NOT_SET it means
 * that the view can extend up to the corresponding edge.
 *
 * @param child    Child to measure 需要测量的子view
 * @param params   LayoutParams associated with child 与子view关联的布局参数
 * @param myWidth  Width of the the RelativeLayout 父布局的width
 * @param myHeight Height of the RelativeLayout 父布局的height
 */
private void measureChild(View child, LayoutParams params, int myWidth, int myHeight) {
    int childWidthMeasureSpec = getChildMeasureSpec(params.mLeft,
            params.mRight, params.width,
            params.leftMargin, params.rightMargin,
            mPaddingLeft, mPaddingRight,
            myWidth);
    int childHeightMeasureSpec = getChildMeasureSpec(params.mTop,
            params.mBottom, params.height,
            params.topMargin, params.bottomMargin,
            mPaddingTop, mPaddingBottom,
            myHeight);
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
}

private void measureChildHorizontal(
        View child, LayoutParams params, int myWidth, int myHeight) {
    //获得子View的宽度MeasureSpec
    final int childWidthMeasureSpec = getChildMeasureSpec(params.mLeft, params.mRight,
            params.width, params.leftMargin, params.rightMargin, mPaddingLeft, mPaddingRight,
            myWidth);

    final int childHeightMeasureSpec;

    //android版本为4.2及其以下时mAllowBrokenMeasureSpecs == true
    if (myHeight < 0 && !mAllowBrokenMeasureSpecs) {
        if (params.height >= 0) {
            //高度确定,直接以MeasureSpec.EXACTLY测量
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    params.height, MeasureSpec.EXACTLY);
        } else {
            // Negative values in a mySize/myWidth/myWidth value in
            // RelativeLayout measurement is code for, "we got an
            // unspecified mode in the RelativeLayout's measure spec."
            // Carry it forward.

            //高度不确定
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
    } else {
        final int maxHeight;
        //在旧版本(4.2及其以下)的平台上有在水平测量期间计算子view的高度时不会考虑margins和padding
        if (mMeasureVerticalWithPaddingMargin) {
            maxHeight = Math.max(0, myHeight - mPaddingTop - mPaddingBottom
                    - params.topMargin - params.bottomMargin);
        } else {
            maxHeight = Math.max(0, myHeight);
        }

        final int heightMode;
        //如果子View的宽度是精确模式(MATCH_PARENT或者dimens),那么它的高度也是精确模式
        if (params.height == LayoutParams.MATCH_PARENT) {
            heightMode = MeasureSpec.EXACTLY;
        } else {
            //如果子View的宽度是AT_MOST模式,那么它的高度的也是AT_MOST模式
            heightMode = MeasureSpec.AT_MOST;
        }
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, heightMode);
    }
    //当宽和高都确定以后就可以开始测量了
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
}

/**
 * Get a measure spec that accounts for all of the constraints on this view.
 * This includes size constraints imposed by the RelativeLayout as well as
 * the View's desired dimension.
 *
 * @param childStart   The left or top field of the child's layout params
 *                     子View布局的左侧或者顶部字段
 * @param childEnd     The right or bottom field of the child's layout params
 *                     子View布局的右侧或底部字段
 * @param childSize    The child's desired size (the width or height field of
 *                     the child's layout params)
 * @param startMargin  The left or top margin
 *                     左侧或者顶部的Margin值
 * @param endMargin    The right or bottom margin
 *                     右侧或者底部的Margin值
 * @param startPadding mPaddingLeft or mPaddingTop
 *                     左侧或者顶部的Padding值
 * @param endPadding   mPaddingRight or mPaddingBottom
 *                     右侧或者底部的Padding值
 * @param mySize       The width or height of this view (the RelativeLayout)
 *                     父布局的width或者height值
 * @return MeasureSpec for the child
 */
private int getChildMeasureSpec(int childStart, int childEnd,
                                int childSize, int startMargin, int endMargin, int startPadding,
                                int endPadding, int mySize) {
    int childSpecMode = 0;
    int childSpecSize = 0;

    // Negative values in a mySize value in RelativeLayout
    // measurement is code for, "we got an unspecified mode in the
    // RelativeLayout's measure spec."
    // RelativeLayout测量中mySize值为负值是因为“在RelativeLayout测量规范中我们得到了未指定的模式”。
    final boolean isUnspecified = mySize < 0;
    // 如果父容器的宽度的小于0并且系统版本大于4.3
    if (isUnspecified && !mAllowBrokenMeasureSpecs) {
        //子View的左右边间距都不等于VALUE_NOT_SET
        if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
            // Constraints fixed both edges, so child has an exact size.
            // 边界确定的话 子View的size也是个精确值
            childSpecSize = Math.max(0, childEnd - childStart);
            childSpecMode = MeasureSpec.EXACTLY;
        } else if (childSize >= 0) {
            // 如果不满足第一个条件但是childSize>=0 那么子View也是精确值
            // The child specified an exact size.
            childSpecSize = childSize;
            childSpecMode = MeasureSpec.EXACTLY;
        } else {
            // 都不满足的话就是不确定值
            // Allow the child to be whatever size it wants.
            childSpecSize = 0;
            childSpecMode = MeasureSpec.UNSPECIFIED;
        }

        return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
    }

    //mySize >= 0 的情况

    // Figure out start and end bounds.
    int tempStart = childStart;
    int tempEnd = childEnd;

    // If the view did not express a layout constraint for an edge, use
    // view's margins and our padding
    // 如果没有指定边界值,设置一个默认值
    if (tempStart == VALUE_NOT_SET) {
        tempStart = startPadding + startMargin;
    }
    if (tempEnd == VALUE_NOT_SET) {
        tempEnd = mySize - endPadding - endMargin;
    }

    // Figure out maximum size available to this view
    // 指明最大可用空间
    final int maxAvailable = tempEnd - tempStart;


    //当左右边界都是确定值时 基本上已经可用确定为精确模式和大小了
    //特殊情况是isUnspecified = true && mAllowBrokenMeasureSpecs = true
    if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
        // Constraints fixed both edges, so child must be an exact size.
        childSpecMode = isUnspecified ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
        childSpecSize = Math.max(0, maxAvailable);
    } else {

        if (childSize >= 0) {
            //确定大小的情况
            // Child wanted an exact size. Give as much as possible.
            childSpecMode = MeasureSpec.EXACTLY;

            if (maxAvailable >= 0) {
                // We have a maximum size in this dimension.
                childSpecSize = Math.min(maxAvailable, childSize);
            } else {
                // We can grow in this dimension.
                childSpecSize = childSize;
            }
        } else if (childSize == LayoutParams.MATCH_PARENT) {
            // MATCH_PARENT情况
            // Child wanted to be as big as possible. Give all available
            // space.
            childSpecMode = isUnspecified ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
            childSpecSize = Math.max(0, maxAvailable);
        } else if (childSize == LayoutParams.WRAP_CONTENT) {
            // WRAP_CONTENT情况
            // Child wants to wrap content. Use AT_MOST to communicate
            // available space if we know our max size.
            if (maxAvailable >= 0) {
                // We have a maximum size in this dimension.
                childSpecMode = MeasureSpec.AT_MOST;
                childSpecSize = maxAvailable;
            } else {
                // We can grow in this dimension. Child can be as big as it
                // wants.
                childSpecMode = MeasureSpec.UNSPECIFIED;
                childSpecSize = 0;
            }
        }
    }

    return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
}

/**
 * 确定水平方向的子View的位置
 *
 * @param child
 * @param params
 * @param myWidth
 * @param wrapContent
 * @return
 */
private boolean positionChildHorizontal(View child, LayoutParams params, int myWidth,
                                        boolean wrapContent) {
    //获取布局方向
    //RTL(从右向左)
    //LTR(默认情况:从左向右)
    final int layoutDirection = getLayoutDirection();
    //获取规则
    int[] rules = params.getRules(layoutDirection);

    if (params.mLeft == VALUE_NOT_SET && params.mRight != VALUE_NOT_SET) {
        // Right is fixed, but left varies
        // 左边界为无效值,右边界确定
        params.mLeft = params.mRight - child.getMeasuredWidth();
    } else if (params.mLeft != VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
        // Left is fixed, but right varies
        // 左边界确定,右边界为无效值
        params.mRight = params.mLeft + child.getMeasuredWidth();
    } else if (params.mLeft == VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
        // Both left and right vary
        // 左右边界均为无效值
        // 如果设置了CENTER_IN_PARENT或者CENTER_HORIZONTAL
        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
            // 如果不是wrap模式(一般就是match/dimens)
            if (!wrapContent) {
                //水平居中
                centerHorizontal(child, params, myWidth);
            } else {
                //wrap模式
                params.mLeft = mPaddingLeft + params.leftMargin;
                params.mRight = params.mLeft + child.getMeasuredWidth();
            }
            return true;
        } else {
            // This is the default case. For RTL we start from the right and for LTR we start
            // from the left. This will give LEFT/TOP for LTR and RIGHT/TOP for RTL.
            // RTL模式
            if (isLayoutRtl()) {
                params.mRight = myWidth - mPaddingRight - params.rightMargin;
                params.mLeft = params.mRight - child.getMeasuredWidth();
            } else {
                // TRL模式
                params.mLeft = mPaddingLeft + params.leftMargin;
                params.mRight = params.mLeft + child.getMeasuredWidth();
            }
        }
    }
    return rules[ALIGN_PARENT_END] != 0;
}

/**
 * 确定垂直方向子View的位置
 * @param child
 * @param params
 * @param myHeight
 * @param wrapContent 是否是wrapContent模式
 * @return
 */
private boolean positionChildVertical(View child, LayoutParams params, int myHeight,
                                      boolean wrapContent) {
    //取得规则
    int[] rules = params.getRules();

    if (params.mTop == VALUE_NOT_SET && params.mBottom != VALUE_NOT_SET) {
        // Bottom is fixed, but top varies
        // 底部边界固定,顶部边界为无效值
        // 顶部的边界为底部的值减去子View的高度
        params.mTop = params.mBottom - child.getMeasuredHeight();
    } else if (params.mTop != VALUE_NOT_SET && params.mBottom == VALUE_NOT_SET) {
        // Top is fixed, but bottom varies
        // 顶部边界固定,底部边界无效值
        // 底部的边界为布局的顶部边界加上字View的高度
        params.mBottom = params.mTop + child.getMeasuredHeight();
    } else if (params.mTop == VALUE_NOT_SET && params.mBottom == VALUE_NOT_SET) {
        // Both top and bottom vary
        // 上下边界均为无效值
        // 如果设置了CENTER_IN_PARENT或者CENTER_VERTICAL属性
        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_VERTICAL] != 0) {
            //是否是wrap模式
            if (!wrapContent) {

                //非wrap模式 垂直居中布局
                centerVertical(child, params, myHeight);
            } else {
                //wrap模式 计算上下边界
                params.mTop = mPaddingTop + params.topMargin;
                params.mBottom = params.mTop + child.getMeasuredHeight();
            }
            return true;
        } else {
            //未设置CENTER_IN_PARENT或者CENTER_VERTICAL属性 计算上下边界
            params.mTop = mPaddingTop + params.topMargin;
            params.mBottom = params.mTop + child.getMeasuredHeight();
        }
    }
    return rules[ALIGN_PARENT_BOTTOM] != 0;
}

private void applyHorizontalSizeRules(LayoutParams childParams, int myWidth, int[] rules) {
    RelativeLayout.LayoutParams anchorParams;

    // VALUE_NOT_SET indicates a "soft requirement" in that direction. For example:
    // left=10, right=VALUE_NOT_SET means the view must start at 10, but can go as far as it
    // wants to the right
    // left=VALUE_NOT_SET, right=10 means the view must end at 10, but can go as far as it
    // wants to the left
    // left=10, right=20 means the left and right ends are both fixed

    // VALUE_NOT_SET 表示的是在方向上的"软需求"
    //例如: left = 10, right = VALUE_NOT_SET 意味着这个view必须在10的位置的开始,但是可以向右移动
    //     left=VALUE_NOT_SET, right=10 意味着这个view必须在10的位置结束,但是可以向左右移动
    //     left=10, right=20 意味着这个view的左右已经被固定

    childParams.mLeft = VALUE_NOT_SET;
    childParams.mRight = VALUE_NOT_SET;

    //取得的当前子View的LEFT_OF属性对应的View
    anchorParams = getRelatedViewParams(rules, LEFT_OF);
    if (anchorParams != null) {
        //如果设置了这个属性,那么当前子View的右坐标就是layout_toLeftOf属性对应的View的左边坐标减去对应View的左右Margin值
        childParams.mRight = anchorParams.mLeft - (anchorParams.leftMargin +
                childParams.rightMargin);
    } else if (childParams.alignWithParent && rules[LEFT_OF] != 0) {
        //如果alignWithParent == true 并且也 LEFT_OF属性存在
        //alignWithParent的值对应的是alignWithParentIfMissing
        //如果LEFT_OF对应的View是null或者gone alignWithParentIfMissing这个值就会起效
        //它会把RelativeLayout当做被依赖的对象
        if (myWidth >= 0) {
            //如果父容器的宽度大于等于0
            //子View的右边界就是父容器的宽度减去paddingRight值和子View的MarginRight值
            childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
        }
    }

    //取得的当前子View的RIGHT_OF属性对应的View 处理逻辑同LEFT_OF相似
    anchorParams = getRelatedViewParams(rules, RIGHT_OF);
    if (anchorParams != null) {
        childParams.mLeft = anchorParams.mRight + (anchorParams.rightMargin +
                childParams.leftMargin);
    } else if (childParams.alignWithParent && rules[RIGHT_OF] != 0) {
        childParams.mLeft = mPaddingLeft + childParams.leftMargin;
    }

    //取得的当前子View的ALIGN_LEFT属性对应的View 处理逻辑同LEFT_OF相似
    anchorParams = getRelatedViewParams(rules, ALIGN_LEFT);
    if (anchorParams != null) {
        childParams.mLeft = anchorParams.mLeft + childParams.leftMargin;
    } else if (childParams.alignWithParent && rules[ALIGN_LEFT] != 0) {
        childParams.mLeft = mPaddingLeft + childParams.leftMargin;
    }

    //取得的当前子View的ALIGN_RIGHT属性对应的View 处理逻辑同LEFT_OF相似
    anchorParams = getRelatedViewParams(rules, ALIGN_RIGHT);
    if (anchorParams != null) {
        childParams.mRight = anchorParams.mRight - childParams.rightMargin;
    } else if (childParams.alignWithParent && rules[ALIGN_RIGHT] != 0) {
        if (myWidth >= 0) {
            childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
        }
    }

    //对属性android:alignParentLeft进行处理
    if (0 != rules[ALIGN_PARENT_LEFT]) {
        //当前子View的left值为PaddingLeft和其自身的MarginLeft值相加
        childParams.mLeft = mPaddingLeft + childParams.leftMargin;
    }

    //对属性android:alignParentRighth进行处理
    if (0 != rules[ALIGN_PARENT_RIGHT]) {
        //父容器宽度大于0
        if (myWidth >= 0) {
            //当前子View的right值为父容器宽度减去PaddingRight和自身的MarginLeft
            childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
        }
    }
}

private void applyVerticalSizeRules(LayoutParams childParams, int myHeight, int myBaseline) {
    final int[] rules = childParams.getRules();

    // Baseline alignment overrides any explicitly specified top or bottom.
    // 基准线对齐覆盖任何指定的顶部或者底部
    int baselineOffset = getRelatedViewBaselineOffset(rules);
    if (baselineOffset != -1) {
        if (myBaseline != -1) {
            baselineOffset -= myBaseline;
        }
        childParams.mTop = baselineOffset;
        childParams.mBottom = VALUE_NOT_SET;
        return;
    }

    // 基准线的偏移量 == -1的情况
    RelativeLayout.LayoutParams anchorParams;

    //默认值
    childParams.mTop = VALUE_NOT_SET;
    childParams.mBottom = VALUE_NOT_SET;

    //ABOVE属性对应的View的布局参数
    anchorParams = getRelatedViewParams(rules, ABOVE);
    if (anchorParams != null) {
        //当前子View的布局底部边界为ABOVE对应的顶部减去上下margin之和
        childParams.mBottom = anchorParams.mTop - (anchorParams.topMargin +
                childParams.bottomMargin);
    } else if (childParams.alignWithParent && rules[ABOVE] != 0) {
        //如果alignWithParent == true 并且也 ABOVE属性存在
        //alignWithParent的值对应的是alignWithParentIfMissing
        //如果ABOVE对应的View是null或者gone alignWithParentIfMissing这个值就会起效
        //它会把RelativeLayout当做被依赖的对象
        if (myHeight >= 0) {
            //如果父容器的宽度大于等于0
            //子View的右边界就是父容器的宽度减去paddingRight值和子View的MarginBottom值
            childParams.mBottom = myHeight - mPaddingBottom - childParams.bottomMargin;
        }
    }

    //BELOW属性对应的View的布局参数 处理逻辑同ABOVE相似
    anchorParams = getRelatedViewParams(rules, BELOW);
    if (anchorParams != null) {
        childParams.mTop = anchorParams.mBottom + (anchorParams.bottomMargin +
                childParams.topMargin);
    } else if (childParams.alignWithParent && rules[BELOW] != 0) {
        childParams.mTop = mPaddingTop + childParams.topMargin;
    }

    //ALIGN_TOP属性对应的View的布局参数 处理逻辑同ABOVE相似
    anchorParams = getRelatedViewParams(rules, ALIGN_TOP);
    if (anchorParams != null) {
        childParams.mTop = anchorParams.mTop + childParams.topMargin;
    } else if (childParams.alignWithParent && rules[ALIGN_TOP] != 0) {
        childParams.mTop = mPaddingTop + childParams.topMargin;
    }

    //ALIGN_BOTTOM属性对应的View的布局参数 处理逻辑同ABOVE相似
    anchorParams = getRelatedViewParams(rules, ALIGN_BOTTOM);
    if (anchorParams != null) {
        childParams.mBottom = anchorParams.mBottom - childParams.bottomMargin;
    } else if (childParams.alignWithParent && rules[ALIGN_BOTTOM] != 0) {
        if (myHeight >= 0) {
            childParams.mBottom = myHeight - mPaddingBottom - childParams.bottomMargin;
        }
    }

    //alignParentTop属性
    if (0 != rules[ALIGN_PARENT_TOP]) {
        childParams.mTop = mPaddingTop + childParams.topMargin;
    }

    //alignParentBottom属性
    if (0 != rules[ALIGN_PARENT_BOTTOM]) {
        if (myHeight >= 0) {
            childParams.mBottom = myHeight - mPaddingBottom - childParams.bottomMargin;
        }
    }
}

private View getRelatedView(int[] rules, int relation) {
    int id = rules[relation];
    if (id != 0) {
        DependencyGraph.Node node = mGraph.mKeyNodes.get(id);
        if (node == null) return null;
        View v = node.view;

        // Find the first non-GONE view up the chain
        while (v.getVisibility() == View.GONE) {
            rules = ((LayoutParams) v.getLayoutParams()).getRules(v.getLayoutDirection());
            node = mGraph.mKeyNodes.get((rules[relation]));
            if (node == null) return null;
            v = node.view;
        }

        return v;
    }

    return null;
}

private LayoutParams getRelatedViewParams(int[] rules, int relation) {
    View v = getRelatedView(rules, relation);
    if (v != null) {
        ViewGroup.LayoutParams params = v.getLayoutParams();
        if (params instanceof LayoutParams) {
            return (LayoutParams) v.getLayoutParams();
        }
    }
    return null;
}

private int getRelatedViewBaselineOffset(int[] rules) {
    final View v = getRelatedView(rules, ALIGN_BASELINE);
    if (v != null) {
        final int baseline = v.getBaseline();
        if (baseline != -1) {
            final ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params instanceof LayoutParams) {
                final LayoutParams anchorParams = (LayoutParams) v.getLayoutParams();
                return anchorParams.mTop + baseline;
            }
        }
    }
    return -1;
}

private static void centerHorizontal(View child, LayoutParams params, int myWidth) {
    int childWidth = child.getMeasuredWidth();
    int left = (myWidth - childWidth) / 2;

    params.mLeft = left;
    params.mRight = left + childWidth;
}

private static void centerVertical(View child, LayoutParams params, int myHeight) {
    int childHeight = child.getMeasuredHeight();
    int top = (myHeight - childHeight) / 2;

    params.mTop = top;
    params.mBottom = top + childHeight;
}

@Override
protected void onLayout(boolean changed, int l, int t, int r, int b) {
    //  The layout has actually already been performed and the positions
    //  cached.  Apply the cached values to the children.

    //  布局实际上已经执行,位置缓存。将缓存的值应用于孩子

    final int count = getChildCount();

    for (int i = 0; i < count; i++) {
        View child = getChildAt(i);
        if (child.getVisibility() != GONE) {
            RelativeLayout.LayoutParams st =
                    (RelativeLayout.LayoutParams) child.getLayoutParams();
            child.layout(st.mLeft, st.mTop, st.mRight, st.mBottom);
        }
    }
}

@Override
public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new RelativeLayout.LayoutParams(getContext(), attrs);
}

/**
 * Returns a set of layout parameters with a width of
 * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT},
 * a height of {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} and no spanning.
 */
@Override
protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
}

// Override to allow type-checking of LayoutParams.
@Override
protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof RelativeLayout.LayoutParams;
}

@Override
protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
    if (sPreserveMarginParamsInLayoutParamConversion) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
    }
    return new LayoutParams(lp);
}

/**
 * @hide
 */
@Override
public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
    if (mTopToBottomLeftToRightSet == null) {
        mTopToBottomLeftToRightSet = new TreeSet<View>(new TopToBottomLeftToRightComparator());
    }

    // sort children top-to-bottom and left-to-right
    for (int i = 0, count = getChildCount(); i < count; i++) {
        mTopToBottomLeftToRightSet.add(getChildAt(i));
    }

    for (View view : mTopToBottomLeftToRightSet) {
        if (view.getVisibility() == View.VISIBLE
                && view.dispatchPopulateAccessibilityEvent(event)) {
            mTopToBottomLeftToRightSet.clear();
            return true;
        }
    }

    mTopToBottomLeftToRightSet.clear();
    return false;
}

@Override
public CharSequence getAccessibilityClassName() {
    return RelativeLayout.class.getName();
}

/**
 * Compares two views in left-to-right and top-to-bottom fashion.
 */
private class TopToBottomLeftToRightComparator implements Comparator<View> {
    public int compare(View first, View second) {
        // top - bottom
        int topDifference = first.getTop() - second.getTop();
        if (topDifference != 0) {
            return topDifference;
        }
        // left - right
        int leftDifference = first.getLeft() - second.getLeft();
        if (leftDifference != 0) {
            return leftDifference;
        }
        // break tie by height
        int heightDiference = first.getHeight() - second.getHeight();
        if (heightDiference != 0) {
            return heightDiference;
        }
        // break tie by width
        int widthDiference = first.getWidth() - second.getWidth();
        if (widthDiference != 0) {
            return widthDiference;
        }
        return 0;
    }
}

/**
 * Per-child layout information associated with RelativeLayout.
 *
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignWithParentIfMissing
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_toLeftOf
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_toRightOf
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_above
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_below
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignBaseline
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignLeft
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignTop
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignRight
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignBottom
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignParentLeft
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignParentTop
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignParentRight
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignParentBottom
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_centerInParent
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_centerHorizontal
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_centerVertical
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_toStartOf
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_toEndOf
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignStart
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignEnd
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignParentStart
 * @attr ref android.R.styleable#RelativeLayout_Layout_layout_alignParentEnd
 */
public static class LayoutParams extends ViewGroup.MarginLayoutParams {
    @ViewDebug.ExportedProperty(category = "layout", resolveId = true, indexMapping = {
            @ViewDebug.IntToString(from = ABOVE, to = "above"),
            @ViewDebug.IntToString(from = ALIGN_BASELINE, to = "alignBaseline"),
            @ViewDebug.IntToString(from = ALIGN_BOTTOM, to = "alignBottom"),
            @ViewDebug.IntToString(from = ALIGN_LEFT, to = "alignLeft"),
            @ViewDebug.IntToString(from = ALIGN_PARENT_BOTTOM, to = "alignParentBottom"),
            @ViewDebug.IntToString(from = ALIGN_PARENT_LEFT, to = "alignParentLeft"),
            @ViewDebug.IntToString(from = ALIGN_PARENT_RIGHT, to = "alignParentRight"),
            @ViewDebug.IntToString(from = ALIGN_PARENT_TOP, to = "alignParentTop"),
            @ViewDebug.IntToString(from = ALIGN_RIGHT, to = "alignRight"),
            @ViewDebug.IntToString(from = ALIGN_TOP, to = "alignTop"),
            @ViewDebug.IntToString(from = BELOW, to = "below"),
            @ViewDebug.IntToString(from = CENTER_HORIZONTAL, to = "centerHorizontal"),
            @ViewDebug.IntToString(from = CENTER_IN_PARENT, to = "center"),
            @ViewDebug.IntToString(from = CENTER_VERTICAL, to = "centerVertical"),
            @ViewDebug.IntToString(from = LEFT_OF, to = "leftOf"),
            @ViewDebug.IntToString(from = RIGHT_OF, to = "rightOf"),
            @ViewDebug.IntToString(from = ALIGN_START, to = "alignStart"),
            @ViewDebug.IntToString(from = ALIGN_END, to = "alignEnd"),
            @ViewDebug.IntToString(from = ALIGN_PARENT_START, to = "alignParentStart"),
            @ViewDebug.IntToString(from = ALIGN_PARENT_END, to = "alignParentEnd"),
            @ViewDebug.IntToString(from = START_OF, to = "startOf"),
            @ViewDebug.IntToString(from = END_OF, to = "endOf")
    }, mapping = {
            @ViewDebug.IntToString(from = TRUE, to = "true"),
            @ViewDebug.IntToString(from = 0, to = "false/NO_ID")
    })

    private int[] mRules = new int[VERB_COUNT];
    private int[] mInitialRules = new int[VERB_COUNT];

    private int mLeft, mTop, mRight, mBottom;

    /**
     * Whether this view had any relative rules modified following the most
     * recent resolution of layout direction.
     * 布局是否根据最近的布局方向改变而修改相关规则
     */
    private boolean mNeedsLayoutResolution;

    private boolean mRulesChanged = false;
    private boolean mIsRtlCompatibilityMode = false;

    /**
     * When true, uses the parent as the anchor if the anchor doesn't exist or if
     * the anchor's visibility is GONE.
     * <p>
     * 当为true时，如果锚不存在或锚点的可见性为GONE，则使用父作为锚点。
     */
    @ViewDebug.ExportedProperty(category = "layout")
    public boolean alignWithParent;

    public LayoutParams(Context c, AttributeSet attrs) {
        super(c, attrs);

        TypedArray a = c.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.RelativeLayout_Layout);

        final int targetSdkVersion = c.getApplicationInfo().targetSdkVersion;
        mIsRtlCompatibilityMode = (targetSdkVersion < JELLY_BEAN_MR1 ||
                !c.getApplicationInfo().hasRtlSupport());

        final int[] rules = mRules;
        //noinspection MismatchedReadAndWriteOfArray
        final int[] initialRules = mInitialRules;

        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignWithParentIfMissing:
                    alignWithParent = a.getBoolean(attr, false);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_toLeftOf:
                    rules[LEFT_OF] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_toRightOf:
                    rules[RIGHT_OF] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_above:
                    rules[ABOVE] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_below:
                    rules[BELOW] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignBaseline:
                    rules[ALIGN_BASELINE] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignLeft:
                    rules[ALIGN_LEFT] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignTop:
                    rules[ALIGN_TOP] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignRight:
                    rules[ALIGN_RIGHT] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignBottom:
                    rules[ALIGN_BOTTOM] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignParentLeft:
                    rules[ALIGN_PARENT_LEFT] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignParentTop:
                    rules[ALIGN_PARENT_TOP] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignParentRight:
                    rules[ALIGN_PARENT_RIGHT] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignParentBottom:
                    rules[ALIGN_PARENT_BOTTOM] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_centerInParent:
                    rules[CENTER_IN_PARENT] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_centerHorizontal:
                    rules[CENTER_HORIZONTAL] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_centerVertical:
                    rules[CENTER_VERTICAL] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_toStartOf:
                    rules[START_OF] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_toEndOf:
                    rules[END_OF] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignStart:
                    rules[ALIGN_START] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignEnd:
                    rules[ALIGN_END] = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignParentStart:
                    rules[ALIGN_PARENT_START] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
                case com.android.internal.R.styleable.RelativeLayout_Layout_layout_alignParentEnd:
                    rules[ALIGN_PARENT_END] = a.getBoolean(attr, false) ? TRUE : 0;
                    break;
            }
        }
        mRulesChanged = true;
        System.arraycopy(rules, LEFT_OF, initialRules, LEFT_OF, VERB_COUNT);

        a.recycle();
    }

    public LayoutParams(int w, int h) {
        super(w, h);
    }

    /**
     * {@inheritDoc}
     */
    public LayoutParams(ViewGroup.LayoutParams source) {
        super(source);
    }

    /**
     * {@inheritDoc}
     */
    public LayoutParams(ViewGroup.MarginLayoutParams source) {
        super(source);
    }

    /**
     * Copy constructor. Clones the width, height, margin values, and rules
     * of the source.
     *
     * @param source The layout params to copy from.
     */
    public LayoutParams(LayoutParams source) {
        super(source);

        this.mIsRtlCompatibilityMode = source.mIsRtlCompatibilityMode;
        this.mRulesChanged = source.mRulesChanged;
        this.alignWithParent = source.alignWithParent;

        System.arraycopy(source.mRules, LEFT_OF, this.mRules, LEFT_OF, VERB_COUNT);
        System.arraycopy(
                source.mInitialRules, LEFT_OF, this.mInitialRules, LEFT_OF, VERB_COUNT);
    }

    @Override
    public String debug(String output) {
        return output + "ViewGroup.LayoutParams={ width=" + sizeToString(width) +
                ", height=" + sizeToString(height) + " }";
    }

    /**
     * Adds a layout rule to be interpreted by the RelativeLayout.
     * <p>
     * This method should only be used for verbs that don't refer to a
     * sibling (ex. {@link #ALIGN_RIGHT}) or take a boolean
     * value ({@link #TRUE} for true or 0 for false). To
     * specify a verb that takes a subject, use {@link #addRule(int, int)}.
     * <p>
     * If the rule is relative to the layout direction (ex.
     * {@link #ALIGN_PARENT_START}), then the layout direction must be
     * resolved using {@link #resolveLayoutDirection(int)} before calling
     * {@link #getRule(int)} an absolute rule (ex.
     * {@link #ALIGN_PARENT_LEFT}.
     *
     * @param verb a layout verb, such as {@link #ALIGN_PARENT_LEFT}
     * @see #addRule(int, int)
     * @see #removeRule(int)
     * @see #getRule(int)
     */
    public void addRule(int verb) {
        addRule(verb, TRUE);
    }

    /**
     * Adds a layout rule to be interpreted by the RelativeLayout.
     * <p>
     * Use this for verbs that refer to a sibling (ex.
     * {@link #ALIGN_RIGHT}) or take a boolean value (ex.
     * {@link #CENTER_IN_PARENT}).
     * <p>
     * If the rule is relative to the layout direction (ex.
     * {@link #START_OF}), then the layout direction must be resolved using
     * {@link #resolveLayoutDirection(int)} before calling
     * {@link #getRule(int)} with an absolute rule (ex. {@link #LEFT_OF}.
     *
     * @param verb    a layout verb, such as {@link #ALIGN_RIGHT}
     * @param subject the ID of another view to use as an anchor, or a
     *                boolean value (represented as {@link #TRUE} for true
     *                or 0 for false)
     * @see #addRule(int)
     * @see #removeRule(int)
     * @see #getRule(int)
     */
    public void addRule(int verb, int subject) {
        // If we're removing a relative rule, we'll need to force layout
        // resolution the next time it's requested.
        if (!mNeedsLayoutResolution && isRelativeRule(verb)
                && mInitialRules[verb] != 0 && subject == 0) {
            mNeedsLayoutResolution = true;
        }

        mRules[verb] = subject;
        mInitialRules[verb] = subject;
        mRulesChanged = true;
    }

    /**
     * Removes a layout rule to be interpreted by the RelativeLayout.
     * <p>
     * If the rule is relative to the layout direction (ex.
     * {@link #START_OF}, {@link #ALIGN_PARENT_START}, etc.) then the
     * layout direction must be resolved using
     * {@link #resolveLayoutDirection(int)} before before calling
     * {@link #getRule(int)} with an absolute rule (ex. {@link #LEFT_OF}.
     *
     * @param verb One of the verbs defined by
     *             {@link android.widget.RelativeLayout RelativeLayout}, such as
     *             ALIGN_WITH_PARENT_LEFT.
     * @see #addRule(int)
     * @see #addRule(int, int)
     * @see #getRule(int)
     */
    public void removeRule(int verb) {
        addRule(verb, 0);
    }

    /**
     * Returns the layout rule associated with a specific verb.
     *
     * @param verb one of the verbs defined by {@link RelativeLayout}, such
     *             as ALIGN_WITH_PARENT_LEFT
     * @return the id of another view to use as an anchor, a boolean value
     * (represented as {@link RelativeLayout#TRUE} for true
     * or 0 for false), or -1 for verbs that don't refer to another
     * sibling (for example, ALIGN_WITH_PARENT_BOTTOM)
     * @see #addRule(int)
     * @see #addRule(int, int)
     */
    public int getRule(int verb) {
        return mRules[verb];
    }

    private boolean hasRelativeRules() {
        return (mInitialRules[START_OF] != 0 || mInitialRules[END_OF] != 0 ||
                mInitialRules[ALIGN_START] != 0 || mInitialRules[ALIGN_END] != 0 ||
                mInitialRules[ALIGN_PARENT_START] != 0 || mInitialRules[ALIGN_PARENT_END] != 0);
    }

    private boolean isRelativeRule(int rule) {
        return rule == START_OF || rule == END_OF
                || rule == ALIGN_START || rule == ALIGN_END
                || rule == ALIGN_PARENT_START || rule == ALIGN_PARENT_END;
    }

    // The way we are resolving rules depends on the layout direction and if we are pre JB MR1
    // or not.
    //
    // If we are pre JB MR1 (said as "RTL compatibility mode"), "left"/"right" rules are having
    // predominance over any "start/end" rules that could have been defined. A special case:
    // if no "left"/"right" rule has been defined and "start"/"end" rules are defined then we
    // resolve those "start"/"end" rules to "left"/"right" respectively.
    //
    // If we are JB MR1+, then "start"/"end" rules are having predominance over "left"/"right"
    // rules. If no "start"/"end" rule is defined then we use "left"/"right" rules.
    //
    // In all cases, the result of the resolution should clear the "start"/"end" rules to leave
    // only the "left"/"right" rules at the end.
    private void resolveRules(int layoutDirection) {
        final boolean isLayoutRtl = (layoutDirection == View.LAYOUT_DIRECTION_RTL);

        // Reset to initial state
        System.arraycopy(mInitialRules, LEFT_OF, mRules, LEFT_OF, VERB_COUNT);

        // Apply rules depending on direction and if we are in RTL compatibility mode
        if (mIsRtlCompatibilityMode) {
            if (mRules[ALIGN_START] != 0) {
                if (mRules[ALIGN_LEFT] == 0) {
                    // "left" rule is not defined but "start" rule is: use the "start" rule as
                    // the "left" rule
                    mRules[ALIGN_LEFT] = mRules[ALIGN_START];
                }
                mRules[ALIGN_START] = 0;
            }

            if (mRules[ALIGN_END] != 0) {
                if (mRules[ALIGN_RIGHT] == 0) {
                    // "right" rule is not defined but "end" rule is: use the "end" rule as the
                    // "right" rule
                    mRules[ALIGN_RIGHT] = mRules[ALIGN_END];
                }
                mRules[ALIGN_END] = 0;
            }

            if (mRules[START_OF] != 0) {
                if (mRules[LEFT_OF] == 0) {
                    // "left" rule is not defined but "start" rule is: use the "start" rule as
                    // the "left" rule
                    mRules[LEFT_OF] = mRules[START_OF];
                }
                mRules[START_OF] = 0;
            }

            if (mRules[END_OF] != 0) {
                if (mRules[RIGHT_OF] == 0) {
                    // "right" rule is not defined but "end" rule is: use the "end" rule as the
                    // "right" rule
                    mRules[RIGHT_OF] = mRules[END_OF];
                }
                mRules[END_OF] = 0;
            }

            if (mRules[ALIGN_PARENT_START] != 0) {
                if (mRules[ALIGN_PARENT_LEFT] == 0) {
                    // "left" rule is not defined but "start" rule is: use the "start" rule as
                    // the "left" rule
                    mRules[ALIGN_PARENT_LEFT] = mRules[ALIGN_PARENT_START];
                }
                mRules[ALIGN_PARENT_START] = 0;
            }

            if (mRules[ALIGN_PARENT_END] != 0) {
                if (mRules[ALIGN_PARENT_RIGHT] == 0) {
                    // "right" rule is not defined but "end" rule is: use the "end" rule as the
                    // "right" rule
                    mRules[ALIGN_PARENT_RIGHT] = mRules[ALIGN_PARENT_END];
                }
                mRules[ALIGN_PARENT_END] = 0;
            }
        } else {
            // JB MR1+ case
            if ((mRules[ALIGN_START] != 0 || mRules[ALIGN_END] != 0) &&
                    (mRules[ALIGN_LEFT] != 0 || mRules[ALIGN_RIGHT] != 0)) {
                // "start"/"end" rules take precedence over "left"/"right" rules
                mRules[ALIGN_LEFT] = 0;
                mRules[ALIGN_RIGHT] = 0;
            }
            if (mRules[ALIGN_START] != 0) {
                // "start" rule resolved to "left" or "right" depending on the direction
                mRules[isLayoutRtl ? ALIGN_RIGHT : ALIGN_LEFT] = mRules[ALIGN_START];
                mRules[ALIGN_START] = 0;
            }
            if (mRules[ALIGN_END] != 0) {
                // "end" rule resolved to "left" or "right" depending on the direction
                mRules[isLayoutRtl ? ALIGN_LEFT : ALIGN_RIGHT] = mRules[ALIGN_END];
                mRules[ALIGN_END] = 0;
            }

            if ((mRules[START_OF] != 0 || mRules[END_OF] != 0) &&
                    (mRules[LEFT_OF] != 0 || mRules[RIGHT_OF] != 0)) {
                // "start"/"end" rules take precedence over "left"/"right" rules
                mRules[LEFT_OF] = 0;
                mRules[RIGHT_OF] = 0;
            }
            if (mRules[START_OF] != 0) {
                // "start" rule resolved to "left" or "right" depending on the direction
                mRules[isLayoutRtl ? RIGHT_OF : LEFT_OF] = mRules[START_OF];
                mRules[START_OF] = 0;
            }
            if (mRules[END_OF] != 0) {
                // "end" rule resolved to "left" or "right" depending on the direction
                mRules[isLayoutRtl ? LEFT_OF : RIGHT_OF] = mRules[END_OF];
                mRules[END_OF] = 0;
            }

            if ((mRules[ALIGN_PARENT_START] != 0 || mRules[ALIGN_PARENT_END] != 0) &&
                    (mRules[ALIGN_PARENT_LEFT] != 0 || mRules[ALIGN_PARENT_RIGHT] != 0)) {
                // "start"/"end" rules take precedence over "left"/"right" rules
                mRules[ALIGN_PARENT_LEFT] = 0;
                mRules[ALIGN_PARENT_RIGHT] = 0;
            }
            if (mRules[ALIGN_PARENT_START] != 0) {
                // "start" rule resolved to "left" or "right" depending on the direction
                mRules[isLayoutRtl ? ALIGN_PARENT_RIGHT : ALIGN_PARENT_LEFT] = mRules[ALIGN_PARENT_START];
                mRules[ALIGN_PARENT_START] = 0;
            }
            if (mRules[ALIGN_PARENT_END] != 0) {
                // "end" rule resolved to "left" or "right" depending on the direction
                mRules[isLayoutRtl ? ALIGN_PARENT_LEFT : ALIGN_PARENT_RIGHT] = mRules[ALIGN_PARENT_END];
                mRules[ALIGN_PARENT_END] = 0;
            }
        }

        mRulesChanged = false;
        mNeedsLayoutResolution = false;
    }

    /**
     * Retrieves a complete list of all supported rules, where the index is the rule
     * verb, and the element value is the value specified, or "false" if it was never
     * set. If there are relative rules defined (*_START / *_END), they will be resolved
     * depending on the layout direction.
     *
     * @param layoutDirection the direction of the layout.
     *                        Should be either {@link View#LAYOUT_DIRECTION_LTR}
     *                        or {@link View#LAYOUT_DIRECTION_RTL}
     * @return the supported rules
     * @hide
     * @see #addRule(int, int)
     */
    public int[] getRules(int layoutDirection) {
        resolveLayoutDirection(layoutDirection);
        return mRules;
    }

    /**
     * Retrieves a complete list of all supported rules, where the index is the rule
     * verb, and the element value is the value specified, or "false" if it was never
     * set. There will be no resolution of relative rules done.
     *
     * @return the supported rules
     * @see #addRule(int, int)
     */
    public int[] getRules() {
        return mRules;
    }

    /**
     * This will be called by {@link android.view.View#requestLayout()} to
     * resolve layout parameters that are relative to the layout direction.
     * <p>
     * After this method is called, any rules using layout-relative verbs
     * (ex. {@link #START_OF}) previously added via {@link #addRule(int)}
     * may only be accessed via their resolved absolute verbs (ex.
     * {@link #LEFT_OF}).
     */
    @Override
    public void resolveLayoutDirection(int layoutDirection) {
        if (shouldResolveLayoutDirection(layoutDirection)) {
            resolveRules(layoutDirection);
        }

        // This will set the layout direction.
        super.resolveLayoutDirection(layoutDirection);
    }

    private boolean shouldResolveLayoutDirection(int layoutDirection) {
        return (mNeedsLayoutResolution || hasRelativeRules())
                && (mRulesChanged || layoutDirection != getLayoutDirection());
    }

    /**
     * @hide
     */
    @Override
    protected void encodeProperties(@NonNull ViewHierarchyEncoder encoder) {
        super.encodeProperties(encoder);
        encoder.addProperty("layout:alignWithParent", alignWithParent);
    }
}

private static class DependencyGraph {
    /**
     * List of all views in the graph.
     * 列出所有的View放在图中
     */
    private ArrayList<Node> mNodes = new ArrayList<Node>();

    /**
     * List of nodes in the graph. Each node is identified by its
     * view id (see View#getId()).
     * 图中的所有节点都会有一个特定的id
     */
    private SparseArray<Node> mKeyNodes = new SparseArray<Node>();

    /**
     * Temporary data structure used to build the list of roots
     * for this graph.
     * <p>
     * 临时数据结构用于构建此图的根列表。
     */
    private ArrayDeque<Node> mRoots = new ArrayDeque<Node>();

    /**
     * Clears the graph.
     */
    void clear() {
        final ArrayList<Node> nodes = mNodes;
        final int count = nodes.size();

        for (int i = 0; i < count; i++) {
            nodes.get(i).release();
        }
        nodes.clear();

        mKeyNodes.clear();
        mRoots.clear();
    }

    /**
     * Adds a view to the graph.
     * 将view添加进图中
     *
     * @param view The view to be added as a node to the graph.
     */
    void add(View view) {
        final int id = view.getId();
        //有图就有节点,根据view生成一个节点
        final Node node = Node.acquire(view);

        //如果当前的view有有效id则将其加入List中
        if (id != View.NO_ID) {
            mKeyNodes.put(id, node);
        }

        mNodes.add(node);
    }

    /**
     * Builds a sorted list of views. The sorting order depends on the dependencies
     * between the view. For instance, if view C needs view A to be processed first
     * and view A needs view B to be processed first, the dependency graph
     * is: B -> A -> C. The sorted array will contain views B, A and C in this order.
     * <p>
     * 创建一个有序的view列表
     *
     * @param sorted The sorted list of views. The length of this array must
     *               be equal to getChildCount().
     * @param rules  The list of rules to take into account.
     */
    void getSortedViews(View[] sorted, int... rules) {
        //首先找到不依赖别的view的view作为root节点
        final ArrayDeque<Node> roots = findRoots(rules);
        int index = 0;

        Node node;
        //读取roots下一个node,直到全部遍历完
        while ((node = roots.pollLast()) != null) {
            //取得view
            final View view = node.view;
            //取得view对应的id值
            final int key = view.getId();
            //把符合规则的view加入到sorted中
            sorted[index++] = view;


            //根据findRoots()方法分析
            //dependents里存的是依赖别人的node
            //如果A、C依赖的是B, 那么B的依赖表中存的是A、C
            final ArrayMap<Node, DependencyGraph> dependents = node.dependents;
            final int count = dependents.size();
            //编辑所有依赖自己的node
            for (int i = 0; i < count; i++) {
                final Node dependent = dependents.keyAt(i);
                //dependencies存的是被依赖node的规则和node
                //如果A依赖B和D才能确定位置,那么dependcies = B,D
                final SparseArray<Node> dependencies = dependent.dependencies;

                //移除当前node和dependencies的依赖关系
                //如果dependencies只和当前的node有依赖,那么移除之后
                //dependencies node也视为rootNode
                //如果B只被A依赖,那么移除和A的关系之后,B就不被任何Node依赖
                dependencies.remove(key);
                if (dependencies.size() == 0) {
                    roots.add(dependent);
                }
            }
        }

        //循环依赖异常报错
        if (index < sorted.length) {
            throw new IllegalStateException("Circular dependencies cannot exist"
                    + " in RelativeLayout");
        }
    }

    /**
     * Finds the roots of the graph. A root is a node with no dependency and
     * with [0..n] dependents.
     *
     * @param rulesFilter The list of rules to consider when building the
     *                    dependencies
     * @return A list of node, each being a root of the graph
     */
    private ArrayDeque<Node> findRoots(int[] rulesFilter) {
        final SparseArray<Node> keyNodes = mKeyNodes;
        final ArrayList<Node> nodes = mNodes;
        final int count = nodes.size();

        // Find roots can be invoked several times, so make sure to clear
        // all dependents and dependencies before running the algorithm

        //查找根节点可能或被调用多次,因此要在运行算法之前确保清除所有依赖关系

        for (int i = 0; i < count; i++) {
            final Node node = nodes.get(i);
            node.dependents.clear();
            node.dependencies.clear();
        }

        // Builds up the dependents and dependencies for each node of the graph

        // 构建图的每个节点的依赖关系和依赖
        // 遍历所有node,node里存了当前的view,它所依赖的关系

        for (int i = 0; i < count; i++) {
            final Node node = nodes.get(i);

            final LayoutParams layoutParams = (LayoutParams) node.view.getLayoutParams();

            //取出当前view所有的依赖关系
            final int[] rules = layoutParams.mRules;
            final int rulesCount = rulesFilter.length;

            // Look only the the rules passed in parameter, this way we build only the
            // dependencies for a specific set of rules

            //只查看参数中传递的规则,这样我们只构建一条特殊规则的依赖关系

            for (int j = 0; j < rulesCount; j++) {
                final int rule = rules[rulesFilter[j]];
                if (rule > 0) {
                    // The node this node depends on
                    // 该节点依赖的节点
                    final Node dependency = keyNodes.get(rule);

                    // Skip unknowns and self dependencies
                    // 忽略未知情况和自我依赖
                    if (dependency == null || dependency == node) {
                        continue;
                    }

                    // 这里一定要分清楚dependencies和dependency
                    //dependents里存的是依赖别人的node
                    //dependencies存的是被依赖node的规则和node
                    //举个例子 A View toLeftOf B View
                    // A 依赖 B
                    //dependecy.dependents.put(A,this);
                    //node.dependencies.put(rule,B);

                    // Add the current node as a dependent
                    dependency.dependents.put(node, this);
                    // Add a dependency to the current node
                    node.dependencies.put(rule, dependency);
                }
            }
        }

        final ArrayDeque<Node> roots = mRoots;
        roots.clear();

        // Finds all the roots in the graph: all nodes with no dependencies
        // 找到图中所有的根节点:所有节点无依赖
        for (int i = 0; i < count; i++) {
            final Node node = nodes.get(i);
            //如果node里存的依赖关系为0,即该view不依赖任何view
            if (node.dependencies.size() == 0) roots.addLast(node);
        }

        return roots;
    }

    /**
     * A node in the dependency graph. A node is a view, its list of dependencies
     * and its list of dependents.
     * <p>
     * 依赖图中的一个节点。 节点是一个View，其依赖关系列表及其依赖列表。
     * <p>
     * A node with no dependent is considered a root of the graph.
     * 一个节点没有依赖的话会被认为是这个视图根节点
     */
    static class Node {
        /**
         * The view representing this node in the layout.
         * 在布局中表示此节点的视图。
         */
        View view;

        /**
         * The list of dependents for this node; a dependent is a node
         * that needs this node to be processed first.
         * <p>
         * 此节点的依赖列表; 一个依赖者是一个需要首先处理该节点的节点。
         */
        final ArrayMap<Node, DependencyGraph> dependents =
                new ArrayMap<Node, DependencyGraph>();

        /**
         * The list of dependencies for this node.
         * <p>
         * 依赖该节点的列表。
         */
        final SparseArray<Node> dependencies = new SparseArray<Node>();

        /*
         * START POOL IMPLEMENTATION
         */
        // The pool is static, so all nodes instances are shared across
        // activities, that's why we give it a rather high limit
        private static final int POOL_LIMIT = 100;
        private static final SynchronizedPool<Node> sPool =
                new SynchronizedPool<Node>(POOL_LIMIT);

        static Node acquire(View view) {
            Node node = sPool.acquire();
            if (node == null) {
                node = new Node();
            }
            node.view = view;
            return node;
        }

        void release() {
            view = null;
            dependents.clear();
            dependencies.clear();

            sPool.release(this);
        }
    /*
     * END POOL IMPLEMENTATION
     */
    }
}
}
