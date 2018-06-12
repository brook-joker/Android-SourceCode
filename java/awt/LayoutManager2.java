
package java.awt;

/**
 * Defines an interface for classes that know how to layout Containers
 * based on a layout constraints object.
 * 定义一个接口类去了解如何基于布局约束对象去布局容器
 *
 * This interface extends the LayoutManager interface to deal with layouts
 * explicitly in terms of constraint objects that specify how and where
 * components should be added to the layout.
 * 这个接口继承自LayoutManger接口,以便在约束对象方面明确地处理布局,这些约束对象指定了
 * 组件添加到布局的方式和位置
 *
 * This minimal extension to LayoutManager is intended for tool
 * providers who wish to the creation of constraint-based layouts.
 * It does not yet provide full, general support for custom
 * constraint-based layout managers.
 *
 * 这种对LayoutManager的最小扩展适用于希望创建基于约束的布局的工具提供者。
 * 它还没有为基于约束的自定义布局管理器提供全面的一般支持
 *
 * @see LayoutManager
 * @see Container
 *
 * @author      Jonni Kanerva
 */
public interface LayoutManager2 extends LayoutManager {

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     * 使用指定的约束类将指定的组件添加到布局中
     * @param comp the component to be added 被添加的组件
     * @param constraints  where/how the component is added to the layout.
     *                     约束组件在什么地方如何添加到布局中
     */
    void addLayoutComponent(Component comp, Object constraints);

    /**
     * Calculates the maximum size dimensions for the specified container,
     * given the components it contains.
     * 根据所包含的组件计算指定容器的最大尺寸大小
     * @see java.awt.Component#getMaximumSize
     * @see LayoutManager
     */
    public Dimension maximumLayoutSize(Container target);

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * 返回沿着X轴对齐方式. 这指定了组件如何相对于其他组件进行对齐。
     * 该值应为0到1之间的数字
     * 其中0表示原点对齐,1对齐最距离起始点最远,0.5为中心等
     */
    public float getLayoutAlignmentX(Container target);

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * 返回沿着y轴对齐方式. 这指定了组件像相对于其他组件进行对齐。
     * 该值应为0到1之间的数字
     * 其中0表示原点对齐 1表示距离起始点最远 0.5表示中心等
     */
    public float getLayoutAlignmentY(Container target);

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     * 使布局无效,需要注意的是如果布局管理器已缓存信息,则应将其丢弃掉.
     */
    public void invalidateLayout(Container target);
}
