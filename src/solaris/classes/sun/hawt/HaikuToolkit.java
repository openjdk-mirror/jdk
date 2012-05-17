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
import sun.awt.peer.cacio.*;

public class HaikuToolkit extends CacioToolkit {

	static {
		System.loadLibrary("awt");
	}

    private PlatformWindowFactory platformWindow;

    public HaikuToolkit() {
        super();
    }

    @Override
    public synchronized PlatformWindowFactory getPlatformWindowFactory() {
        if (platformWindow == null) {
            platformWindow = new HaikuPlatformWindowFactory();
        }
        return platformWindow;
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
    public DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TrayIconPeer createTrayIcon(TrayIcon target) throws HeadlessException, AWTException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SystemTrayPeer createSystemTray(SystemTray target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isTraySupported() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FontPeer getFontPeer(String name, int style) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RobotPeer createRobot(Robot target, GraphicsDevice screen) throws AWTException {
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
    public void grab(Window w) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ungrab(Window w) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDesktopSupported() {
        return false;
    }

    @Override
    protected DesktopPeer createDesktopPeer(Desktop target)
            throws HeadlessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getScreenResolution() throws HeadlessException {
        HaikuGraphicsConfig config =
                HaikuGraphicsConfig.getDefaultConfiguration();
        return (int)((HaikuGraphicsDevice)config.getDevice()).getScreenResolution();
    }

    @Override
    public ColorModel getColorModel() throws HeadlessException {
        throw new UnsupportedOperationException("Not supported yet.");
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
    }

    @Override
    public Clipboard getSystemClipboard() throws HeadlessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<TextAttribute, ?> mapInputMethodHighlight(InputMethodHighlight highlight) throws HeadlessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputMethodDescriptor getInputMethodAdapterDescriptor() throws AWTException {
        return null;
    }
}
