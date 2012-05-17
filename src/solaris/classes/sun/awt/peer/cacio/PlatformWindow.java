/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.GraphicsConfiguration;
import java.awt.image.ColorModel;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;

import sun.awt.CausedFocusEvent.Cause;
import sun.java2d.pipe.Region;

public interface PlatformWindow {

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

    /**
     * Disposes the underlying platform window and releases all associated
     * resources.
     */
    void dispose();

    /**
     * Returns a Graphics2D object for drawing on this window.
     *
     * @param foreground the foreground to be set
     * @param background the background to be set
     * @param font the font to be set
     *
     * @return  a Graphics2D object for drawing on this window
     */
    Graphics2D getGraphics(Color foreground, Color background, Font font);

    /**
     * Sets the bounds of this native window. The X and Y coordinates are
     * relative to the parent window (or the screen for toplevel windows).
     * The <code>op</code> parameter specifies the actual operation of this
     * methods, according to the various constants in {@link ComponentPeer}.
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @param op
     *
     * @see ComponentPeer#setBounds(int, int, int, int, int)
     */
    void setBounds(int x, int y, int width, int height, int op);

    /**
     * Returns the insets of this native window. Undecorated windows usually
     * don't have insets. For decorated windows, the insets are the border
     * widths of the window decoration. The PlatformWindow implementation is
     * encouraged to cache the insets.
     *
     * @return the insets of this native window
     */
    Insets getInsets();

    /**
     * Returns the location of the native window on the screen. For
     * decorated windows this must include the window decorations.
     *
     * @return the location of the native window on the screen
     */
    Point getLocationOnScreen();

    /**
     * Returns <code>true</code> if the platform window implementation can
     * determine if the window has been obscured, <code>false</code>
     * otherwise.
     * 
     * @return <code>true</code> if the platform window implementation can
     *         determine if the window has been obscured, <code>false</code>
     *         otherwise
     *
     * @see #isObscured()
     */
    boolean canDetermineObscurity();

    /**
     * Returns <code>true</code> if the platform window implementation
     * determines that it has been obscured, <code>false</code> otherwise.
     * This is only called when {@link #canDetermineObscurity()} returns
     * <code>true</code>.
     *
     * @return <code>true</code> if the platform window implementation
     *         determines that it has been obscured, <code>false</code>
     *         otherwise
     *
     * @see #canDetermineObscurity()
     */
    boolean isObscured();

    /**
     * Applies the specified shape to the native component window.
     * 
     * @param shape the shape to apply to the window
     */
    void applyShape(Region shape);

    /**
     * Returns <code>true</code> if reparenting of native windows is supported,
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if reparenting of native windows is supported,
     *         <code>false</code> otherwise
     *
     * @see #reparent(ContainerPeer)
     */
    boolean isReparentSuppored();

    /**
     * Reparents this native window to become a child of the specified
     * container. This is only called when {@link #isReparentSuppored()}
     * returns <code>true</code>.
     *
     * @param newContainer the new container window of this native window
     *
     * @see #isReparentSuppored()
     */
    void reparent(ContainerPeer newContainer);

    /**
     * Returns <code>true</code> if the native window implementation supports
     * restacking of the window hierarchy, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the native window implementation supports
     *         restacking of the window hierarchy, <code>false</code> otherwise
     */
    boolean isRestackSupported();

    /**
     * Updates the stacking order of the window hierarchy. This is only
     * called when {@link #isRestackSupported()} returns <code>true</code>.
     */
    void restack();

    /**
     * Shows or hides this native window.
     *
     * @param v <code>true</code> shows this native window
     *        <code>false</code> hides it.
     */
    void setVisible(boolean b);

    /**
     * Requests a focus change to this window. If the focus change was
     * successful it is necessary to send back the appropriate FocusEvent.
     *
     * @param lightweightChild the actual lightweight child that wants focus
     * @param temporary <code>true</code> if the focus change should be
     *        temporary, <code>false</code> otherwise
     * @param focusedWindowChangeAllowed <code>true</code> when changing the
     *        focus of the corresponding toplevel window is allowed,
     *        <code>false</code> otherwise
     * @param time the time of the focus change request
     * @param cause the cause of the focus change request
     *
     * @return <code>true</code> when the focus transfer is guaranteed to be
     *         successful, <code>false</code> otherwise
     */
    boolean requestFocus(Component lightweightChild, boolean temporary,
                         boolean focusedWindowChangeAllowed, long time,
                         Cause cause);


    /**
     * Creates back buffers for flipping. This method may throw an AWTException
     * to indicate that flipping is not supported, in which case a blitting
     * buffer strategy is used by AWT.
     *
     * @param numBuffers the number of buffers to create for flipping
     * @param caps the required buffer capabilities
     *
     * @throws AWTException if flipping is not supported
     */
    void createBuffers(int numBuffers, BufferCapabilities caps)
        throws AWTException;

    /**
     * Destroys the flipping buffers. If flipping is not supported (i.e.
     * createBuffers() throws an AWTException), this method should do nothing.
     *
     * @see #createBuffers(int, BufferCapabilities)
     */
    void destroyBuffers();

    /**
     * Flips the (current) backbuffer to the front buffer.
     * If flipping is not supported (i.e. createBuffers() throws an
     * AWTException), this method should do nothing.
     *
     * @param x1 the area to flip, left X coordinate
     * @param y1 the area to flip, top Y coordinate
     * @param x2 the area to flip, right X coordinate
     * @param y2 the area to flip, bottom Y coordinate
     * @param flipAction the requested flip action
     */
    void flip(int x1, int y1, int x2, int y2, FlipContents flipAction);

    /**
     * Provides access to the (current) backbuffer as an image.
     * If flipping is not supported (i.e. createBuffers() throws an
     * AWTException), this method should do nothing.
     *
     * @return the current backbuffer, as an image
     */
    Image getBackBuffer();

    /**
     * Request focus on the window. When the focus has been granted by the
     * platform windowing implementation, the platform window needs to send
     * an appropriate FOCUS_GAINED event. It is not necessary to send
     * FOCUS_LOST events though.
     */
    void requestFocus();

}
