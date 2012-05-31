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
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.dnd.peer.*;
import java.awt.font.*;
import java.awt.im.*;
import java.awt.im.spi.*;
import java.awt.image.*;
import java.awt.peer.*;
import java.util.*;
import java.util.logging.*;
import sun.awt.*;
import sun.lwawt.*;
import sun.lwawt.LWWindowPeer.PeerType;

public class HaikuToolkit extends LWToolkit {

    private HaikuClipboard clipboard;

	private static native void nativeInit();
    private native void nativeRunMessage();
    private native void nativeLoadSystemColors(int[] systemColors);
    private native void nativeBeep();

	static {
		System.loadLibrary("awt");
	}

    public HaikuToolkit() {
        super();
        SunToolkit.setDataTransfererClassName("sun.hawt.HaikuDataTransferer");

		nativeInit();
        init();
    }

    @Override
    protected void loadSystemColors(int[] systemColors) {
        if (systemColors == null)
            return;

        nativeLoadSystemColors(systemColors);
    }

    @Override
    protected PlatformWindow createPlatformWindow(PeerType peerType) {
        if (peerType == PeerType.EMBEDDEDFRAME) {
        	System.err.println("Creating embedded frame!");
        	return null;
        } else {
        	return new HaikuPlatformWindow(peerType);
        }
    }

    @Override
    protected PlatformComponent createPlatformComponent() {
        return new HaikuPlatformComponent();
    }

    @Override
    protected void platformCleanup() {
    }

    @Override
    protected void platformInit() {
    }

    @Override
    protected void platformRunMessage() {
    	nativeRunMessage();
    }

    @Override
    protected void platformShutdown() {
    }

    @Override
    protected void initializeDesktopProperties() {
        super.initializeDesktopProperties();

        // Enable font antialiasing by default
        // Note -- can get these settings programmatically using some private
        // functions in libbe.
        Map<Object, Object> fontHints = new HashMap<Object, Object>();
        fontHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        fontHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        desktopProperties.put(SunToolkit.DESKTOPFONTHINTS, fontHints);
    }

    @Override
    public Clipboard createPlatformClipboard() {
        synchronized (this) {
            if (clipboard == null)
                clipboard = new HaikuClipboard("System");
        }
        return clipboard;
    }

    @Override
    public LWCursorManager getCursorManager() {
        return HaikuCursorManager.getInstance();
    }

    @Override
    protected FileDialogPeer createFileDialogPeer(FileDialog target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MenuPeer createMenu(Menu target) {
        MenuPeer peer = new HaikuMenu(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuBarPeer createMenuBar(MenuBar target) {
        MenuBarPeer peer = new HaikuMenuBar(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuItemPeer createMenuItem(MenuItem target) {
        MenuItemPeer peer = new HaikuMenuItem(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
        CheckboxMenuItemPeer peer = new HaikuCheckboxMenuItem(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public PopupMenuPeer createPopupMenu(PopupMenu target) {
        PopupMenuPeer peer = new HaikuPopupMenu(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isTraySupported() {
        return false;
    }

    @Override
    public TrayIconPeer createTrayIcon(TrayIcon target) throws HeadlessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SystemTrayPeer createSystemTray(SystemTray target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    class HaikuPlatformFont extends PlatformFont {

        public HaikuPlatformFont(String name, int style) {
            super(name, style);
        }

        protected char getMissingGlyphCharacter() {
            return (char)0xfff8;
        }
    }

    @Override
    public FontPeer getFontPeer(String name, int style) {
        return new HaikuPlatformFont(name, style);
    }


    @Override
    public RobotPeer createRobot(Robot target, GraphicsDevice screen) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected int getScreenWidth() {
        GraphicsConfiguration config =
                HaikuGraphicsConfig.getDefaultConfiguration();
        return config.getBounds().width;
    }

    @Override
    protected int getScreenHeight() {
        GraphicsConfiguration config =
                HaikuGraphicsConfig.getDefaultConfiguration();
        return config.getBounds().height;
    }

    @Override
    protected boolean syncNativeQueue(long timeout) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDesktopSupported() {
        return true;
    }

    @Override
    protected DesktopPeer createDesktopPeer(Desktop target)
    		throws HeadlessException {
        return new HaikuDesktopPeer();
    }

    @Override
    public int getScreenResolution() throws HeadlessException {
        HaikuGraphicsConfig config =
                HaikuGraphicsConfig.getDefaultConfiguration();
        return (int)((HaikuGraphicsDevice)config.getDevice()).getScreenResolution();
    }

    @Override
    public ColorModel getColorModel() throws HeadlessException {
        HaikuGraphicsConfig config =
                HaikuGraphicsConfig.getDefaultConfiguration();
        return config.getColorModel();
    }

    @Override
    public void sync() {
    }

    @Override
    public PrintJob getPrintJob(Frame frame, String jobtitle,
                                Properties props) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void beep() {
    	nativeBeep();
    }

    @Override
    public Map<TextAttribute, ?> mapInputMethodHighlight(InputMethodHighlight highlight) throws HeadlessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputMethodDescriptor getInputMethodAdapterDescriptor() throws AWTException {
        return null;
    }

}
