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

package sun.awt.peer.cacio.managed;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.util.List;

public interface PlatformScreen {

    /**
     * Returns the color model used by the native window.
     *
     * @return the color model used by the native window
     */
    ColorModel getColorModel();

    /**
     * Returns the graphics configuration used by the native window.
     *
     * @return the graphics configuration used by the native window
     */
    GraphicsConfiguration getGraphicsConfiguration();


    /**
     * Returns the bounds of the native window. The resulting rectangle
     * has the X and Y coordinates of the window relative to its parent and
     * the width and height of the window. For decorated windows the bounds
     * must include the window decorations.
     *
     * @return the bounds of the native window
     */
    Rectangle getBounds();

    abstract Graphics2D getClippedGraphics(Color fg, Color bg, Font f,
                                           List<Rectangle> clipRects);

}
