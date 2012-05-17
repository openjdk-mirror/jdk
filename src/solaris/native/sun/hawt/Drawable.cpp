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

#include "PlatformView.h"

#include <stdio.h>

Drawable::Drawable()
	:
	BLocker(),
	fSurface(NULL),
	fView(NULL)
{
}


Drawable::Drawable(PlatformView* view)
	:
	BLocker(),
	fSurface(NULL),
	fView(view)
{
}


bool
Drawable::Allocate(int width, int height)
{
	if (fSurface != NULL) {
		delete fSurface;
		fSurface = NULL;
	}

	printf("(Re)allocating a bitmap, size: %d x %d\n", width, height);
	fSurface = new BBitmap(BRect(0, 0, width - 1, height - 1), B_RGBA32);

	if (!fSurface->IsValid()) {
		delete fSurface;
		fSurface = NULL;
		return false;
	}

	return true;
}


int32
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
Drawable::Invalidate(Rectangle rect)
{
	if (fView != NULL) {
		fView->LockLooper();
		fView->DeferredDraw(BRect(rect.x, rect.y,
			rect.x + rect.width - 1, rect.y + rect.height - 1));
		fView->UnlockLooper();
	}
}



