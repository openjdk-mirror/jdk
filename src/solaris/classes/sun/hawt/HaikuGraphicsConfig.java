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
import java.awt.geom.*;
import java.awt.image.*;
import sun.java2d.SurfaceData;

public class HaikuGraphicsConfig extends GraphicsConfiguration {
    private final HaikuGraphicsDevice device;
    private ColorModel colorModel;

	static {
		initIDs();
	}

    public HaikuGraphicsConfig(HaikuGraphicsDevice device) {
        this.device = device;
    }

	private static native void initIDs();
    private static native void nativeGetBounds(int displayID,
    	Rectangle bounds);

    @Override
    public Rectangle getBounds() {
    	Rectangle bounds = new Rectangle();
        nativeGetBounds(device.getDisplayID(), bounds);
        return bounds;
    }

    @Override
    public ColorModel getColorModel() {
        return getColorModel(Transparency.OPAQUE);
    }

    @Override
    public ColorModel getColorModel(int transparency) {
    	if (colorModel == null)
    		colorModel = new DirectColorModel(32, 0x00FF0000, 0x0000FF00,
	        	0x000000FF, 0xFF000000);
		return colorModel;
    }

    @Override
    public AffineTransform getDefaultTransform() {
        return new AffineTransform();
    }

    @Override
    public HaikuGraphicsDevice getDevice() {
        return device;
    }
    
    public static HaikuGraphicsConfig getDefaultConfiguration() {
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        HaikuGraphicsConfig gc =
        		(HaikuGraphicsConfig)gd.getDefaultConfiguration();
        return gc;
    }
    
    @Override
    public AffineTransform getNormalizingTransform() {
        double xscale = device.getXResolution() / 72.0;
        double yscale = device.getYResolution() / 72.0;
        return new AffineTransform(xscale, 0.0, 0.0, yscale, 0.0, 0.0);
    }
}
