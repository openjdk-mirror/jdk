/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.awt.peer.cacio;

import java.awt.Composite;
import java.awt.Rectangle;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.SpanIterator;

/**
 * Implements clipping for blit functions for systems that don't support
 * clipping for blits at all, or that only support clipping with rectangular
 * clips.
 */
public class BlitClipHelper {

    /**
     * This is the actual blitting function to be called by this utility
     * class.
     */
    public interface Blitter {

        /**
         * Performs the actual blit operation. This method gets called
         * 0 - N times, depending on the clip:
         *
         * <ul>
         * <li>If the clip or the clipped source and/or destination area is
         *     empty, then it is not called at all</li>
         * <li>If the clip is rectangular, then it is called once, with
         *     source and destination clipped accordingly.</li>
         * <li>If the clip is non-rectangular, it is called 1 - N times,
         *     depending on the complexity of the clip. The source and
         *     destination areas span parts of the original source and
         *     destination area, clipped against tiles of the clip.</li>
         * </ul>
         *
         * @param src the source surface
         * @param dst the destination surface
         * @param comp the composite setting
         * @param sx the source area, upper left X coordinate
         * @param sy the source area, upper left Y coordinate
         * @param dx the destination area, upper left X coordinate
         * @param dy the destination area, upper left Y coordinate
         * @param w the width of the source/destination area
         * @param h the height of the source/destination area
         */
        void doBlit(SurfaceData src, SurfaceData dst, Composite comp,
                    int sx, int sy, int dx, int dy, int w, int h);
    }


    public static void blitWithAnyClip(Blitter blitter,
                                       SurfaceData src,
                                       SurfaceData dst,
                                       Composite comp,
                                       Region clip,
                                       int sx, int sy,
                                       int dx, int dy,
                                       int w, int h) {

        if (clip == null) {
            blitWithRectangularClip(blitter, src, dst, comp, -1, -1, -1, -1,
                                    sx, sy, dx, dy, w, h);
        } else if (clip.isRectangular()) {
            int cx = clip.getLoX();
            int cy = clip.getLoY();
            int cw = clip.getWidth();
            int ch = clip.getHeight();
            blitWithRectangularClip(blitter, src, dst, comp, cx, cy, cw, ch,
                                    sx, sy, dx, dy, w, h);
        } else {
            SpanIterator si = clip.getSpanIterator();
            int spanbox[] = new int[4];
            while (si.nextSpan(spanbox)) {
                int cx = spanbox[0];
                int cy = spanbox[1];
                int cw = spanbox[2] - spanbox[0];
                int ch = spanbox[3] - spanbox[1];
                blitWithRectangularClip(blitter, src, dst, comp,
                                        cx, cy, cw, ch,
                                        sx, sy, dx, dy, w, h);
            }
        }
    }

    private static void blitWithRectangularClip(Blitter blitter,
                                                SurfaceData src,
                                                SurfaceData dst,
                                                Composite comp,
                                                int cx, int cy, int cw, int ch,
                                                int sx, int sy,
                                                int dx, int dy,
                                                int w, int h) {

        int dstX = dx;
	int dstY = dy;
	int srcX = sx;
	int srcY = sy;
	int width = w;
	int height = h;
	if (cw != -1) { // Original clip was null.
	    int cx1 = cx;
	    int cy1 = cy;
	    // Clip against destination rectangle.
	    dstX = Math.max(dstX, cx1);
	    dstY = Math.max(dstY, cy1);
	    width = width - (dstX - dx);
	    height = height - (dstY - dy);
	    srcX = sx + w - width;
	    srcY = sy + h - height;
	    // Now the right and bottom part...
	    int cx2 = cx + cw;
	    int cy2 = cy + ch;
	    int dstX2 = dstX + width;
	    int dstY2 = dstY + height;
	    dstX2 = Math.min(cx2, dstX2);
	    dstY2 = Math.min(cy2, dstY2);
	    width = dstX2 - dstX;
	    height = dstY2 - dstY;
	}

	if (width <= 0 || height <= 0) {
	    return;
	}

        blitter.doBlit(src, dst, comp, srcX, srcY, dstX, dstY, width, height);
    }
}
