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
#ifndef PLATFORM_VIEW_H
#define	PLATFORM_VIEW_H

#include <View.h>

#include "HaikuPlatformWindow.h"

class PlatformView : public BView, public PlatformWindow {
public:
							PlatformView(jobject platformWindow, bool root);

			Rectangle		GetBounds();
			PlatformView*	GetContainer();
			Drawable*		GetDrawable();
			Point			GetLocation();
			Point			GetLocationOnScreen();
			void			SetBounds(Rectangle bounds);
			void			SetParent(PlatformView* parent);
			bool			GetVisible();
			void			SetVisible(bool visible);
			void			Dispose();
			void			Focus();

			void			DeferredDraw(BRect updateRect);

	virtual	void			Draw(BRect updateRect);
	virtual	void			FrameMoved(BPoint origin);
	virtual	void			FrameResized(float width, float height);
	virtual	void			KeyDown(const char* bytes, int32 numBytes);
	virtual	void			KeyUp(const char* bytes, int32 numBytes);
	virtual	void			MakeFocus(bool focused);
	virtual	void			MessageReceived(BMessage* message);
	virtual	void			MouseDown(BPoint point);
	virtual	void			MouseMoved(BPoint point, uint32 transit,
								const BMessage* message);
	virtual	void			MouseUp(BPoint point);
	
private:
			void			_HandleKeyEvent(BMessage* message);
			void			_HandleMouseEvent(BMessage* message, BPoint point,
								uint32 transit = 0);
			void			_HandleWheelEvent(BMessage* message);

private:
			bool			fRoot;
			Drawable		fDrawable;
			rgb_color		fColour;
			jobject			fPlatformWindow;
			uint32			fPreviousButtons;
};

#endif	/* PLATFORM_VIEW_H */
