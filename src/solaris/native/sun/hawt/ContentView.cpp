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

#include "ContentView.h"

#include <String.h>
#include <Window.h>

#include "java_awt_event_KeyEvent.h"
#include "java_awt_event_MouseEvent.h"
#include "java_awt_event_MouseWheelEvent.h"

#include "HaikuPlatformWindow.h"

ContentView::ContentView(jobject platformWindow)
	:
	BView(BRect(0, 0, 0, 0), NULL, B_FOLLOW_ALL,
		B_WILL_DRAW | B_FRAME_EVENTS | B_NAVIGABLE),
	fDrawable(this),
	fPlatformWindow(platformWindow)
{
	get_mouse(NULL, &fPreviousButtons);
}


Drawable*
ContentView::GetDrawable()
{
	return &fDrawable;
}


void
ContentView::Focus()
{
	LockLooper();
	MakeFocus(true);
	UnlockLooper();
}


void
ContentView::DeferredDraw(BRect updateRect)
{
	//fDefferedDraw = true;
	//Invalidate(updateRect);
	printf("1 Drawing in %d %d %ld %ld\n", (int)updateRect.left, (int)updateRect.top, updateRect.IntegerWidth() + 1, updateRect.IntegerHeight() + 1);
	// updateRect is in frame coords
	BRect viewRect = ((PlatformWindow*)Window())
		->ViewFromFrame(updateRect);
	// viewRect is in view coords
	if (!fDrawable.Lock())
		return;
	if (fDrawable.IsValid())
		DrawBitmapAsync(fDrawable.GetBitmap(), updateRect, viewRect);
	fDrawable.Unlock();
}


void
ContentView::Draw(BRect updateRect)
{
	// updateRect is in view coords
	BRect rect = ((PlatformWindow*)Window())->ViewToFrame(updateRect);
	// rect is in frame coords
	DeferredDraw(rect);

	//if (!fDeferredDraw) {
		jint x = rect.left;
		jint y = rect.top;
		jint width = rect.IntegerWidth() + 1;
		jint height = rect.IntegerHeight() + 1;
		DoCallback(fPlatformWindow, "eventRepaint", "(IIII)V", x, y, width, height);
	//} else
	//	fDeferredDraw = false;
}


void
ContentView::FrameResized(float width, float height)
{
	int w = width + 1;
	int h = height + 1;

    // todo review resize heuristic make function
	if (fDrawable.Lock()) {
		if (!fDrawable.IsValid()
				|| w > fDrawable.Width() || h > fDrawable.Height()) {
			fDrawable.Allocate(w + 100, h + 100);
		}
		fDrawable.Unlock();
	}
}


void
ContentView::KeyDown(const char* bytes, int32 numBytes)
{
	_HandleKeyEvent(Window()->CurrentMessage());
}


void
ContentView::KeyUp(const char* bytes, int32 numBytes)
{
	_HandleKeyEvent(Window()->CurrentMessage());
}


void
ContentView::MakeFocus(bool focused)
{
	DoCallback(fPlatformWindow, "eventActivate", "(Z)V", focused);
	BView::MakeFocus(focused);
}


void
ContentView::MessageReceived(BMessage* message)
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
ContentView::MouseDown(BPoint point)
{
	_HandleMouseEvent(Window()->CurrentMessage(), point);
	BView::MouseDown(point);
}


void
ContentView::MouseMoved(BPoint point, uint32 transit, const BMessage* message)
{
	_HandleMouseEvent(Window()->CurrentMessage(), point, transit);
	BView::MouseMoved(point, transit, message);
}


void
ContentView::MouseUp(BPoint point)
{
	_HandleMouseEvent(Window()->CurrentMessage(), point);
	BView::MouseUp(point);
}


void
ContentView::_HandleKeyEvent(BMessage* message)
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
ContentView::_HandleMouseEvent(BMessage* message, BPoint point, uint32 transit)
{
	BPoint screenPoint = ConvertToScreen(point);
	BPoint framePoint = ((PlatformWindow*)Window())->ViewToFrame(point);
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

	jint javaPressed = ConvertButtonsToJava(pressed);
	jint javaReleased = ConvertButtonsToJava(released);
	jint javaButtons = ConvertButtonsToJava(buttons);

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

	jint mods = ConvertInputModifiersToJava(modifiers) | javaButtons;

	// Drop the lock when doing this callback, because the MouseEvent
	// constructor will call getLocationOnScreen which may call native code.
	UnlockLooper();
	DoCallback(fPlatformWindow, "eventMouse", "(IJIIIIIIIII)V", id,
		(jlong)(when / 1000), mods, (jint)framePoint.x, (jint)framePoint.y,
		(jint)screenPoint.x, (jint)screenPoint.y, (jint)clicks, javaPressed,
		javaReleased, javaButtons);
	LockLooper();
}


void
ContentView::_HandleWheelEvent(BMessage* message)
{
	int64 when = 0;
	message->FindInt64("when", &when);
	int32 modifiers = ::modifiers();
	
	uint32 buttons = 0;
	BPoint point;
	GetMouse(&point, &buttons);
	BPoint framePoint = ((PlatformWindow*)Window())->ViewToFrame(point);

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
	DoCallback(fPlatformWindow, "eventWheel", "(JIIIIIID)V",
		(jlong)(when / 1000), mods, (jint)framePoint.x, (jint)framePoint.y,
		scrollType, scrollAmount, (jint)wheelRotation, (jdouble)wheelRotation);
	LockLooper();
}
