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
package java.awt.peer;

import java.awt.*;

/**
 * The peer interface for {@link Container}. This is the parent interface
 * for all container like widgets.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface ContainerPeer extends ComponentPeer {

    /**
     * Returns the insets of this container. Insets usually is the space that
     * is occupied by things like borders.
     *
     * 返回容器的插图对象,插图通常是被边界占领的空间。
     *
     * @return the insets of this container
     */
    Insets getInsets();

    /**
     * Notifies the peer that validation of the component tree is about to
     * begin.
     *
     * 通知对等体组件树的验证即将开始
     *
     * @see Container#validate()
     */
    void beginValidate();

    /**
     * Notifies the peer that validation of the component tree is finished.
     *
     * 通知对等体组件树的验证已经完成
     * @see Container#validate()
     */
    void endValidate();

    /**
     * Notifies the peer that layout is about to begin. This is called
     * before the container itself and its children are laid out.
     *
     * 通知对等体布局即将开始,这个方法在容器本身和它的孩子被放置之前调用
     *
     * @see Container#validateTree()
     */
    void beginLayout();

    /**
     * Notifies the peer that layout is finished. This is called after the
     * container and its children have been laid out.
     *
     * 通知对等体布局已经完成,这个方法在容器本身和它的孩子已经被放置完成之后调用
     * @see Container#validateTree()
     */
    void endLayout();
}
