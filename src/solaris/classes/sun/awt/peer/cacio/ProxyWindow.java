/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.awt.peer.cacio;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JComponent;

/**
 * A specialized Window implementation with the sole purpose of providing
 * the Swing components that back up the AWT widgets a heavyweight parent.
 * From the point of view of the Swing component, this window acts exactly
 * like a usual heavyweight window. But it is infact just a proxy to
 * the AWT widget. This behaviour is achieved by overriding certain methods,
 * and by a special WindowPeer implementation in {@link ProxyWindow}.
 */
public class ProxyWindow extends Window {

    private CacioComponentPeer target;

    ProxyWindow(CacioComponentPeer t, JComponent c) {
        super(null);
        target = t;
        add(c);
    }

    CacioComponentPeer getTargetPeer() {
        return target;
    }

    void handleFocusEvent(FocusEvent e) {
        // TODO: Retarget?
        processFocusEvent(e);
    }

    void handleKeyEvent(KeyEvent e) {
        // TODO: Retarget?
        processKeyEvent(e);
    }

    void handleMouseEvent(MouseEvent e) {
        MouseEvent me = new MouseEvent(this, e.getID(), e.getWhen(),
                                       e.getModifiers(), e.getX(), e.getY(),
                                       e.getXOnScreen(), e.getYOnScreen(),
                                       e.getClickCount(), e.isPopupTrigger(),
                                       e.getButton());
        // IMPORTANT: See comment on the helper method!
        doLightweightDispatching(e);
    }

    void handleMouseMotionEvent(MouseEvent e) {
        MouseEvent me = new MouseEvent(this, e.getID(), e.getWhen(),
                                       e.getModifiers(), e.getX(), e.getY(),
                                       e.getXOnScreen(), e.getYOnScreen(),
                                       e.getClickCount(), e.isPopupTrigger(),
                                       e.getButton());
        // IMPORTANT: See comment on the helper method!
        doLightweightDispatching(e);
    }

    private static Field dispatcherField;
    private static Method dispatchMethod;

    private static void initReflection() {
        try {
            dispatcherField = Container.class.getDeclaredField("dispatcher");
            dispatcherField.setAccessible(true);
            Class dispatcherCls = Class.forName("java.awt.LightweightDispatcher");
            dispatchMethod = dispatcherCls.getDeclaredMethod("dispatchEvent", AWTEvent.class);
            dispatchMethod.setAccessible(true);
        } catch (Exception ex) {
            InternalError err = new InternalError();
            err.initCause(ex);
            throw err;
        }
    }

    /**
     * Performs lightweight dispatching for the specified event on this window.
     * This only calls the lightweight dispatcher. We cannot simply
     * call dispatchEvent() because that would also send the event to the
     * Toolkit dispatching mechanism (AWTEventListener, etc), which has ugly
     * side effects, like popups closing too early.
     *
     * @param e the event to be dispatched
     */
    private void doLightweightDispatching(AWTEvent e) {
        if (dispatcherField == null) {
            initReflection();
        }
        try {
            Object dispatcher = dispatcherField.get(this);
            if (dispatcher != null) {
                dispatchMethod.invoke(dispatcher, e);
            }
        } catch (Exception ex) {
            InternalError err = new InternalError();
            err.initCause(ex);
            throw err;
        }

    }

    /**
     * We need to override isShowing() and proxy it to the real AWT component,
     * otherwise we end up and try to draw on non-showing components...
     */
    public boolean isShowing() {
        return target.getAWTComponent().isShowing();
    }
}
