/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.Window;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import sun.awt.AWTAutoShutdown;

import sun.awt.PaintEventDispatcher;

/**
 * A thread that polls the native event queue for events and posts
 * them to the AWT event queue.
 */
public abstract class CacioEventPump<ET> implements Runnable {

    private static final int BUTTON_DOWN_MASK =
        MouseEvent.BUTTON1_DOWN_MASK
        | MouseEvent.BUTTON2_DOWN_MASK
        | MouseEvent.BUTTON3_DOWN_MASK;

    /**
     * The last state of the modifier mask for mouse events. Used to compute
     * the changed button mask.
     */
    private int lastModifierState;

    /**
     * Creates and starts a CacioEventPump with the specified
     * event source.
     *
     * @param s the event source to get events from
     */
    protected CacioEventPump() {
        // Nothing to do here.
    }

    protected void start() {
        Thread t = new Thread(this, "CacioEventPump");
        t.setDaemon(true);
        t.start();
    }

    /**
     * The main loop of the event pump.
     */
    public final void run() {

        AWTAutoShutdown.notifyToolkitThreadBusy();
        while (true) {
            // Jump out if we get interrupted.
            if (Thread.interrupted()) {
                return;
            }
            try {
                AWTAutoShutdown.notifyToolkitThreadFree();
                ET nativeEvent = fetchNativeEvent();
                AWTAutoShutdown.notifyToolkitThreadBusy();
                dispatchNativeEvent(nativeEvent);
            } catch (Exception ex) {
                // Print stack trace but don't kill the pump.
                ex.printStackTrace();
            }
        }
    }

    /**
     * Fetches the next native event. This method is called in an AWT
     * Autoshutdown free block, that means, as long as the thread is in this
     * method, it doesn't prevent shutting down AWT. Therefore, this method
     * should only wait for the next native event and return it as soon as
     * possible. Implement {@link dispatchNativeEvent} for dispatching the
     * event.
     *
     * @return the native event
     * @throws InterruptedException 
     */
    protected abstract ET fetchNativeEvent() throws InterruptedException;

    /**
     * Dispatches the native event. This method is called in an AWT
     * Autoshutdown busy block, that means, as long as the thread is in this
     * method, it prevents shutting down AWT.
     *
     * @param nativeEvent
     */
    protected abstract void dispatchNativeEvent(ET nativeEvent);

    protected final void postMouseEvent(CacioComponent source, int id,
                                        long time, int modifiers, int x, int y,
                                        int clickCount, boolean popupTrigger) {

        int button;
        if (id == MouseEvent.MOUSE_DRAGGED) {
            button = getButton(modifiers);
        } else {
            int modifierChange = lastModifierState ^ modifiers;
            button = getButton(modifierChange);
            lastModifierState = modifiers;
        }

        MouseEvent ev = new MouseEvent(source.getAWTComponent(), id, time,
                                       modifiers, x, y, clickCount,
                                       popupTrigger, button);
        postEvent(source, ev);

    }

    private int getButton(int theModifierChange) {
        switch (theModifierChange & BUTTON_DOWN_MASK) {
        case MouseEvent.BUTTON1_DOWN_MASK :
            return MouseEvent.BUTTON1;
        case MouseEvent.BUTTON2_DOWN_MASK :
            return MouseEvent.BUTTON2;
        case MouseEvent.BUTTON3_DOWN_MASK :
            return MouseEvent.BUTTON3;
        default :
            return MouseEvent.NOBUTTON;
        }
    }

    protected final void postKeyEvent(CacioComponent source, int id, long time,
                                      int modifiers, int keyCode) {

        KeyEvent ke = new KeyEvent(source.getAWTComponent(), id, time,
                                   modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
        postEvent(source, ke);

    }

    protected final void postKeyTypedEvent(CacioComponent source, int id,
                                           long time, int modifiers,
                                           char keyChar) {

        KeyEvent ke = new KeyEvent(source.getAWTComponent(), id, time,
                                   modifiers, KeyEvent.VK_UNDEFINED, keyChar);
        postEvent(source, ke);

    }

    protected final void postComponentEvent(CacioComponent source, int id) {

        ComponentEvent ev = new ComponentEvent(source.getAWTComponent(), id);
        postEvent(source, ev);

    }
    
    protected final void postPaintEvent(CacioComponent source, int x, int y,
                                        int width, int height, boolean paintBackground) {
        if (paintBackground)
        {
          ((CacioComponentPeer) source).clearBackground();
        }
        
        PaintEvent ev = PaintEventDispatcher.getPaintEventDispatcher()
              .createPaintEvent(source.getAWTComponent(), x, y, width, height);
        postEvent(source, ev);

    }
    
    protected final void postPaintEvent(CacioComponent source, int x, int y,
                                        int width, int height) {
        postPaintEvent(source, x, y, width, height, false);
    }
    
    protected final void postFocusEvent(CacioComponent source, int id,
                                        boolean temporary,
                                        CacioComponent opposite) {
        Component awtComponent = null;
        if (opposite != null) {
            awtComponent = opposite.getAWTComponent();
        }

        FocusEvent ev = new FocusEvent(source.getAWTComponent(), id, temporary,
                                       awtComponent);
        postEvent(source, ev);
    }

    protected final void postWindwoEvent(CacioComponent source, int id,
                                         int oldState, int newState) {

        WindowEvent ev;
        if (id == WindowEvent.WINDOW_STATE_CHANGED) {
            ev = new WindowEvent((Window) (source.getAWTComponent()), id,
                                 oldState, newState);
        } else {
            ev = new WindowEvent((Window) (source.getAWTComponent()), id);
        }
        postEvent(source, ev);

    }

    private void postEvent(CacioComponent c, AWTEvent ev) {
        c.handlePeerEvent(ev);
    }
}
