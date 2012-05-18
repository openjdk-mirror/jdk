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

#include <jni.h>

#include "sun_hawt_HaikuWindowSurfaceData.h"

#include <Bitmap.h>

#include "Drawable.h"
#include "HaikuWindowSurfaceData.h"
#include "SurfaceData.h"

extern "C" {

extern void JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg);


static jint HaikuLock(JNIEnv* env, SurfaceDataOps* ops,
                    SurfaceDataRasInfo* rasInfo, jint lockFlags);
static void HaikuGetRasInfo(JNIEnv* env, SurfaceDataOps* ops,
                          SurfaceDataRasInfo* rasInfo);
static void HaikuRelease(JNIEnv* env, SurfaceDataOps* ops,
                       SurfaceDataRasInfo* rasInfo);
static void HaikuUnlock(JNIEnv* env, SurfaceDataOps* ops,
                      SurfaceDataRasInfo* rasInfo);

JNIEXPORT void JNICALL Java_sun_hawt_HaikuWindowSurfaceData_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL Java_sun_hawt_HaikuWindowSurfaceData_initOps
  (JNIEnv* env, jobject thiz, jlong drawable, jint width, jint height)
{
    HaikuWindowSurfaceDataOps* operations = (HaikuWindowSurfaceDataOps*)
        SurfaceData_InitOps(env, thiz, sizeof(HaikuWindowSurfaceDataOps));

    operations->sdOps.Lock = &HaikuLock;
    operations->sdOps.GetRasInfo = &HaikuGetRasInfo;
    operations->sdOps.Release = &HaikuRelease;
    operations->sdOps.Unlock = &HaikuUnlock;
    operations->drawable = (Drawable*)drawable;
    operations->width = width;
    operations->height = height;
    
    if (operations->drawable->Lock()) {
	    operations->drawable->Allocate(width, height);
    	operations->drawable->Unlock();
    }
}

static jint HaikuLock(JNIEnv* env, SurfaceDataOps* ops,
                    SurfaceDataRasInfo* rasInfo, jint lockFlags)
{
    HaikuWindowSurfaceDataOps* operations = (HaikuWindowSurfaceDataOps*)ops;

    if (!operations->drawable->Lock())
		return SD_FAILURE;

	// We don't clip to Drawable bounds because we just
	// reallocate the Drawable if neccessary in GetRasInfo.
	// We probably should clip to PlatformView bounds though.
/*
	int width = operations->drawable->Width();
	int height = operations->drawable->Height();

    if (rasInfo->bounds.x1 > width) {
        rasInfo->bounds.x1 = width;
    }

    if (rasInfo->bounds.y1 > height) {
        rasInfo->bounds.y1 = height;
    }

    if (rasInfo->bounds.x2 > width) {
      rasInfo->bounds.x2 = width;
    }

    if (rasInfo->bounds.y2 > height) {
      rasInfo->bounds.y2 = height;
    }
*/

	// We can honour the SD_LOCK_FASTEST since we just provide
	// direct pixel access via BBitmap::Bits()
	operations->lockFlags = lockFlags;
    return SD_SUCCESS;
}

static void HaikuGetRasInfo(JNIEnv* env, SurfaceDataOps* ops,
        SurfaceDataRasInfo* rasInfo)
{
    HaikuWindowSurfaceDataOps* operations = (HaikuWindowSurfaceDataOps*)ops;
    Drawable* drawable = operations->drawable;

	int width = rasInfo->bounds.x2;
	int height = rasInfo->bounds.y2;

	if (!drawable->IsValid() || width > drawable->Width()
			|| height > drawable->Height()) {
		drawable->Allocate(width, height);
	}
    
    if (drawable->IsValid()) {
        rasInfo->rasBase = drawable->Bits();
        rasInfo->pixelStride = drawable->BytesPerPixel();
        rasInfo->pixelBitOffset = 0;
        rasInfo->scanStride = drawable->BytesPerRow();
    } else {
    	// Out of memory
        rasInfo->rasBase = NULL;
        rasInfo->pixelStride = 0;
        rasInfo->pixelBitOffset = 0;
        rasInfo->scanStride = 0;
    }
}

static void HaikuRelease(JNIEnv* env, SurfaceDataOps* ops,
        SurfaceDataRasInfo* rasInfo)
{
}

static void HaikuUnlock(JNIEnv* env, SurfaceDataOps* ops,
        SurfaceDataRasInfo* rasInfo)
{
	HaikuWindowSurfaceDataOps* operations = (HaikuWindowSurfaceDataOps*)ops;

	// Must drop the lock before invalidating because otherwise
	// we can deadlock with FrameResized. Invalidate wants
	// the looper lock which FrameResized holds and FrameResized
	// wants (indirectly) the Drawable lock which we hold.
    operations->drawable->Unlock();

    // If we were locked for writing the view needs
    // to redraw now.
    if (operations->lockFlags & SD_LOCK_WRITE) {
    	int x = rasInfo->bounds.x1;
    	int y = rasInfo->bounds.y1;
    	int w = rasInfo->bounds.x2 - x;
    	int h = rasInfo->bounds.y2 - y;
	    operations->drawable->Invalidate(Rectangle(x, y, w, h));
    }
}

}
