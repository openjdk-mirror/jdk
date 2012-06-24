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

#include "Drawable.h"

#include <Bitmap.h>
#include <GraphicsDefs.h>
#include <Rect.h>
#include <View.h>

#include "ContentView.h"


Drawable::Drawable()
	:
	BLocker(),
	fSurface(NULL),
	fView(NULL)
{
}


Drawable::Drawable(ContentView* view)
	:
	BLocker(),
	fSurface(NULL),
	fView(view)
{
}


Drawable::~Drawable()
{
	Lock();
	if (fSurface != NULL)
		delete fSurface;
	Unlock();
}


bool
Drawable::Allocate(int width, int height)
{
	BBitmap* newSurface = new BBitmap(BRect(0, 0, width - 1, height - 1), B_RGBA32);

	if (!newSurface->IsValid()) {
		delete newSurface;
		return false;
	}

	if (fSurface != NULL) {
		// blit the contents of the old bitmap to the new one
		BRect bounds = fSurface->Bounds();
		int oldWidth = bounds.IntegerWidth() + 1;
		int blitWidth =  width > oldWidth ? oldWidth : width;
		int oldHeight = bounds.IntegerHeight() + 1;
		int blitHeight = height > oldHeight ? oldHeight : height;

		newSurface->ImportBits(fSurface, BPoint(0, 0), BPoint(0, 0), blitWidth,
			blitHeight);

		delete fSurface;
	}

	fSurface = newSurface;
	return true;
}


int
Drawable::BytesPerPixel()
{
	// This code works but since we're hard-coding B_RGBA32
	// for efficiency we might as well just...
#if 0
	if (fSurface == NULL)
		return 0;
	
	size_t pixelChunk, pixelsPerChunk;
	if (get_pixel_size_for(fSurface->ColorSpace(), &pixelChunk, NULL,
			&pixelsPerChunk) == B_OK)
        return pixelChunk / pixelsPerChunk;

	return 0;
#else
	return 4;
#endif
}


void
Drawable::Invalidate(BRect rect)
{
	if (fView != NULL) {
		if (fView->LockLooper()) {
			fView->DeferredDraw(rect);
			fView->UnlockLooper();
		}
	}
}

extern "C" {

/*
 * Class:     sun_hawt_HaikuDrawable
 * Method:    nativeAllocate
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuDrawable_nativeAllocate(JNIEnv *env, jobject thiz)
{
	Drawable* drawable = new(std::nothrow) Drawable();
	return ptr_to_jlong(drawable);
}

/*
 * Class:     sun_hawt_HaikuDrawable
 * Method:    nativeResize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_hawt_HaikuDrawable_nativeResize(JNIEnv *env, jobject thiz,
	jlong nativeDrawable, jint width, jint height)
{
	Drawable* drawable = (Drawable*)jlong_to_ptr(nativeDrawable);
	drawable->Allocate(width, height);
	return drawable->IsValid();
}

/*
 * Class:     sun_hawt_HaikuDrawable
 * Method:    nativeDispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuDrawable_nativeDispose(JNIEnv *env, jobject thiz,
	jlong nativeDrawable)
{
	Drawable* drawable = (Drawable*)jlong_to_ptr(nativeDrawable);
	delete drawable;
}

}
