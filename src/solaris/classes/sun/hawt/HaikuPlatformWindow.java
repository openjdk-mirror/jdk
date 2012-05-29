/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.hawt;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.peer.*;

import sun.awt.*;
import sun.lwawt.*;
import sun.lwawt.LWWindowPeer.PeerType;

import sun.java2d.*;

class HaikuPlatformWindow implements PlatformWindow {

    static {
        initIDs();
    }

    private Window target;
    private LWWindowPeer peer;
    private HaikuPlatformWindow owner;

    private long nativeWindow;
    private HaikuWindowSurfaceData surfaceData;

    private PeerType peerType;

    private static native void initIDs();
    private native long nativeInitView(int x, int y, int width, int height,
        long parent);
    private native long nativeInitFrame(int x, int y, int width, int height,
        boolean decorated);
    private native long nativeGetDrawable(long nativeWindow);
    private native void nativeGetBounds(long nativeWindow, Rectangle bounds);
    private native void nativeSetBounds(long nativeWindow, int x, int y,
        int width, int height);
    private native boolean nativeGetVisible(long nativeWindow);
    private native void nativeSetVisible(long nativeWindow, boolean visible);
    private native void nativeGetLocation(long nativeWindow, Point location);
    private native void nativeGetLocationOnScreen(long nativeWindow,
        Point location);
    private native void nativeDispose(long nativeWindow);
    private native void nativeFocus(long nativeWindow);

    public HaikuPlatformWindow(final PeerType peerType) {
        this.peerType = peerType;
    }

    /*
     * Delegate initialization (create native window and all the
     * related resources).
     */
    @Override
    public void initialize(Window target, LWWindowPeer peer, PlatformWindow owner) {
        this.peer = peer;
        this.target = target;
        if (owner instanceof HaikuPlatformWindow) {
            this.owner = (HaikuPlatformWindow)owner;
        }

        nativeWindow = nativeInitFrame(0, 0, 0, 0, peerType != PeerType.SIMPLEWINDOW);
    }

    @Override
    public void dispose() {
        nativeDispose(nativeWindow);
    }

    @Override
    public void setVisible(boolean visible) {
        nativeSetVisible(nativeWindow, visible);
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        nativeSetBounds(nativeWindow, x, y, width, height);
    }

    @Override
    public int getScreenImOn() {
        return 0;
    }

    @Override
    public Point getLocationOnScreen() {
        Point location = new Point();
        nativeGetLocationOnScreen(nativeWindow, location);
        return location;
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        (new RuntimeException("unimplemented")).printStackTrace();
        return null;
    }

    @Override
    public SurfaceData getScreenSurface() {
        if (surfaceData == null) {
            long drawable = nativeGetDrawable(nativeWindow);
            surfaceData = new HaikuWindowSurfaceData(
                HaikuWindowSurfaceData.typeDefault, getColorModel(),
                getBounds(), getGraphicsConfiguration(), this, drawable);
        }
        return surfaceData;
    }

    @Override
    public SurfaceData replaceSurfaceData() {
        return getScreenSurface();
    }
    
    @Override
    public Image createBackBuffer() {
        return null;
    }

    @Override
    public void flip(int x1, int y1, int x2, int y2,
            BufferCapabilities.FlipContents flipAction) {
        (new RuntimeException("unimplemented")).printStackTrace();
    }
    
    @Override
    public void toFront() {
    }
    
    @Override
    public void toBack() {
    }

    @Override
    public void setMenuBar(MenuBar mb) {
        System.err.println("todo");
    }

    @Override
    public void setAlwaysOnTop(boolean value) {
    }
    
    @Override
    public void updateFocusableWindowState() {
    }

    @Override
    public void setResizable(boolean resizable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setMinimumSize(int width, int height) {
    }

    @Override
    public Graphics transformGraphics(Graphics g) {
        return g;
    }

    @Override
    public void updateIconImages() {
    }

    @Override
    public void setOpacity(float opacity) {
    }

    @Override
    public void setOpaque(boolean isOpaque) {
    }

    @Override
    public void enterFullScreenMode() {
    }

    @Override
    public void exitFullScreenMode() {
    }

    @Override
    public void setWindowState(int windowState) {
    }

    @Override
    public long getLayerPtr() {
        return nativeWindow;
    }

    @Override
    public LWWindowPeer getPeer() {
        return peer;
    }
    
    @Override
    public boolean requestWindowFocus() {
        nativeFocus(nativeWindow);
        return false;
    }    
    
    @Override
    public boolean isActive() {
        return false;
    }

    public ColorModel getColorModel() {
        return getGraphicsConfiguration().getColorModel();
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        return HaikuGraphicsConfig.getDefaultConfiguration();
    }

    public Rectangle getBounds() {
        Rectangle bounds = new Rectangle();
        nativeGetBounds(nativeWindow, bounds);
        return bounds;
    }

    // =====================
    // Native code callbacks
    // =====================

    public void eventRepaint(int x, int y, int width, int height) {
        peer.notifyExpose(x, y, width, height);
    }

    public void eventResize(int width, int height) {
        Rectangle bounds = peer.getBounds();
        peer.notifyReshape(bounds.x, bounds.y, width, height);
    }
    
    public void eventMove(int x, int y) {
        Rectangle bounds = peer.getBounds();
        peer.notifyReshape(x, y, bounds.width, bounds.height);
    }
    
    public void eventMaximize(boolean maximize) {
        peer.notifyZoom(maximize);
    }
    
    public void eventMinimize(boolean minimize) {
        peer.notifyIconify(minimize);
    }
    
    public void eventWindowClosing() {
        if (peer.getBlocker() == null)  {
            peer.postEvent(new WindowEvent(target, WindowEvent.WINDOW_CLOSING));
        }
    }

    public void eventFocus(boolean focused) {
        peer.notifyActivation(focused);
    }

    private void handleMouseDown(long when, int modifiers, int x, int y,
            int screenX, int screenY, int clicks, int button) {
        boolean popup = button == MouseEvent.BUTTON2 ||
            (button == MouseEvent.BUTTON1 &&
            (modifiers & MouseEvent.CTRL_DOWN_MASK) != 0);

        peer.dispatchMouseEvent(MouseEvent.MOUSE_PRESSED, when, button,
            x, y, screenX, screenY, modifiers, clicks, popup, null);
    }

    private void handleMouseUp(long when, int modifiers, int x, int y,
            int screenX, int screenY, int clicks, int button) {
        peer.dispatchMouseEvent(MouseEvent.MOUSE_RELEASED, when, button,
            x, y, screenX, screenY, modifiers, clicks, false, null);
        peer.dispatchMouseEvent(MouseEvent.MOUSE_CLICKED, when, button,
            x, y, screenX, screenY, modifiers, clicks, false, null);
    }

    private void handleMouseDrag(long when, int modifiers, int x, int y,
            int screenX, int screenY, int clicks, int button) {
        peer.dispatchMouseEvent(MouseEvent.MOUSE_DRAGGED, when, button,
            x, y, screenX, screenY, modifiers, clicks, false, null);
    }

    public void eventMouse(int id, long when, int modifiers, int x, int y,
            int screenX, int screenY, int clicks, int pressed, int released,
            int buttons) {
        // Mouse up/down is weird on Haiku so we check what buttons
        // exactly have changed with every mouse message and then
        // fire off the appropriate events.
        if ((pressed & MouseEvent.BUTTON1_DOWN_MASK) != 0)
            handleMouseDown(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON1);
        if ((pressed & MouseEvent.BUTTON2_DOWN_MASK) != 0)
            handleMouseDown(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON2);
        if ((pressed & MouseEvent.BUTTON3_DOWN_MASK) != 0)
            handleMouseDown(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON3);
        if ((released & MouseEvent.BUTTON1_DOWN_MASK) != 0)
            handleMouseUp(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON1);
        if ((released & MouseEvent.BUTTON2_DOWN_MASK) != 0)
            handleMouseUp(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON2);
        if ((released & MouseEvent.BUTTON3_DOWN_MASK) != 0)
            handleMouseUp(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON3);

        if (id == MouseEvent.MOUSE_DRAGGED) {
            if ((buttons & MouseEvent.BUTTON1_DOWN_MASK) != 0)
                handleMouseDrag(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON1);
            if ((buttons & MouseEvent.BUTTON2_DOWN_MASK) != 0)
                handleMouseDrag(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON2);
            if ((buttons & MouseEvent.BUTTON3_DOWN_MASK) != 0)
                handleMouseDrag(when, modifiers, x, y, screenX, screenY, clicks, MouseEvent.BUTTON3);
        } else if (id != MouseEvent.MOUSE_PRESSED && id != MouseEvent.MOUSE_RELEASED)
            peer.dispatchMouseEvent(id, when, 0, x, y, screenX, screenY,
                modifiers, clicks, false, null);
    }
}
