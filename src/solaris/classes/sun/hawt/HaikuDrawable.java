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
import java.awt.image.*;
import sun.java2d.*;

/**
 * A Drawable is a BBitmap wrapper with simpler resizing semantics.
 */
public class HaikuDrawable {

    private long nativeDrawable;
    private HaikuDrawableSurfaceData surfaceData;
    private Dimension size;

    private native long nativeAllocate();
    private native boolean nativeResize(long nativeDrawable, int width,
        int height);
    private native void nativeDispose(long nativeDrawable);

    public HaikuDrawable(int width, int height) {
        nativeDrawable = nativeAllocate();
        if (nativeDrawable == 0) {
            throw new OutOfMemoryError();
        }

        setSize(new Dimension(width, height));

        GraphicsConfiguration config =
            HaikuGraphicsConfig.getDefaultConfiguration();

        surfaceData = new HaikuDrawableSurfaceData(
            HaikuDrawableSurfaceData.typeDefault, config.getColorModel(),
            config, nativeDrawable);
    }

    public static HaikuDrawable createFromImage(Image image) {
        HaikuDrawable drawable = new HaikuDrawable(image.getWidth(null),
            image.getHeight(null));
        drawable.createGraphics().drawImage(image, 0, 0, null);
        return drawable;    
    }

    public Graphics2D createGraphics() {
        return new SunGraphics2D(surfaceData, Color.white, Color.black,
            new Font(Font.DIALOG, Font.PLAIN, 12));
    }

    public int getWidth() {
        return size.width;
    }

    public int getHeight() {
        return size.height;
    }

    public void setSize(Dimension size) {
    	if (!nativeResize(nativeDrawable, size.width, size.height)) {
    	    dispose();
    	    throw new OutOfMemoryError();
    	}
    }

    public void dispose() {
        nativeDispose(nativeDrawable);
        nativeDrawable = 0;
    }

    public long getNativeDrawable() {
        return nativeDrawable;
    }
}
