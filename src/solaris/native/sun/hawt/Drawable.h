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
#ifndef DRAWABLE_H
#define DRAWABLE_H


#include <Bitmap.h>
#include <Locker.h>
#include <Rect.h>
#include <SupportDefs.h>

#include "Utilities.h"

class ContentView;

class Drawable : public BLocker {
public:
					Drawable();
					Drawable(ContentView* view);
					~Drawable();

	bool			Allocate(int width, int height);

	bool			IsValid() { return fSurface != NULL
						&& fSurface->IsValid(); }

	void*			Bits() { return fSurface->Bits(); }
	int				BytesPerRow() { return fSurface->BytesPerRow(); }
	int				BytesPerPixel();

	int				Height() { return fSurface->Bounds().IntegerHeight() + 1; }
	int				Width() { return fSurface->Bounds().IntegerWidth() + 1; }

	BBitmap*		GetBitmap() { return fSurface; }
	
	void			Invalidate(BRect rect);
private:
	BBitmap*		fSurface;
	ContentView*	fView;
};

#endif	/* DRAWABLE_H */
