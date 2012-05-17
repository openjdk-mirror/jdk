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
import java.util.*;
import sun.java2d.*;

/**
 * Haiku GraphicsEnvironment implementation.
 * @author Hamish Morrison
 */
public class HaikuGraphicsEnvironment extends SunGraphicsEnvironment {

    static {
        SurfaceManagerFactory.setInstance(new HaikuSurfaceManagerFactory());
    }

    private final Map<Integer, HaikuGraphicsDevice> devices = new HashMap<>();

    private static native int[] getDisplayIDs();
    private static native int getMainDisplayID();

    public HaikuGraphicsEnvironment() {
        if (isHeadless()) {
            return;
        }
        initDevices();
    }

    private synchronized void initDevices() {
        devices.clear();

        final int[] displayIDs = getDisplayIDs();
        for (int displayID : displayIDs) {
            devices.put(displayID, new HaikuGraphicsDevice(displayID));
        }
    }

    @Override
    public synchronized GraphicsDevice getDefaultScreenDevice() throws HeadlessException {
        final int mainDisplayID = getMainDisplayID();
        HaikuGraphicsDevice d = devices.get(mainDisplayID);
        return d;
    }

    @Override
    public synchronized GraphicsDevice[] getScreenDevices() throws HeadlessException {
    	Collection<HaikuGraphicsDevice> screens = devices.values();
        return screens.toArray(new HaikuGraphicsDevice[screens.size()]);
    }

    @Override
    protected synchronized int getNumScreens() {
        return devices.size();
    }

    @Override
    protected GraphicsDevice makeScreenDevice(int screennum) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public boolean isDisplayLocal() {
       return true;
    }
}
