/*
 * Copyright (c) 1995, 2005, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the interface for classes that know how to lay out
 * <code>Container</code>s.
 * <p>
 * Swing's painting architecture assumes the children of a
 * <code>JComponent</code> do not overlap.  If a
 * <code>JComponent</code>'s <code>LayoutManager</code> allows
 * children to overlap, the <code>JComponent</code> must override
 * <code>isOptimizedDrawingEnabled</code> to return false.
 *
 * @see Container
 * @see javax.swing.JComponent#isOptimizedDrawingEnabled
 *
 * @author      Sami Shaio
 * @author      Arthur van Hoff
 */
public interface LayoutManager {
    /**
     * If the layout manager uses a per-component string,
     * adds the component <code>comp</code> to the layout,
     * associating it
     * with the string specified by <code>name</code>.
     * 如果布局管理器对每个组件使用字符串表示,即将组件<code>comp</code>添加到布局,
     * 与之关联的是一个特定的字符串<code>name</code>.
     *
     * @param name the string to be associated with the component
     *             与组件关联的字符串
     * @param comp the component to be added
     *             被添加的组件
     */
    void addLayoutComponent(String name, Component comp);

    /**
     * Removes the specified component from the layout.
     * 从布局中移除组件
     * @param comp the component to be removed 被移除的组件
     */
    void removeLayoutComponent(Component comp);

    /**
     * Calculates the preferred size dimensions for the specified
     * container, given the components it contains.
     *
     * 根据它包含的组件,计算指定容器的首选大小尺寸
     *
     * @param parent the container to be laid out 父容器
     *
     * @see #minimumLayoutSize
     */
    Dimension preferredLayoutSize(Container parent);

    /**
     * Calculates the minimum size dimensions for the specified
     * container, given the components it contains.
     *
     * 根据它包含的组件,计算指定容器的最小大小尺寸
     * @param parent the component to be laid out
     * @see #preferredLayoutSize
     */
    Dimension minimumLayoutSize(Container parent);

    /**
     * Lays out the specified container.
     * 布置指定容器
     * @param parent the container to be laid out
     */
    void layoutContainer(Container parent);
}
