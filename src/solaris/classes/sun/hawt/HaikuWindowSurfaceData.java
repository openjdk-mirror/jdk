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

import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

import sun.java2d.SurfaceData;
import sun.java2d.loops.SurfaceType;

class HaikuWindowSurfaceData extends SurfaceData {

    static SurfaceType typeDefault =
            SurfaceType.IntArgb.deriveSubType("Haiku-ARGB");

    static {
        initIDs();
    }

    private Rectangle bounds;
    private GraphicsConfiguration config;
    private HaikuPlatformWindow window;

    private static final native void initIDs();
    private native final void initOps(long drawable, int width, int height);

    HaikuWindowSurfaceData(SurfaceType surfaceType, ColorModel colorModel,
    		Rectangle bounds, GraphicsConfiguration config,
    		HaikuPlatformWindow window, long drawable) {
        super(surfaceType, colorModel);

        this.bounds = bounds;
        this.config = config;
        this.window = window;

        initOps(drawable, bounds.width, bounds.height);
    }

    @Override
    public Rectangle getBounds() {
    	Rectangle bounds = window.getBounds();
        bounds.x = bounds.y = 0;
        return bounds;
    }

    @Override
    public Object getDestination() {
        return (Object)window;
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return config;
    }

    @Override
    public Raster getRaster(int arg0, int arg1, int arg2, int arg3) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SurfaceData getReplacement() {
        throw new UnsupportedOperationException("Not supported.");
    }



}
