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

#include "PlatformView.h"

#include <BitmapStream.h>
#include <File.h>
#include <TranslatorRoster.h>
#include <kernel/OS.h>

#include <stdio.h>
#include <stdlib.h>

PlatformView::PlatformView(jobject platformWindow, bool root)
	:
	BView(BRect(0, 0, 0, 0), NULL, root ? B_FOLLOW_ALL : B_FOLLOW_NONE,
		B_WILL_DRAW | B_FRAME_EVENTS),
	fRoot(root),
	fDrawable(this),
	fPlatformWindow(platformWindow)
{
}


Rectangle
PlatformView::GetBounds()
{
	LockLooper();
	BRect frame = Frame();
	return Rectangle(frame.left, frame.top, frame.IntegerWidth() + 1,
		frame.IntegerHeight() + 1);
	UnlockLooper();
}


PlatformView*
PlatformView::GetContainer()
{
	return this;
}


Drawable*
PlatformView::GetDrawable()
{
	return &fDrawable;
}


Point
PlatformView::GetLocation()
{
	Rectangle bounds = GetBounds();
	return Point(bounds.x, bounds.y);
}


Point
PlatformView::GetLocationOnScreen()
{
	LockLooper();
	BRect frame = Bounds();
	ConvertToScreen(&frame);
	return Point(frame.left, frame.top);
	UnlockLooper();
}


int
PlatformView::GetState()
{
	return 0;
}


void
PlatformView::SetBounds(Rectangle bounds)
{
	LockLooper();
	MoveTo(bounds.x, bounds.y);
	ResizeTo(bounds.width - 1, bounds.height - 1);
	UnlockLooper();
}


void
PlatformView::SetParent(PlatformView* parent)
{
	LockLooper();
	Rectangle bounds = parent->GetBounds();
	PlatformView* oldParent = (PlatformView*)Parent();
	if (oldParent != NULL)
		oldParent->RemoveChild(this);
	parent->AddChild(this);
	UnlockLooper();
}


void
PlatformView::SetResizable(bool resizable)
{
}


void
PlatformView::SetState(int state)
{
}


void
PlatformView::SetVisible(bool visible)
{
	LockLooper();
	if (visible)
		Show();
	else
		Hide();
	UnlockLooper();
}


void
PlatformView::FrameMoved(BPoint origin)
{
	if (!fRoot) {
		int x = origin.x;
		int y = origin.y;
		DoCallback(fPlatformWindow, "eventMove", "(II)V", x, y);
	}
}


void
PlatformView::FrameResized(float width, float height)
{
	int w = width + 1;
	int h = height + 1;

	fDrawable.Lock();
	if (fDrawable.IsValid()) {
		if (w > fDrawable.Width() || h > fDrawable.Height()) {
			// If we're resizing, increase the bitmap size by 100px
			// so we're not reallocating all the time through a resize.
			// Also, don't bother with shrinking?
			fDrawable.Allocate(w + 100, h + 100);
		}
	}
	fDrawable.Unlock();
	
	if (!fRoot) {
		DoCallback(fPlatformWindow, "eventResize", "(II)V", w, h);
	}
}


void
PlatformView::Draw(BRect updateRect)
{
	DeferredDraw(updateRect);

	jint x = updateRect.left;
	jint y = updateRect.top;
	jint width = updateRect.right - updateRect.left + 1;
	jint height = updateRect.bottom - updateRect.top + 1;
	DoCallback(fPlatformWindow, "eventRepaint", "(IIII)V", x, y, width, height);
}


void
PlatformView::MakeFocus(bool focused)
{
	if (!fRoot)
		DoCallback(fPlatformWindow, "eventFocus", "(Z)V", focused);
}


void
PlatformView::DeferredDraw(BRect updateRect)
{
	fDrawable.Lock();
	if (fDrawable.IsValid())
		DrawBitmap(fDrawable.GetBitmap(), updateRect, updateRect);
	fDrawable.Unlock();
		
}
