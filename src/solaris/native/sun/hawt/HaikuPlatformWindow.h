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
#ifndef HAIKU_PLATFORM_WINDOW_H
#define	HAIKU_PLATFORM_WINDOW_H

#include <View.h>
#include <Window.h>

#include "Drawable.h"
#include "Utilities.h"

class PlatformView;

const int kStateMinimized = 0x01;
const int kStateNormal = 0x02;
const int kStateMaximized = 0x03;

// The PlatformWindow "interface" defines the methods required of
// PlatformFrame and PlatformView, since I decided to treat them
// the same in the Java class.
class PlatformWindow {
public:
	virtual Rectangle		GetBounds() = 0;
	virtual	PlatformView*	GetContainer() = 0;
	virtual	Drawable*		GetDrawable() = 0;
	virtual	Point			GetLocation() = 0;
	virtual	Point			GetLocationOnScreen() = 0;
	virtual	int				GetState() = 0;
	virtual	void			SetBounds(Rectangle bounds) = 0;
	virtual	void			SetParent(PlatformView* parent) = 0;
	virtual	void			SetResizable(bool resizable) = 0;
	virtual	void			SetState(int state) = 0;
	virtual	void			SetVisible(bool visible) = 0;
};

#endif	/* HAIKU_PLATFORM_WINDOW_H */
