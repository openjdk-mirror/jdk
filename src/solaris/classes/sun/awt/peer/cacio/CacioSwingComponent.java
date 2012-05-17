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

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

interface CacioSwingComponent {

    /**
     * Returns the actual swing component.
     *
     * @return the actual swing component
     */
    JComponent getJComponent();

    /**
     * Handles a mouse event. This is usually forwarded to
     * {@link Component#processMouseMotionEvent(MouseEvent)} of the swing
     * component.
     *
     * @param ev the mouse event
     */
    void handleMouseEvent(MouseEvent ev);

    /**
     * Handles a mouse motion event. This is usually forwarded to
     * {@link Component#processMouseEvent(MouseEvent)} of the swing
     * component.
     *
     * @param ev the mouse motion event
     */
    void handleMouseMotionEvent(MouseEvent ev);

    /**
     * Handles a key event. This is usually forwarded to
     * {@link Component#processKeyEvent(KeyEvent)} of the swing
     * component.
     *
     * @param ev the key event
     */
    void handleKeyEvent(KeyEvent ev);
    
    /**
     * Handles a focus event. This is usually forwarded to
     * {@link Component#processFocusEvent(FocusEvent)} of the swing
     * component.
     *
     * @param ev the focus event
     */
    void handleFocusEvent(FocusEvent ev);

}
