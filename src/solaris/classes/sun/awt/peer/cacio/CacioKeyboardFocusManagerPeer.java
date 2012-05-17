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
import java.awt.Component;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.peer.KeyboardFocusManagerPeer;
import sun.awt.AppContext;
import sun.awt.CausedFocusEvent;
import sun.awt.CausedFocusEvent.Cause;
import sun.awt.SunToolkit;

class CacioKeyboardFocusManagerPeer implements KeyboardFocusManagerPeer {

    private static CacioKeyboardFocusManagerPeer instance;

    static CacioKeyboardFocusManagerPeer getInstance() {
        if (instance == null) {
            instance = new CacioKeyboardFocusManagerPeer();
        }
        return instance;
    }

    private CacioKeyboardFocusManagerPeer() {
    }

    private Window currentFocusedWindow;
    private Component currentFocusOwner;

    /**
     * Returns the currently focused window.
     *
     * @return the currently focused window
     *
     * @see KeyboardFocusManager#getNativeFocusedWindow()
     */
    @Override
    public Window getCurrentFocusedWindow() {
        return currentFocusedWindow;
    }

    /**
     * Sets the component that should become the focus owner.
     *
     * @param comp the component to become the focus owner
     *
     * @see KeyboardFocusManager#setNativeFocusOwner(Component)
     */
    @Override
    public void setCurrentFocusOwner(Component comp) {
        currentFocusOwner = comp;
    }

    /**
     * Returns the component that currently owns the input focus.
     *
     * @return the component that currently owns the input focus
     *
     * @see KeyboardFocusManager#getNativeFocusOwner()
     */
    @Override
    public Component getCurrentFocusOwner() {
        return currentFocusOwner;
    }

    /**
     * Clears the current global focus owner.
     *
     * @param activeWindow
     *
     * @see KeyboardFocusManager#clearGlobalFocusOwner()
     */
    @Override
    public void clearGlobalFocusOwner(Window activeWindow) {
        // TODO: Implement.
    }

    void setCurrentFocusedWindow(Window w) {
        currentFocusedWindow = w;
    }

    boolean requestFocus(Component target, Component lightweightChild, boolean temporary,
                         boolean focusedWindowChangeAllowed, long time, Cause cause) {

        if (lightweightChild == null) {
            lightweightChild = target;
        }
        Component currentOwner = getCurrentFocusOwner();
        if (currentOwner != null && currentOwner.getPeer() == null) {
            currentOwner = null;
        }
        FocusEvent fg = new CausedFocusEvent(lightweightChild, FocusEvent.FOCUS_GAINED, false, currentOwner, cause);
        FocusEvent fl = null;
        if (currentOwner != null) {
            fl = new CausedFocusEvent(currentOwner, FocusEvent.FOCUS_LOST, false, lightweightChild, cause);
        }

        if (fl != null) {
            postEvent(fl);
        }
        postEvent(fg);
        return true;

    }

    private void postEvent(AWTEvent ev) {
        SunToolkit.postEvent(AppContext.getAppContext(), ev);
    }

}
