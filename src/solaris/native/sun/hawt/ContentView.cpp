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
	fPlatformWindow(platformWindow),
	fDropTargetComponent(NULL),
	fDropTargetContext(NULL),
	fDragSourceContext(NULL)
{
//	SetEventMask(B_POINTER_EVENTS);
	get_mouse(&fPreviousPoint, &fPreviousButtons);
}


void
ContentView::AddDropTarget(jobject target)
{
	fDropTargetComponent = target;
}


void
ContentView::RemoveDropTarget()
{
	GetEnv()->DeleteWeakGlobalRef(fDropTargetComponent);
	fDropTargetComponent = NULL;
}


void
ContentView::StartDrag(BMessage* message, jobject dragSource)
{
	fDragSourceContext = dragSource;

	// TODO use the provided image instead of this rectangle
	BPoint mouse;
	get_mouse(&mouse, NULL);
	ConvertFromScreen(&mouse);
	BRect rect(mouse.x - 64, mouse.y - 64, mouse.x + 63, mouse.y + 63);

	// We add this field to the message so we can identify replies
	// that indicate that the drag is over.
	message->AddBool("java:drag_source_message", true);
	DragMessage(message, rect, this);
}


Drawable*
ContentView::GetDrawable()
{
	return &fDrawable;
}


void
ContentView::DeferredDraw(BRect frameRect)
{
	// frameRect is in decorator frame space and indicates which region
	// of the bitmap we need to paint. We need to find the corresponding
	// region of the content view
	BRect viewRect = ((PlatformWindow*)Window())->ViewFromFrame(frameRect);
	if (!fDrawable.Lock()) {
		return;
	}

	if (fDrawable.IsValid()) {
		DrawBitmapAsync(fDrawable.GetBitmap(), frameRect, viewRect);
	}

	fDrawable.Unlock();
}


void
ContentView::Draw(BRect updateRect)
{
	// updateRect is in view-space here. We need to translate to frame-space
	// in order to deal with the bitmap's insets
	BRect rect = ((PlatformWindow*)Window())->ViewToFrame(updateRect);
	DeferredDraw(rect);

	jint x = rect.left;
	jint y = rect.top;
	jint width = rect.IntegerWidth() + 1;
	jint height = rect.IntegerHeight() + 1;
	DoCallback(fPlatformWindow, "eventRepaint", "(IIII)V", x, y, width, height);
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
	BView::MakeFocus(focused);
}


DECLARE_JAVA_CLASS(dragSourceClazz, "sun/hawt/HaikuDragSourceContextPeer")


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
	
	// A reply is sent for our drag message -- either by the view it gets
	// dropped on or when the message is destroyed
	if (message->IsReply()) {
		if (message->Previous()->HasBool("java:drag_source_message")) {
			JNIEnv* env = GetEnv();
			DECLARE_VOID_JAVA_METHOD(dragDone, dragSourceClazz, "dragDone",
				"()V");
			env->CallVoidMethod(fDragSourceContext, dragDone);
		}
	}

	if (message->WasDropped() && fDropTargetComponent != NULL) {
		message->PrintToStream();
		_HandleDnDDrop(message);
	}

	BView::MessageReceived(message);
}


void
ContentView::MouseDown(BPoint point)
{
	SetMouseEventMask(B_POINTER_EVENTS, 0);
	_HandleMouseEvent(Window()->CurrentMessage(), point);
	BView::MouseDown(point);
}


void
ContentView::MouseMoved(BPoint point, uint32 transit, const BMessage* message)
{
	// If the mouse entered the view we should reset our previous buttons
	if (transit == B_ENTERED_VIEW)
		get_mouse(NULL, &fPreviousButtons);

	_HandleMouseEvent(Window()->CurrentMessage(), point, transit, message);
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
	if (message->what == B_KEY_DOWN
			&& message->FindString("bytes", &bytes) == B_OK) {
		id = java_awt_event_KeyEvent_KEY_TYPED;
		keyCode = java_awt_event_KeyEvent_VK_UNDEFINED;
		keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;
			
   		JNIEnv* env = GetEnv();
		jstring keyChar = env->NewStringUTF(bytes.String());
		if (keyChar == NULL)
			return;
		DoCallback(fPlatformWindow, "eventKeyTyped",
			"(JILjava/lang/String;)V", (jlong)(when / 1000), mods, keyChar);
		env->DeleteLocalRef(keyChar);
	}
}


void
ContentView::_HandleMouseEvent(BMessage* message, BPoint point, uint32 transit,
	const BMessage* dragMessage)
{
	// Get out early if this message is useless
	int32 buttons = 0;
	message->FindInt32("buttons", &buttons);
	int32 buttonChange = buttons ^ fPreviousButtons;
	if (point == fPreviousPoint && buttonChange == 0)
		return;

	fPreviousPoint = point;
	fPreviousButtons = buttons;

	BPoint screenPoint = ConvertToScreen(point);
	int64 when = 0;
	message->FindInt64("when", &when);
	int32 clicks = 0;
	message->FindInt32("clicks", &clicks);

   	//_HandleDnDMessage(transit, dragMessage, screenPoint.x, screenPoint.y);

	int32 modifiers = 0;
	if (message->FindInt32("modifiers", &modifiers) != B_OK)
		modifiers = ::modifiers();
	jint mods = ConvertInputModifiersToJava(modifiers)
		| ConvertMouseMaskToJava(buttons);

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

	jint button = java_awt_event_MouseEvent_NOBUTTON;
	if (id == java_awt_event_MouseEvent_MOUSE_PRESSED
			|| id == java_awt_event_MouseEvent_MOUSE_RELEASED)
		button = ConvertMouseButtonToJava(buttonChange);
	else if (id == java_awt_event_MouseEvent_MOUSE_DRAGGED)
		button = ConvertMouseButtonToJava(buttons);

	DoCallback(fPlatformWindow, "eventMouse", "(IJIIIIIII)V", id,
		(jlong)(when / 1000), mods, (jint)point.x, (jint)point.y,
		(jint)screenPoint.x, (jint)screenPoint.y, (jint)clicks, button);
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
	DoCallback(fPlatformWindow, "eventWheel", "(JIIIIIID)V",
		(jlong)(when / 1000), mods, (jint)point.x, (jint)point.y,
		scrollType, scrollAmount, (jint)wheelRotation, (jdouble)wheelRotation);
}


void
ContentView::_HandleDnDMessage(uint32 transit, const BMessage* dragMessage,
	int x, int y)
{
	if (fDropTargetComponent == NULL)
		return;

	JNIEnv* env = GetEnv();
	if (dragMessage == NULL) {
		// Clear out the drop target context if there's no drag message --
		// there must been a drop or an exit that we missed somehow
		if (fDropTargetContext != NULL) {
			// TODO Need to dispose context as it may hold a pointer to
			// a native message
			env->DeleteGlobalRef(fDropTargetContext);
			fDropTargetContext = NULL;
		}
		return;
	}

	if (transit == B_ENTERED_VIEW || fDropTargetContext == NULL) {
		// Note maybe store something in the message so we don't have to
		// recreate the context everytime a drag enters? Anything we add
		// to the drag message might not be retained though...

		// Get rid of the old drop target context
		if (fDropTargetContext != NULL) {
			// TODO Need to dispose context as it may hold a pointer to
			// a native message
			env->DeleteGlobalRef(fDropTargetContext);
			fDropTargetContext = NULL;
		}

		// Get a new drop target context for this action
		jclass clazz = env->FindClass("sun/hawt/HaikuDropTargetContextPeer");
		jmethodID getContext = env->GetStaticMethodID(clazz,
			"createDropTargetContextPeer",
			"(JIILjava/awt/Component;)Lsun/hawt/HaikuDropTargetContextPeer;");
		if (getContext == NULL) {
			// !!!
			return;
		}

		// We create a copy of the drag message, because java expects to be
		// able to query it for formats and data before it the user even
		// drops it
		BMessage* copyMessage = new BMessage(*dragMessage);
		// The static method handles the handle
		jobject localContext = env->CallStaticObjectMethod(clazz, getContext,
			ptr_to_jlong(copyMessage), (jint)x, (jint)y);
		fDropTargetContext = env->NewGlobalRef(localContext);
		env->DeleteLocalRef(localContext);
	} else if (transit == B_INSIDE_VIEW) {
		DoCallback(fDropTargetContext, "handleMotion", "(II)V", (jint)x,
			(jint)y);
	} else if (transit == B_EXITED_VIEW) {
		DoCallback(fDropTargetContext, "handleExit", "()V", (jint)x,
			(jint)y);

		// Now we need to get rid of the context as any re-entry could be
		// a new DnD operation
		env->DeleteGlobalRef(fDropTargetContext);
		fDropTargetContext = NULL;
	}
}


void
ContentView::_HandleDnDDrop(BMessage* message)
{
	// TODO

	// Hopefully this message matches the one already passed up
	// to the context. If it doesn't...!

	BPoint dropPoint = message->DropPoint();
	if (fDropTargetContext == NULL) {
		JNIEnv* env = GetEnv();

		// Get a new drop target context for this action
		jclass clazz = env->FindClass("sun/hawt/HaikuDropTargetContextPeer");
		jmethodID getContext = env->GetStaticMethodID(clazz,
			"createDropTargetContextPeer",
			"(JII)Lsun/hawt/HaikuDropTargetContextPeer;");
		if (getContext == NULL) {
			// !!!
			return;
		}

		// We create a copy of the drag message, because java expects to be
		// able to query it for formats and data before it the user even
		// drops it
		BMessage* copyMessage = new BMessage(*message);
		// The static method handles the handle
		jobject localContext = env->CallStaticObjectMethod(clazz, getContext,
			ptr_to_jlong(copyMessage), (jint)dropPoint.x, (jint)dropPoint.y);
		fDropTargetContext = env->NewGlobalRef(localContext);
		env->DeleteLocalRef(localContext);		
	}

	DoCallback(fDropTargetContext, "handleDrop", "(II)V", (jint)dropPoint.x,
		(jint)dropPoint.y);
	GetEnv()->DeleteGlobalRef(fDropTargetContext);
}
