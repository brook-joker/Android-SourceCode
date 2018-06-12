/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package java.awt;

import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * A flow layout arranges components in a directional flow, much
 * like lines of text in a paragraph. The flow direction is
 * determined by the container's <code>componentOrientation</code>
 * property and may be one of two values:
 * <ul>
 * <li><code>ComponentOrientation.LEFT_TO_RIGHT</code>
 * <li><code>ComponentOrientation.RIGHT_TO_LEFT</code>
 * </ul>
 * Flow layouts are typically used
 * to arrange buttons in a panel. It arranges buttons
 * horizontally until no more buttons fit on the same line.
 * The line alignment is determined by the <code>align</code>
 * property. The possible values are:
 * <ul>
 * <li>{@link #LEFT LEFT}
 * <li>{@link #RIGHT RIGHT}
 * <li>{@link #CENTER CENTER}
 * <li>{@link #LEADING LEADING}
 * <li>{@link #TRAILING TRAILING}
 * </ul>
 * <p>
 * For example, the following picture shows an applet using the flow
 * layout manager (its default layout manager) to position three buttons:
 * <p>
 * <img src="doc-files/FlowLayout-1.gif"
 * ALT="Graphic of Layout for Three Buttons"
 * style="float:center; margin: 7px 10px;">
 * <p>
 * Here is the code for this applet:
 * <p>
 * <hr><blockquote><pre>
 * import java.awt.*;
 * import java.applet.Applet;
 * <p>
 * public class myButtons extends Applet {
 *     Button button1, button2, button3;
 *     public void init() {
 *         button1 = new Button("Ok");
 *         button2 = new Button("Open");
 *         button3 = new Button("Close");
 *         add(button1);
 *         add(button2);
 *         add(button3);
 *     }
 * }
 * </pre></blockquote><hr>
 * <p>
 * A flow layout lets each component assume its natural (preferred) size.
 *
 * @author Arthur van Hoff
 * @author Sami ShaioLayoutManager
 * @see ComponentOrientation
 * @since JDK1.0
 */
public class FlowLayout implements LayoutManager, java.io.Serializable {

    /**
     * This value indicates that each row of components
     * should be left-justified.
     */
    public static final int LEFT = 0;

    /**
     * This value indicates that each row of components
     * should be centered.
     */
    public static final int CENTER = 1;

    /**
     * This value indicates that each row of components
     * should be right-justified.
     */
    public static final int RIGHT = 2;

    /**
     * This value indicates that each row of components
     * should be justified to the leading edge of the container's
     * orientation, for example, to the left in left-to-right orientations.
     *
     * 该值表示每一行组件应该对齐到容器的前沿方向,例如,从左到右的方向向左
     * @see java.awt.Component#getComponentOrientation
     * @see java.awt.ComponentOrientation
     * @since 1.2
     */
    public static final int LEADING = 3;

    /**
     * This value indicates that each row of components
     * should be justified to the trailing edge of the container's
     * orientation, for example, to the right in left-to-right orientations.
     *
     * 该值表示每一行组件应该对齐到容器的后端方向,例如,从左到右的方向向右
     * @see java.awt.Component#getComponentOrientation
     * @see java.awt.ComponentOrientation
     * @since 1.2
     */
    public static final int TRAILING = 4;

    /**
     * <code>align</code> is the property that determines
     * how each row distributes empty space.
     * It can be one of the following values:
     * <ul>
     * <li><code>LEFT</code>
     * <li><code>RIGHT</code>
     * <li><code>CENTER</code>
     * </ul>
     *
     * @serial
     * @see #getAlignment
     * @see #setAlignment
     */
    int align;          // This is for 1.1 serialization compatibility

    /**
     * <code>newAlign</code> is the property that determines
     * how each row distributes empty space for the Java 2 platform,
     * v1.2 and greater.
     * newAlign是确定每行为Java2平台v1.2及更高版本分配空白空间的属性
     * It can be one of the following three values:
     * <ul>
     * <li><code>LEFT</code>
     * <li><code>RIGHT</code>
     * <li><code>CENTER</code>
     * <li><code>LEADING</code>
     * <li><code>TRAILING</code>
     * </ul>
     */
    int newAlign;       // This is the one we actually use

    /**
     * The flow layout manager allows a seperation of
     * components with gaps.  The horizontal gap will
     * specify the space between components and between
     * the components and the borders of the
     * <p>
     * 流式布局管理器允许使用间隙分离组件。
     * 水平间隙将指定组件之间以及组件与边框之间的控件
     */
    int hgap;

    /**
     * The flow layout manager allows a seperation of
     * components with gaps.  The vertical gap will
     * specify the space between rows and between the
     * the rows and the borders of the <code>Container</code>.
     *
     * @serial
     * @see #getHgap()
     * @see #setHgap(int)
     */
    int vgap;

    /**
     * If true, components will be aligned on their baseline.
     */
    private boolean alignOnBaseline;

    /*
     * JDK 1.1 serialVersionUID
     */
    private static final long serialVersionUID = -7262534875583282631L;

    /**
     * Constructs a new <code>FlowLayout</code> with a centered alignment and a
     * default 5-unit horizontal and vertical gap.
     */
    public FlowLayout() {
        this(CENTER, 5, 5);
    }

    /**
     * Constructs a new <code>FlowLayout</code> with the specified
     * alignment and a default 5-unit horizontal and vertical gap.
     * The value of the alignment argument must be one of
     * <code>FlowLayout.LEFT</code>, <code>FlowLayout.RIGHT</code>,
     * <code>FlowLayout.CENTER</code>, <code>FlowLayout.LEADING</code>,
     * or <code>FlowLayout.TRAILING</code>.
     *
     * @param align the alignment value
     */
    public FlowLayout(int align) {
        this(align, 5, 5);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps.
     * <p>
     * The value of the alignment argument must be one of
     * <code>FlowLayout.LEFT</code>, <code>FlowLayout.RIGHT</code>,
     * <code>FlowLayout.CENTER</code>, <code>FlowLayout.LEADING</code>,
     * or <code>FlowLayout.TRAILING</code>.
     *
     * @param align the alignment value
     * @param hgap  the horizontal gap between components
     *              and between the components and the
     *              borders of the <code>Container</code>
     * @param vgap  the vertical gap between components
     *              and between the components and the
     *              borders of the <code>Container</code>
     */
    public FlowLayout(int align, int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
        setAlignment(align);
    }

    /**
     * Gets the alignment for this layout.
     * Possible values are <code>FlowLayout.LEFT</code>,
     * <code>FlowLayout.RIGHT</code>, <code>FlowLayout.CENTER</code>,
     * <code>FlowLayout.LEADING</code>,
     * or <code>FlowLayout.TRAILING</code>.
     *
     * @return the alignment value for this layout
     * @see java.awt.FlowLayout#setAlignment
     * @since JDK1.1
     */
    public int getAlignment() {
        return newAlign;
    }

    /**
     * Sets the alignment for this layout.
     * Possible values are
     * <ul>
     * <li><code>FlowLayout.LEFT</code>
     * <li><code>FlowLayout.RIGHT</code>
     * <li><code>FlowLayout.CENTER</code>
     * <li><code>FlowLayout.LEADING</code>
     * <li><code>FlowLayout.TRAILING</code>
     * </ul>
     *
     * @param align one of the alignment values shown above
     * @see #getAlignment()
     * @since JDK1.1
     */
    public void setAlignment(int align) {
        this.newAlign = align;

        // this.align is used only for serialization compatibility,
        // so set it to a value compatible with the 1.1 version
        // of the class

        switch (align) {
            case LEADING:
                this.align = LEFT;
                break;
            case TRAILING:
                this.align = RIGHT;
                break;
            default:
                this.align = align;
                break;
        }
    }

    /**
     * Gets the horizontal gap between components
     * and between the components and the borders
     * of the <code>Container</code>
     *
     * @return the horizontal gap between components
     * and between the components and the borders
     * of the <code>Container</code>
     * @see java.awt.FlowLayout#setHgap
     * @since JDK1.1
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components and
     * between the components and the borders of the
     * <code>Container</code>.
     *
     * @param hgap the horizontal gap between components
     *             and between the components and the borders
     *             of the <code>Container</code>
     * @see java.awt.FlowLayout#getHgap
     * @since JDK1.1
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components and
     * between the components and the borders of the
     * <code>Container</code>.
     *
     * @return the vertical gap between components
     * and between the components and the borders
     * of the <code>Container</code>
     * @see java.awt.FlowLayout#setVgap
     * @since JDK1.1
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components and between
     * the components and the borders of the <code>Container</code>.
     *
     * @param vgap the vertical gap between components
     *             and between the components and the borders
     *             of the <code>Container</code>
     * @see java.awt.FlowLayout#getVgap
     * @since JDK1.1
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Sets whether or not components should be vertically aligned along their
     * baseline.  Components that do not have a baseline will be centered.
     * The default is false.
     *
     * @param alignOnBaseline whether or not components should be
     *                        vertically aligned on their baseline
     * @since 1.6
     */
    public void setAlignOnBaseline(boolean alignOnBaseline) {
        this.alignOnBaseline = alignOnBaseline;
    }

    /**
     * Returns true if components are to be vertically aligned along
     * their baseline.  The default is false.
     *
     * @return true if components are to be vertically aligned along
     * their baseline
     * @since 1.6
     */
    public boolean getAlignOnBaseline() {
        return alignOnBaseline;
    }

    /**
     * Adds the specified component to the layout.
     * Not used by this class.
     *
     * @param name the name of the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * Not used by this class.
     *
     * @param comp the component to remove
     * @see java.awt.Container#removeAll
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Returns the preferred dimensions for this layout given the
     * <i>visible</i> components in the specified target container.
     *
     * @param target the container that needs to be laid out
     * @return the preferred dimensions to lay out the
     * subcomponents of the specified container
     * @see Container
     * @see #minimumLayoutSize
     * @see java.awt.Container#getPreferredSize
     */
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            //初始化一个大小为0子尺寸容器类来记录所有组件尺寸的总和
            Dimension dim = new Dimension(0, 0);
            //获取容器中的组件数量
            int nmembers = target.getComponentCount();
            //标记第一个组件是否可视
            boolean firstVisibleComponent = true;
            //基准线
            boolean useBaseline = getAlignOnBaseline();
            //上偏移量
            int maxAscent = 0;
            //下偏移量
            int maxDescent = 0;

            for (int i = 0; i < nmembers; i++) {
                //遍历组件
                Component m = target.getComponent(i);
                //是否可见
                if (m.isVisible()) {
                    //获取组件尺寸
                    Dimension d = m.getPreferredSize();
                    //计算当前能容纳下所有组件的最大高度
                    dim.height = Math.max(dim.height, d.height);
                    if (firstVisibleComponent) {
                        //如果是第一个可视组件width就不用加水平间隙hgap
                        firstVisibleComponent = false;
                    } else {
                        //否则的话加上水平间隙hagp
                        dim.width += hgap;
                    }
                    //容器的宽度增加一个组件的宽度
                    dim.width += d.width;
                    //基准线适配
                    if (useBaseline) {
                        //根据组件的宽高获取对应的基准线
                        int baseline = m.getBaseline(d.width, d.height);
                        //非负数情况
                        if (baseline >= 0) {
                            //记录向上的偏移量
                            maxAscent = Math.max(maxAscent, baseline);
                            //记录向下的偏移量
                            maxDescent = Math.max(maxDescent, d.height - baseline);
                        }
                    }
                }
            }
            if (useBaseline) {
                //高度修正
                dim.height = Math.max(maxAscent + maxDescent, dim.height);
            }
            Insets insets = target.getInsets();
            //因为容器的边缘和组件之间会存在一个间隙,所以最后需要加上四周的间隙来最终确定容器的尺寸大小
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i>
     * components contained in the specified target container.
     *
     * @param target the container that needs to be laid out
     * @return the minimum dimensions to lay out the
     * subcomponents of the specified container
     * @see #preferredLayoutSize
     * @see java.awt.Container
     * @see java.awt.Container#doLayout
     */
    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            //判断是否使用了基准线
            boolean useBaseline = getAlignOnBaseline();
            //初始化一个大小为0子尺寸容器类来记录所有组件尺寸的总和
            Dimension dim = new Dimension(0, 0);
            //获取组件数量
            int nmembers = target.getComponentCount();
            //上偏移量
            int maxAscent = 0;
            //下偏移量
            int maxDescent = 0;
            //第一个可见组件
            boolean firstVisibleComponent = true;

            for (int i = 0; i < nmembers; i++) {
                //遍历组件
                Component m = target.getComponent(i);
                //是否可见
                if (m.visible) {
                    //获取最小尺寸
                    Dimension d = m.getMinimumSize();
                    //开始记录行高
                    dim.height = Math.max(dim.height, d.height);
                    if (firstVisibleComponent) {
                        firstVisibleComponent = false;
                    } else {
                        //记录水平间隙
                        dim.width += hgap;
                    }
                    //记录宽度
                    dim.width += d.width;
                    if (useBaseline) {
                        //如果使用了基准线的话需要对高度进行修正
                        int baseline = m.getBaseline(d.width, d.height);
                        if (baseline >= 0) {
                            maxAscent = Math.max(maxAscent, baseline);
                            maxDescent = Math.max(maxDescent,
                                    dim.height - baseline);
                        }
                    }
                }
            }
            //高度修正
            if (useBaseline) {
                dim.height = Math.max(maxAscent + maxDescent, dim.height);
            }
            Insets insets = target.getInsets();
            //因为容器的边缘和组件之间会存在一个间隙,所以最后需要加上四周的间隙来最终确定容器的尺寸大小
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    /**
     * Centers the elements in the specified row, if there is any slack.
     * 将元素放到指定的行中
     *
     * @param target      the component which needs to be moved 需要被移动的组件
     * @param x           the x coordinate  x坐标
     * @param y           the y coordinate  y坐标
     * @param width       the width dimensions 宽
     * @param height      the height dimensions 高
     * @param rowStart    the beginning of the row   行始端
     * @param rowEnd      the the ending of the row  行末端
     * @param useBaseline Whether or not to align on baseline. 是否发对齐基准线
     * @param ascent      Ascent for the components. This is only valid if
     *                    useBaseline is true.
     *                    使组件向上偏移,这个只有在使用基准线的前提下有效
     * @param descent     Ascent for the components. This is only valid if
     *                    useBaseline is true.
     *                    使组件向下偏移,这个只有在使用记住县的前提下有效
     * @return actual row height 返回行高
     */
    private int moveComponents(Container target, int x, int y, int width, int height,
                               int rowStart, int rowEnd, boolean ltr,
                               boolean useBaseline, int[] ascent,
                               int[] descent) {
        //根据newAlign方式去修正
        switch (newAlign) {
            case LEFT:
                //居左  LTR: X = X ; RTL: X = X + Width;
                x += ltr ? 0 : width;
                break;
            case CENTER:
                //居中
                x += width / 2;
                break;
            case RIGHT:
                //居右 LTR: X= X + width ; RTL: X = X;
                x += ltr ? width : 0;
                break;
            case LEADING:
                //沿着容器的左端
                break;
            case TRAILING:
                //沿着容器的后端
                x += width;
                break;
        }
        //上偏移量
        int maxAscent = 0;
        //非基准线高度
        int nonbaselineHeight = 0;
        //基准线偏移量
        int baselineOffset = 0;
        //使用了基准线的情况
        if (useBaseline) {
            int maxDescent = 0;
            for (int i = rowStart; i < rowEnd; i++) {
                //遍历一行的组件
                Component m = target.getComponent(i);
                //可视的情况下
                if (m.visible) {
                    if (ascent[i] >= 0) {
                        //记录上下偏移量
                        maxAscent = Math.max(maxAscent, ascent[i]);
                        maxDescent = Math.max(maxDescent, descent[i]);
                    } else {
                        //高度修正
                        nonbaselineHeight = Math.max(m.getHeight(),
                                nonbaselineHeight);
                    }
                }
            }
            //高度修正
            height = Math.max(maxAscent + maxDescent, nonbaselineHeight);
            //基准线偏移量修正
            baselineOffset = (height - maxAscent - maxDescent) / 2;
        }
        for (int i = rowStart; i < rowEnd; i++) {
            //遍历一行的组件
            Component m = target.getComponent(i);
            //可视的情况下
            if (m.isVisible()) {
                //修正过后的y坐标值
                int cy;
                if (useBaseline && ascent[i] >= 0) {
                    //如果使用了基准线并且对应的向上偏移量非负数
                    cy = y + baselineOffset + maxAscent - ascent[i];
                } else {
                    cy = y + (height - m.height) / 2;
                }
                //对当前的布局模式进行判断并设置组件的位置
                if (ltr) {
                    m.setLocation(x, cy);
                } else {
                    m.setLocation(target.width - x - m.width, cy);
                }
                //设置完以后x坐标向右偏移
                x += m.width + hgap;
            }
        }
        return height;
    }

    /**
     * Lays out the container. This method lets each
     * <i>visible</i> component take
     * its preferred size by reshaping the components in the
     * target container in order to satisfy the alignment of
     * this <code>FlowLayout</code> object.
     *
     * 这个方法让每个可见的组件通过矫正目标容器中的组件来获得其优先尺寸
     * @param target the specified component being laid out
     * @see Container
     * @see java.awt.Container#doLayout
     */
    public void layoutContainer(Container target) {
        //获取锁对象
        synchronized (target.getTreeLock()) {
            //获取内间距对象
            Insets insets = target.getInsets();
            //计算组件的真实宽度(除去左右内间距和间隙)
            int maxwidth = target.width - (insets.left + insets.right + hgap * 2);
            //获取组件数量
            int nmembers = target.getComponentCount();
            //初始化坐标
            int x = 0, y = insets.top + vgap;
            int rowh = 0, start = 0;

            //获取组件的方向是否是LTR
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            //基准线
            boolean useBaseline = getAlignOnBaseline();
            //上偏移量
            int[] ascent = null;
            //下偏移量
            int[] descent = null;

            //如果使用了基准线 那么就需要根据上下偏移量去修正位置
            if (useBaseline) {
                ascent = new int[nmembers];
                descent = new int[nmembers];
            }

            for (int i = 0; i < nmembers; i++) {
                //遍历容器中的组件
                Component m = target.getComponent(i);
                //可视化
                if (m.isVisible()) {
                    //获取尺寸
                    Dimension d = m.getPreferredSize();
                    //设置宽高
                    m.setSize(d.width, d.height);
                    //基准线模式
                    if (useBaseline) {
                        //获取基准线
                        int baseline = m.getBaseline(d.width, d.height);
                        //非0情况
                        if (baseline >= 0) {
                            ascent[i] = baseline;
                            descent[i] = d.height - baseline;
                        } else {
                            //基准线为负数
                            ascent[i] = -1;
                        }
                    }
                    //单行之间的水平坐标计算
                    if ((x == 0) || ((x + d.width) <= maxwidth)) {
                        if (x > 0) {
                            //x>0说明当前组件不是第一个组件需要加上组件之间的水平间隙
                            x += hgap;
                        }
                        //坐标向右偏移
                        x += d.width;
                        //水平坐标修正
                        rowh = Math.max(rowh, d.height);
                    } else {
                        //换行
                        rowh = moveComponents(target, insets.left + hgap, y,
                                maxwidth - x, rowh, start, i, ltr,
                                useBaseline, ascent, descent);
                        x = d.width;
                        y += vgap + rowh;
                        rowh = d.height;
                        start = i;
                    }
                }
            }
            //组件的真正摆放的是在这个方法中去处理的
            moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh,
                    start, nmembers, ltr, useBaseline, ascent, descent);
        }
    }

    //
    // the internal serial version which says which version was written
    // - 0 (default) for versions before the Java 2 platform, v1.2
    // - 1 for version >= Java 2 platform v1.2, which includes "newAlign" field
    //
    private static final int currentSerialVersion = 1;
    /**
     * This represent the <code>currentSerialVersion</code>
     * which is bein used.  It will be one of two values :
     * <code>0</code> versions before Java 2 platform v1.2..
     * <code>1</code> versions after  Java 2 platform v1.2..
     *
     * @serial
     * @since 1.2
     */
    private int serialVersionOnStream = currentSerialVersion;

    /**
     * Reads this object out of a serialization stream, handling
     * objects written by older versions of the class that didn't contain all
     * of the fields we use now..
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        if (serialVersionOnStream < 1) {
            // "newAlign" field wasn't present, so use the old "align" field.
            setAlignment(this.align);
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Returns a string representation of this <code>FlowLayout</code>
     * object and its values.
     *
     * @return a string representation of this layout
     */
    public String toString() {
        String str = "";
        switch (align) {
            case LEFT:
                str = ",align=left";
                break;
            case CENTER:
                str = ",align=center";
                break;
            case RIGHT:
                str = ",align=right";
                break;
            case LEADING:
                str = ",align=leading";
                break;
            case TRAILING:
                str = ",align=trailing";
                break;
        }
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + str + "]";
    }


}
