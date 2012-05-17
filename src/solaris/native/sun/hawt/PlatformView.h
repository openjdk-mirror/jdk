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
			int				GetState();
			void			SetBounds(Rectangle bounds);
			void			SetParent(PlatformView* parent);
			void			SetResizable(bool resizable);
			void			SetState(int state);
			void			SetVisible(bool visible);
	
	virtual	void			FrameMoved(BPoint origin);
	virtual	void			FrameResized(float width, float height);
	virtual	void			Draw(BRect updateRect);
	virtual	void			MakeFocus(bool focused);
			void			DeferredDraw(BRect updateRect);
	
private:
			bool			fRoot;
			Drawable		fDrawable;
			rgb_color		fColour;
			jobject			fPlatformWindow;
};

#endif	/* PLATFORM_VIEW_H */
