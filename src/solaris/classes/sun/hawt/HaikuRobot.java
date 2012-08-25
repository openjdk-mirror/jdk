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
import java.awt.peer.RobotPeer;
import sun.hawt.HaikuGraphicsDevice;

class HaikuRobot implements RobotPeer {

	private Robot target;
    private HaikuGraphicsDevice device;

    private native void nativeGetPixels(int displayID, int x, int y,
        int width, int height, int[] pixels);

    public HaikuRobot(Robot target, GraphicsDevice device) {
        this.device = (HaikuGraphicsDevice)device;
        this.target = target;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void mouseMove(int x, int y) {
        // No real way to do this
    }

    @Override
    public void mousePress(int buttons) {
        // No real way to do this
    }

    @Override
    public void mouseRelease(int buttons) {
        // No real way to do this
    }

    @Override
    public void mouseWheel(int wheelAmt) {
    	// No real way to do this
    }

    @Override
    public void keyPress(final int keycode) {
        // No real way to do this
    }

    @Override
    public void keyRelease(final int keycode) {
        // No real way to do this
    }

    @Override
    public int getRGBPixel(int x, int y) {
        int[] pixels = getRGBPixels(new Rectangle(x, y, 1, 1));
        return pixels[0];
    }

    @Override
    public int[] getRGBPixels(Rectangle bounds) {
        int[] pixels = new int[bounds.width * bounds.height];
        nativeGetPixels(device.getDisplayID(), bounds.x, bounds.y,
            bounds.width, bounds.height, pixels);
        return pixels;
    }
}
