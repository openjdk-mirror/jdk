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

package sun.awt.peer.cacio.managed;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.util.Deque;
import java.util.List;

/**
 * A container for Caciocavallo managed windows. Each managed window has to
 * live in a container that provides the necessary infrastructure for the
 * managed window. This container can either be another managed window
 * (windows can be nested), or a top level container, which is usually
 * implemented by client code.
 */
public interface ManagedWindowContainer {

    /**
     * Adds a child window to this container. This will be the topmost
     * window in the stack.
     *
     * @param child the window to add
     */
    void add(ManagedWindow child);

    /**
     * Removes a child window from this container.
     *
     * @param child the window to be removed
     */
    void remove(ManagedWindow child);

    Deque<ManagedWindow> getChildren();

    /**
     * Returns the location of the specified child window relative to
     * the screen (== outermost container).
     *
     * @param child the child to find the screen location of
     *
     * @return the location of the specified child on screen
     */
    Point getLocationOnScreen();

    /**
     * Processes and dispatches the incoming event. This should include
     * finding and setting the correct source window, synthesizing additional
     * required events (i.e. focus events) and eventually posting the event
     * to the AWT event queue.
     *
     * @param event the event to dispatch
     */
    void dispatchEvent(EventData event);

    /**
     * Creates and returns a Graphics2D that has the specified rectangles
     * applied as default clip. No drawing operation must ever draw
     * <em>inside</em> the specified rectangles. Note that this is
     * the inverse meaning of the normal clip shape in Graphics2D.
     * This clip must not be revertible, not even by calling
     * {@code setClip(null)} or {@code resetClip()}. Applying addition clips
     * to the returned {@code Graphics2D} object must always also apply this
     * default clip.
     *
     * The clip rectangles are in the coordinate space of this container.
     *
     * If {@code clipRects} is empty or {@code null}, no default clip is
     * to be set.
     *
     * @param clipRects the rectangles to be clipped 'away'
     *
     * @return a Graphics2D object with the specified default clips applied
     */
    Graphics2D getClippedGraphics(Color fg, Color bg, Font font,
                                  List<Rectangle> clipRects);

    /**
     * Triggers repainting of the specified area in this container. This
     * should repaint all the child windows that actually lie in this
     * region. This method does not perform any painting itself, but
     * only calculates the correct rectangles for all the children
     * and probably the parent (for overlapping windows), and sends
     * appropriate paint events.
     *
     * @param x the x location of the area to be repainted
     * @param y the y location of the area to be repainted
     * @param w the width of the area to be repainted
     * @param h the height of the area to be repainted
     */
    void repaint(int x, int y, int w, int h);

    ColorModel getColorModel();

    GraphicsConfiguration getGraphicsConfiguration();
}
