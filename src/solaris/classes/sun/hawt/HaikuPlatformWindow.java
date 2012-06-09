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

public class HaikuPlatformWindow implements PlatformWindow {

    private Window target;
    private LWWindowPeer peer;
    private HaikuPlatformWindow owner;
    private long nativeWindow;
    private HaikuWindowSurfaceData surfaceData;
    private PeerType peerType;
    private LWWindowPeer blocker;
    private final Point location = new Point();

    private static native void initIDs();
    private native long nativeInit(boolean simpleWindow);
    private native void nativeRun(long nativeWindow);
    private native long nativeGetDrawable(long nativeWindow);
    private native void nativeSetBounds(long nativeWindow, int x, int y,
        int width, int height);
    private native void nativeSetVisible(long nativeWindow, boolean visible);
    private native void nativeGetLocation(long nativeWindow, Point location);
    private native void nativeDispose(long nativeWindow);
    private native void nativeFocus(long nativeWindow);
    private native void nativeSetWindowState(long nativeWindow,
        int windowState);
    private native void nativeSetTitle(long nativeWindow, String title);
    private native void nativeSetResizable(long nativeWindow,
        boolean resizable);
    private native void nativeToFront(long nativeWindow);
    private native void nativeToBack(long nativeWindow);
    private native void nativeSetMenuBar(long nativeWindow, long menuBarPtr);
    private native void nativeSetMinimumSize(long nativeWindow, int width,
        int height);
    private native void nativeGetInsets(long nativeWindow, Insets insets);
    private native boolean nativeIsActive(long nativeWindow);
    private native void nativeSetBlocked(long nativeWindow, long nativeBlocker,
        boolean blocked);

    private native void nativeAddDropTarget(long nativeWindow, Component target);
    private native void nativeRemoveDropTarget(long nativeWindow);

    static {
        initIDs();
    }

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
        this.owner = (HaikuPlatformWindow)owner;
        nativeWindow = nativeInit(peerType == PeerType.SIMPLEWINDOW);
        nativeRun(nativeWindow);
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
    public void dispose() {
        nativeDispose(nativeWindow);
    }

    @Override
    public void setVisible(boolean visible) {
        nativeSetVisible(nativeWindow, visible);
    }

    @Override
    public void setTitle(String title) {
        nativeSetTitle(nativeWindow, title);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        nativeSetBounds(nativeWindow, x, y, width, height);
    }

    @Override
    public Point getLocationOnScreen() {
        return (Point)location.clone();
    }

    @Override
    public Insets getInsets() {
        Insets insets = new Insets(0, 0, 0, 0);
        nativeGetInsets(nativeWindow, insets);
        return insets;
    }

    @Override
    public void toFront() {
        nativeToFront(nativeWindow);
    }
    
    @Override
    public void toBack() {
        nativeToBack(nativeWindow);
    }

    @Override
    public void setMenuBar(MenuBar menuBar) {
        HaikuMenuBar peer = (HaikuMenuBar)LWToolkit.targetToPeer(menuBar);
        if (peer != null) {
            nativeSetMenuBar(nativeWindow, peer.getModel());
        } else {
            nativeSetMenuBar(nativeWindow, 0);
        }
    }

    @Override
    public void setAlwaysOnTop(boolean value) {
    }
    
    @Override
    public void updateFocusableWindowState() {
    }

    @Override
    public void setResizable(boolean resizable) {
        nativeSetResizable(nativeWindow, resizable);
    }

    @Override
    public void setMinimumSize(int width, int height) {
        //nativeSetMinimumSize(nativeWindow, width, height);
    }

    @Override
    public void setWindowState(int windowState) {
        nativeSetWindowState(nativeWindow, windowState);
    }

    @Override
    public boolean rejectFocusRequest(CausedFocusEvent.Cause cause) {
        return false;
    }

    @Override
    public boolean requestWindowFocus() {
        nativeFocus(nativeWindow);
        return true;
    }

    @Override
    public void setModalBlocked(boolean blocked) {
        if (blocked) {
            assert blocker == null : "Setting a new blocker on an already blocked window";
            blocker = peer.getFirstBlocker();
            assert blocker != null : "Being asked to block but have no blocker";
        } else {
            assert blocker != null : "Being asked to unblock but have no blocker";
        }

        System.err.format("This is %d Setting blocked %b by %d%n", hashCode(), blocked, blocker.getPlatformWindow().hashCode());
        long nativeBlocker = blocker.getPlatformWindow().getLayerPtr();
        nativeSetBlocked(nativeWindow, nativeBlocker, blocked);

        if (!blocked) {
            blocker = null;
        }
    }

    @Override
    public GraphicsDevice getGraphicsDevice() {
        return getGraphicsConfiguration().getDevice();
    }

    @Override
    public boolean isActive() {
        return nativeIsActive(nativeWindow);
    }

    @Override
    public Graphics transformGraphics(Graphics g) {
        return g;
    }

    @Override
    public SurfaceData getScreenSurface() {
        if (surfaceData == null) {
            long drawable = nativeGetDrawable(nativeWindow);
            surfaceData = new HaikuWindowSurfaceData(
                HaikuWindowSurfaceData.typeDefault, getColorModel(),
                getGraphicsConfiguration(), this, drawable);
        }
        return surfaceData;
    }

    @Override
    public SurfaceData replaceSurfaceData() {
        return getScreenSurface();
    }

    public ColorModel getColorModel() {
        return getGraphicsConfiguration().getColorModel();
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        return HaikuGraphicsConfig.getDefaultConfiguration();
    }

    public Rectangle getBounds() {
        return peer.getBounds();
    }

    public Window getTarget() {
    	return target;
    }

    public void addDropTarget(Component target) {
        nativeAddDropTarget(nativeWindow, target);
    }

    public void removeDropTarget() {
        nativeRemoveDropTarget(nativeWindow);
    }

    // =======================================
    // Unimplemented/unsupported functionality
    // =======================================

    @Override
    public void updateIconImages() {
        // not supported
    }

    @Override
    public void setOpacity(float opacity) {
        // not supported
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        // not supported
    }

    @Override
    public void enterFullScreenMode() {
        // not supported
    }

    @Override
    public void exitFullScreenMode() {
        // not supported
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        (new RuntimeException("unimplemented")).printStackTrace();
        return null;
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
    
    // =====================
    // Native code callbacks
    // =====================

    public void eventRepaint(int x, int y, int width, int height) {
        // The repaint event coordinates are in view-space, so we need
        // to translate them.
        Insets insets = peer.getInsets();
        peer.notifyExpose(x + insets.left, y + insets.top, width, height);
    }

    public void eventReshape(int x, int y, int width, int height) {
        location.x = x;
        location.y = y;

        peer.notifyReshape(x, y, width, height);
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

    public void eventActivate(boolean activated) {
        peer.notifyActivation(activated);
    }

    public void eventKey(int id, long when, int modifiers, int keyCode,
            int keyLocation) {
        peer.dispatchKeyEvent(id, when, modifiers, keyCode,
            KeyEvent.CHAR_UNDEFINED, keyLocation);
    }

    public void eventKeyTyped(long when, int modifiers, String keyChar) {
    	if (keyChar.length() > 0) {
            peer.dispatchKeyEvent(KeyEvent.KEY_TYPED, when, modifiers,
                KeyEvent.VK_UNDEFINED, keyChar.charAt(0),
                KeyEvent.KEY_LOCATION_UNKNOWN);
    	}
    }

    public void eventMouse(int id, long when, int modifiers, int x, int y,
            int screenX, int screenY, int clicks, int button) {
        // Mouse event coordinates are in view-space, so we need
        // to translate them.
        Insets insets = peer.getInsets();
        x += insets.left;
        y += insets.top;

        // Popup = press button 2 or ctrl press button 1
        boolean popup = id == MouseEvent.MOUSE_PRESSED
            && (button == MouseEvent.BUTTON3 ||
	               (button == MouseEvent.BUTTON1 &&
    	       (modifiers & MouseEvent.CTRL_DOWN_MASK) != 0));

        peer.dispatchMouseEvent(id, when, button, x, y, screenX, screenY,
            modifiers, clicks, popup, null);
    }

    public void eventWheel(long when, int modifiers, int x, int y, int scrollType,
            int scrollAmount, int wheelRotation, double preciseWheelRotation) {
        // Mouse event coordinates are in view-space, so we need
        // to translate them.
        Insets insets = peer.getInsets();
        x += insets.left;
        y += insets.top;

        peer.dispatchMouseWheelEvent(when, x, y, modifiers, scrollType,
            scrollAmount, wheelRotation, preciseWheelRotation, null);
    }

    public void updateInsets(int left, int top, int right, int bottom) {
        Insets insets = new Insets(top, left, bottom, right);
        peer.updateInsets(insets);
    }

}
