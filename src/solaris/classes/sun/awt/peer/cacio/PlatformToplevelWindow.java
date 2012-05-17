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

import java.awt.Frame;
import java.awt.Rectangle;

public interface PlatformToplevelWindow extends PlatformWindow {

    /**
     * Returns the current state of the native window according to the
     * constants in {@link java.awt.Frame}. The state is a bitmask, ORed
     * together by these constants. This is only called for toplevel frames.
     *
     * @return the current state of the native window according to the
     *         constants in java.awt.Frame
     *
     * @see Frame#getExtendedState()
     * @see Frame#NORMAL
     * @see Frame#ICONIFIED
     * @see Frame#MAXIMIZED_HORIZ
     * @see Frame#MAXIMIZED_VERT
     * @see Frame#MAXIMIZED_BOTH
     * @see #setState(int)
     */
    int getState();

    /**
     * Sets the state of the native window according to the various constants
     * in {@link java.awt.Frame}. The state is a bitmask ORed together by
     * these constants. This is only called for toplevel frames.
     *
     * @param state the new state of the window
     *
     * @see Frame#setExtendedState()
     * @see Frame#NORMAL
     * @see Frame#ICONIFIED
     * @see Frame#MAXIMIZED_HORIZ
     * @see Frame#MAXIMIZED_VERT
     * @see Frame#MAXIMIZED_BOTH
     * @see #getState(int)
     */
    void setState(int state);

    /**
     * Sets the bounds for this native window that it should take when it
     * becomes maximized. This is only called for toplevel frames.
     *
     * @param bounds the maximized bounds to set
     */
    void setMaximizedBounds(Rectangle bounds);

    /**
     * Sets if the native window should be resizable (by the user) or not.
     * This is only called for toplevel frames and dialogs.
     *
     * @param resizable <code>true</code> when the native window should be
     *        resizable, <code>false</code> otherwise
     */
    void setResizable(boolean resizable);

    /**
     * Sets the title of the native window. This is only called for toplevel
     * frames and dialogs.
     *
     * @param title the title to set
     */
    void setTitle(String title);

    /**
     * Blocks or unblocks the native window.
     * This is only called for toplevel frames and dialogs.
     *
     * @param blocked true if set to blocked
     *
     * @see DialogPeer#blockWindows(List<Window>)
     * @see WindowPeer#setModalBlocked(Dialog, boolean)
     */
    void setBlocked(boolean blocked);

}
