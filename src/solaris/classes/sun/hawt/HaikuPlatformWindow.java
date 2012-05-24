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

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.peer.ContainerPeer;
import java.awt.Window;

import sun.awt.AWTAccessor;
import sun.awt.CausedFocusEvent.Cause;
import sun.awt.PaintEventDispatcher;
import sun.awt.peer.cacio.CacioComponent;
import sun.awt.peer.cacio.PlatformToplevelWindow;
import sun.awt.peer.cacio.PlatformWindow;
import sun.awt.SunToolkit;
import sun.java2d.NullSurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;

class HaikuPlatformWindow implements PlatformToplevelWindow {

    static {
        initIDs();
    }

    private long nativeWindow;
    private boolean toplevel;
    private HaikuWindowSurfaceData surfaceData;
    private CacioComponent cacioComponent;
    private HaikuPlatformWindow parent;
    
    private int windowState = Frame.NORMAL;

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

    HaikuPlatformWindow(CacioComponent cacioComponent,
    		HaikuPlatformWindow parent, boolean toplevel, int x, int y,
    		int width, int height) {

    	if (toplevel) {
    		Component awtComp = cacioComponent.getAWTComponent();

    		boolean decorated = false;
    		if (awtComp instanceof Frame || awtComp instanceof Dialog)
    			decorated = true;
    		nativeWindow = nativeInitFrame(x, y, width, height, decorated);
    	} else {
    		if (parent != null)
		    	nativeWindow = nativeInitView(x, y, width, height,
		    		parent.nativeWindow);
			else
				nativeWindow = nativeInitView(x, y, width, height, 0);
    	}

        this.cacioComponent = cacioComponent;
        this.parent = parent;
        this.toplevel = toplevel;
    }

    public CacioComponent getCacioComponent() {
        return cacioComponent;
    }

    public ColorModel getColorModel() {
        return getGraphicsConfiguration().getColorModel();
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        return HaikuGraphicsConfig.getDefaultConfiguration();
    }

    public Graphics2D getGraphics(Color foreground, Color background,
    		Font font) {
    	//System.err.println("Get graphics for " + hashCode());
        SurfaceData surface = getSurfaceData();
        Graphics2D graphics = new SunGraphics2D(surface, foreground,
        	background, font);
        return graphics;
    }

    private SurfaceData getSurfaceData() {
        if (surfaceData == null) {
        	long drawable = nativeGetDrawable(nativeWindow);
            surfaceData = new HaikuWindowSurfaceData(
            	HaikuWindowSurfaceData.typeDefault, getColorModel(),
            	getBounds(), getGraphicsConfiguration(), this, drawable);
        }
        return surfaceData;
    }

    public void setBounds(int x, int y, int width, int height, int op) {
    	// TODO handle the client area op
        nativeSetBounds(nativeWindow, x, y, width, height);
    }

	public Rectangle getBounds() {
		Rectangle bounds = new Rectangle();
		nativeGetBounds(nativeWindow, bounds);
		return bounds;
	}

    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    public Point getLocationOnScreen() {
        Point location = new Point();
        nativeGetLocationOnScreen(nativeWindow, location);
        return location;
    }

    private Point getLocation() {
        Point location = new Point();
        nativeGetLocation(nativeWindow, location);
        return location;
    }

    public void setVisible(boolean visible) {
        nativeSetVisible(nativeWindow, visible);
    }

    public void dispose() {
        nativeDispose(nativeWindow);
    }

    public void requestFocus() {
    	System.err.println("Requesting focus for " + nativeWindow);
        nativeFocus(nativeWindow);
    }

	// =====================
	// Native code callbacks
	// =====================

	public void eventRepaint(int x, int y, int width, int height) {
		Component awtComp = cacioComponent.getAWTComponent();
		if (!AWTAccessor.getComponentAccessor().getIgnoreRepaint(awtComp)) {
	        PaintEvent ev = PaintEventDispatcher.getPaintEventDispatcher()
    	          .createPaintEvent(awtComp, x, y, width, height);
        	postEvent(cacioComponent, ev);
		}
	}

    public void eventResize(int width, int height) {
    	Component awtComp = cacioComponent.getAWTComponent();
    	AWTAccessor.getComponentAccessor().setSize(awtComp, width, height);
        ComponentEvent ev = new ComponentEvent(awtComp,
        	ComponentEvent.COMPONENT_RESIZED);
        postEvent(cacioComponent, ev);
    }
    
    public void eventMove(int x, int y) {
    	Component awtComp = cacioComponent.getAWTComponent();
    	AWTAccessor.getComponentAccessor().setLocation(awtComp, x, y);
        ComponentEvent ev = new ComponentEvent(awtComp,
        	ComponentEvent.COMPONENT_MOVED);
        postEvent(cacioComponent, ev);
    }
    
    public void eventMaximize(boolean maximize) {
    	int newState = maximize ? Frame.MAXIMIZED_BOTH : Frame.NORMAL;
    	WindowEvent ev = new WindowEvent((Window)cacioComponent.getAWTComponent(),
    		WindowEvent.WINDOW_STATE_CHANGED, windowState, newState);
    	postEvent(cacioComponent, ev);
    	
    	windowState = newState;
    }
    
    public void eventMinimize(boolean minimize) {
    	Component awtComp = cacioComponent.getAWTComponent();
    	WindowEvent iconifyEv = new WindowEvent((Window)awtComp,
    		minimize ? WindowEvent.WINDOW_ICONIFIED :
    		WindowEvent.WINDOW_DEICONIFIED);
    	postEvent(cacioComponent, iconifyEv);
    	
    	int newState = minimize ? Frame.ICONIFIED : Frame.NORMAL;
    	WindowEvent stateEv = new WindowEvent((Window)awtComp,
    		WindowEvent.WINDOW_STATE_CHANGED, windowState, newState);
    	postEvent(cacioComponent, stateEv);

    	windowState = newState;
    }
    
    public void eventWindowClosing() {
    	WindowEvent ev = new WindowEvent((Window)cacioComponent.getAWTComponent(),
    		WindowEvent.WINDOW_CLOSING);
    	postEvent(cacioComponent, ev);
    }

    public void eventFocus(boolean focused) {
    	FocusEvent ev = new FocusEvent(cacioComponent.getAWTComponent(),
    		focused ? FocusEvent.FOCUS_GAINED : FocusEvent.FOCUS_LOST);
    	postEvent(cacioComponent, ev);
    }

	private void handleMouseDown(long when, int modifiers, int x, int y,
			int clicks, int button) {
		boolean popup = false;
		// Right mouse button or Ctrl and left mouse button are popup
		// triggers on Haiku
		if (button == MouseEvent.BUTTON2 || (button == MouseEvent.BUTTON1
				&& (modifiers & MouseEvent.CTRL_DOWN_MASK) != 0))
			popup = true;

		Component comp = cacioComponent.getAWTComponent();
		MouseEvent ev = new MouseEvent(comp, MouseEvent.MOUSE_PRESSED, when,
			modifiers, x, y, clicks, popup, button);
		postEvent(cacioComponent, ev);
	}

	private void handleMouseUp(long when, int modifiers, int x, int y,
			int clicks, int button) {
		Component comp = cacioComponent.getAWTComponent();
		MouseEvent ev = new MouseEvent(comp, MouseEvent.MOUSE_RELEASED, when,
			modifiers, x, y, clicks, false, button);
		postEvent(cacioComponent, ev);
		ev = new MouseEvent(comp, MouseEvent.MOUSE_CLICKED, when,
			modifiers, x, y, clicks, false, button);
		postEvent(cacioComponent, ev);
	}

	public void eventMouse(int id, long when, int modifiers, int x, int y,
			int clicks, int pressed, int released) {
    	// Mouse up/down is weird on Haiku so we check what buttons
    	// exactly have changed with every mouse message and then
    	// fire off the appropriate events.
		if ((pressed & MouseEvent.BUTTON1_DOWN_MASK) != 0)
			handleMouseDown(when, modifiers, x, y, clicks, MouseEvent.BUTTON1);
		if ((pressed & MouseEvent.BUTTON2_DOWN_MASK) != 0)
			handleMouseDown(when, modifiers, x, y, clicks, MouseEvent.BUTTON2);
		if ((pressed & MouseEvent.BUTTON3_DOWN_MASK) != 0)
			handleMouseDown(when, modifiers, x, y, clicks, MouseEvent.BUTTON3);
		if ((released & MouseEvent.BUTTON1_DOWN_MASK) != 0)
			handleMouseUp(when, modifiers, x, y, clicks, MouseEvent.BUTTON1);
		if ((released & MouseEvent.BUTTON2_DOWN_MASK) != 0)
			handleMouseUp(when, modifiers, x, y, clicks, MouseEvent.BUTTON2);
		if ((released & MouseEvent.BUTTON3_DOWN_MASK) != 0)
			handleMouseUp(when, modifiers, x, y, clicks, MouseEvent.BUTTON3);

		if (id != MouseEvent.MOUSE_PRESSED
				&& id != MouseEvent.MOUSE_RELEASED) {
    		Component awtComp = cacioComponent.getAWTComponent();
			MouseEvent ev = new MouseEvent(cacioComponent.getAWTComponent(),
				id, when, modifiers, x, y, clicks, false);
			postEvent(cacioComponent, ev);
		}
	}
    
    private void postEvent(CacioComponent component, AWTEvent ev) {
		component.handlePeerEvent(ev);
    }

	// ===================
	// Unimplemented stuff
	// ===================


    public void setMaximizedBounds(Rectangle bounds) {
        System.err.println("PlatformWindow.setMaximizedBounds(): Implement me!");
    }

    public boolean canDetermineObscurity() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isObscured() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void applyShape(Region shape) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isReparentSuppored() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void reparent(ContainerPeer newContainer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isRestackSupported() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void restack() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean requestFocus(Component lightweightChild, boolean temporary, boolean focusedWindowChangeAllowed, long time, Cause cause) {
    	System.err.println("Requesting focus for " + nativeWindow);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBackground(Color c) {
    	System.err.println("Set background called.");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setForeground(Color c) {
    	System.err.println("Set foreground called.");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setFont(Font f) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void createBuffers(int numBuffers, BufferCapabilities caps) throws AWTException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void destroyBuffers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void flip(int x1, int y1, int x2, int y2, FlipContents flipAction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Image getBackBuffer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getState() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setState(int state) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setResizable(boolean resizable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setTitle(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlocked(boolean blocked) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
