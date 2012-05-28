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

#include <String.h>

#include <stdio.h>
#include <stdlib.h>

#include "java_awt_event_KeyEvent.h"
#include "java_awt_event_MouseEvent.h"
#include "java_awt_event_MouseWheelEvent.h"

PlatformView::PlatformView(jobject platformWindow, bool root)
	:
	BView(BRect(0, 0, 0, 0), NULL, root ? B_FOLLOW_ALL : B_FOLLOW_NONE,
		B_WILL_DRAW | B_FRAME_EVENTS | B_NAVIGABLE),
	fRoot(root),
	fDrawable(this),
	fPlatformWindow(platformWindow)
{
}


Rectangle
PlatformView::GetBounds()
{
	if (!LockLooper())
		return Rectangle(0, 0, 0, 0);

	BRect frame = Frame();
	UnlockLooper();
	return Rectangle(frame.left, frame.top, frame.IntegerWidth() + 1,
		frame.IntegerHeight() + 1);
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
	if (!LockLooper())
		return Point(0, 0);

	BRect frame = Bounds();
	ConvertToScreen(&frame);
	UnlockLooper();
	return Point(frame.left, frame.top);
}


int
PlatformView::GetState()
{
	return 0;
}


void
PlatformView::SetBounds(Rectangle bounds)
{
	if (!LockLooper())
		return;

	MoveTo(bounds.x, bounds.y);
	ResizeTo(bounds.width - 1, bounds.height - 1);
	UnlockLooper();
}


void
PlatformView::SetName(const char* name)
{
}


void
PlatformView::SetParent(PlatformView* parent)
{
	// We don't support reparenting yet but when we do
	// we need to rethink the locking here
	
	PlatformView* oldParent = (PlatformView*)Parent();
	if (oldParent != NULL) {
		oldParent->RemoveChild(this);
	}
	parent->AddChild(this);
}


void
PlatformView::SetResizable(bool resizable)
{
}


void
PlatformView::SetState(int state)
{
}


bool
PlatformView::GetVisible()
{
	if (!LockLooper())
		return false;

	// This also reflects whether the parent views and
	// Window are hidden.
	bool visible = !IsHidden();

	UnlockLooper();
	return visible;
}


void
PlatformView::SetVisible(bool visible)
{
	if (!LockLooper())
		return;

	if (visible) {
		while (IsHidden())
			Show();
	} else {
		while (!IsHidden())
			Hide();
	}
	UnlockLooper();
}


void
PlatformView::Dispose()
{
	RemoveSelf();
	delete this;
}


void
PlatformView::Focus()
{
	LockLooper();
	MakeFocus(true);
	UnlockLooper();
}


void
PlatformView::DeferredDraw(BRect updateRect)
{
	if (!fDrawable.Lock())
		return;
	if (fDrawable.IsValid())
		DrawBitmapAsync(fDrawable.GetBitmap(), updateRect, updateRect);
	//if (fRoot)
	//	FillRect(Bounds(), B_SOLID_LOW);
	fDrawable.Unlock();
		
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
PlatformView::FrameMoved(BPoint origin)
{
	if (!fRoot) {
		int x = origin.x;
		int y = origin.y;
        UnlockLooper();
		DoCallback(fPlatformWindow, "eventMove", "(II)V", x, y);
		LockLooper();
	}
}


void
PlatformView::FrameResized(float width, float height)
{
	int w = width + 1;
	int h = height + 1;

	if (fDrawable.Lock()) {
		if (!fDrawable.IsValid()
				|| w > fDrawable.Width() || h > fDrawable.Height()) {
			fDrawable.Allocate(w + 100, h + 100);
		}
		fDrawable.Unlock();
	}
	
	if (!fRoot) {
        UnlockLooper();
		DoCallback(fPlatformWindow, "eventResize", "(II)V", w, h);
		LockLooper();
	}
}


void
PlatformView::KeyDown(const char* bytes, int32 numBytes)
{
	_HandleKeyEvent(Window()->CurrentMessage());
}


void
PlatformView::KeyUp(const char* bytes, int32 numBytes)
{
	_HandleKeyEvent(Window()->CurrentMessage());
}


void
PlatformView::MakeFocus(bool focused)
{
	DoCallback(fPlatformWindow, "eventFocus", "(Z)V", focused);
	BView::MakeFocus(focused);
}


void
PlatformView::MessageReceived(BMessage* message)
{
	switch (message->what) {
		case B_UNMAPPED_KEY_DOWN:
		case B_UNMAPPED_KEY_UP:
			_HandleKeyEvent(message);
			break;
		case B_MOUSE_WHEEL_CHANGED:
			_HandleWheelEvent(message);
			break;
		default:
			break;
	}
	BView::MessageReceived(message);
}


void
PlatformView::MouseDown(BPoint point)
{
	_HandleMouseEvent(Window()->CurrentMessage(), point);
	BView::MouseDown(point);
}


void
PlatformView::MouseMoved(BPoint point, uint32 transit, const BMessage* message)
{
	_HandleMouseEvent(Window()->CurrentMessage(), point, transit);
	BView::MouseMoved(point, transit, message);
}


void
PlatformView::MouseUp(BPoint point)
{
	_HandleMouseEvent(Window()->CurrentMessage(), point);
	BView::MouseUp(point);
}


void
PlatformView::_HandleKeyEvent(BMessage* message)
{
	int64 when = 0;
	message->FindInt64("when", &when);
	int32 modifiers = 0;
	message->FindInt32("modifiers", &modifiers);
	int32 key = 0;
	message->FindInt32("key", &key);	

	jint id;
	jint mods = 0;
	jint keyCode = java_awt_event_KeyEvent_VK_UNDEFINED;
	jint keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;
	jchar keyChar = java_awt_event_KeyEvent_CHAR_UNDEFINED;

	if (message->what == B_KEY_DOWN || message->what == B_UNMAPPED_KEY_DOWN)
		id = java_awt_event_KeyEvent_KEY_PRESSED;
	else
		id = java_awt_event_KeyEvent_KEY_RELEASED;

	mods = ConvertInputModifiersToJava(modifiers);
	ConvertKeyCodeToJava(key, modifiers, &keyCode, &keyLocation);
	DoCallback(fPlatformWindow, "eventKey", "(IJIII)V", id,
		(jlong)(when / 1000), mods, keyCode, keyLocation);

	BString bytes;
	if (message->FindString("bytes", &bytes) == B_OK) {
		// Don't really get what this is about
		// I just copied it from the 1.4.2 port  vvvvvvvvv
		
		// If we hava a key field that's non-zero, respond on KEY_UP.
		// If we don't have a key field (key is zero), we need to fire on KEY_DOWN.
		if ((key != 0 && (message->what == B_KEY_UP || message->what == B_UNMAPPED_KEY_UP)) ||
		    (key == 0 && (message->what == B_KEY_DOWN || message->what == B_UNMAPPED_KEY_DOWN)))
		{
			id = java_awt_event_KeyEvent_KEY_TYPED;
			keyCode = java_awt_event_KeyEvent_VK_UNDEFINED;
			keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;
			
    		JNIEnv* env = NULL;
    		jvm->AttachCurrentThread((void**)&env, NULL);
			jstring keyChar = env->NewStringUTF(bytes.String());
			if (keyChar == NULL)
				return;

			DoCallback(fPlatformWindow, "eventKeyTyped",
				"(JILjava/lang/String;)V", (jlong)(when / 1000), mods, keyChar);
			env->DeleteLocalRef(keyChar);
		}
	}
}


void
PlatformView::_HandleMouseEvent(BMessage* message, BPoint point, uint32 transit)
{
	int64 when = 0;
	message->FindInt64("when", &when);
	int32 buttons = 0;
	message->FindInt32("buttons", &buttons);
	int32 clicks = 0;
	message->FindInt32("clicks", &clicks);
	int32 modifiers = 0;
	if (message->FindInt32("modifiers", &modifiers) != B_OK)
		modifiers = ::modifiers();

	int pressed = buttons & ~fPreviousButtons;
	int released = ~buttons & fPreviousButtons;
	fPreviousButtons = buttons;

	jint javaPressed = 0;
	if (pressed & B_PRIMARY_MOUSE_BUTTON)
		javaPressed |= java_awt_event_MouseEvent_BUTTON1_DOWN_MASK;
	if (pressed & B_SECONDARY_MOUSE_BUTTON)
		javaPressed |= java_awt_event_MouseEvent_BUTTON2_DOWN_MASK;
	if (pressed & B_TERTIARY_MOUSE_BUTTON)
		javaPressed |= java_awt_event_MouseEvent_BUTTON3_DOWN_MASK;

	jint javaReleased = 0;
	if (released & B_PRIMARY_MOUSE_BUTTON)
		javaReleased |= java_awt_event_MouseEvent_BUTTON1_DOWN_MASK;
	if (released & B_SECONDARY_MOUSE_BUTTON)
		javaReleased |= java_awt_event_MouseEvent_BUTTON2_DOWN_MASK;
	if (released & B_TERTIARY_MOUSE_BUTTON)
		javaReleased |= java_awt_event_MouseEvent_BUTTON3_DOWN_MASK;

	jint id = 0;
	switch (message->what) {
		case B_MOUSE_DOWN:
			id = java_awt_event_MouseEvent_MOUSE_PRESSED;
			break;
		case B_MOUSE_UP:
			id = java_awt_event_MouseEvent_MOUSE_RELEASED;
			break;
		case B_MOUSE_MOVED:
			if (transit == B_ENTERED_VIEW)
				id = java_awt_event_MouseEvent_MOUSE_ENTERED;
			else if (transit == B_EXITED_VIEW)
				id = java_awt_event_MouseEvent_MOUSE_EXITED;
			else {
				if (buttons != 0)
					id = java_awt_event_MouseEvent_MOUSE_DRAGGED;
				else
					id = java_awt_event_MouseEvent_MOUSE_MOVED;
			}
			break;
		default:
			break;
	}

	jint mods = ConvertInputModifiersToJava(modifiers);
	if (buttons & B_PRIMARY_MOUSE_BUTTON)
		mods |= java_awt_event_MouseEvent_BUTTON1_DOWN_MASK;
	if (buttons & B_SECONDARY_MOUSE_BUTTON)
		mods |= java_awt_event_MouseEvent_BUTTON2_DOWN_MASK;
	if (buttons & B_TERTIARY_MOUSE_BUTTON)
		mods |= java_awt_event_MouseEvent_BUTTON3_DOWN_MASK;

	// Drop the lock when doing this callback, because the MouseEvent
	// constructor will call getLocationOnScreen which may call native code.
	UnlockLooper();
	DoCallback(fPlatformWindow, "eventMouse", "(IJIIIIII)V", id,
		(jlong)(when / 1000), mods, (jint)point.x, (jint)point.y,
		(jint)clicks, javaPressed, javaReleased);
	LockLooper();
}


void
PlatformView::_HandleWheelEvent(BMessage* message)
{
	int64 when = 0;
	message->FindInt64("when", &when);
	int32 modifiers = ::modifiers();
	
	uint32 buttons = 0;
	BPoint point;
	GetMouse(&point, &buttons);

	jint mods = ConvertInputModifiersToJava(modifiers);
	if (buttons & B_PRIMARY_MOUSE_BUTTON)
		mods |= java_awt_event_MouseEvent_BUTTON1_DOWN_MASK;
	if (buttons & B_SECONDARY_MOUSE_BUTTON)
		mods |= java_awt_event_MouseEvent_BUTTON2_DOWN_MASK;
	if (buttons & B_TERTIARY_MOUSE_BUTTON)
		mods |= java_awt_event_MouseEvent_BUTTON3_DOWN_MASK;

	float wheelRotation = 0;
	message->FindFloat("be:wheel_delta_y", &wheelRotation);
	
	jint scrollType = java_awt_event_MouseWheelEvent_WHEEL_UNIT_SCROLL;
	if ((modifiers & (B_OPTION_KEY | B_COMMAND_KEY | B_CONTROL_KEY)) != 0)
	    scrollType = java_awt_event_MouseWheelEvent_WHEEL_BLOCK_SCROLL;

	jint scrollAmount = 3;
	UnlockLooper();
	DoCallback(fPlatformWindow, "eventWheel", "(JIIIIII)V",
		(jlong)(when / 1000), mods, (jint)point.x, (jint)point.y, scrollType,
		scrollAmount, (jint)wheelRotation);
	LockLooper();
}
