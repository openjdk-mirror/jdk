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

#include <jlong.h>
#include <jni.h>

#include "HaikuPlatformWindow.h"

#include "sun_hawt_HaikuPlatformWindow.h"
#include "java_awt_Frame.h"

#include <kernel/OS.h>
#include <MenuBar.h>
#include <MenuItem.h>
#include <View.h>
#include <Window.h>

// The amount of extra size we give the drawable
// so we're not reallocating it all the time
static const int kResizeBuffer = 100;

static jfieldID pointXField;
static jfieldID pointYField;
static jfieldID rectXField;
static jfieldID rectYField;
static jfieldID rectWidthField;
static jfieldID rectHeightField;
static jfieldID insetsLeftField;
static jfieldID insetsTopField;
static jfieldID insetsRightField;
static jfieldID insetsBottomField;

extern "C" {


extern sem_id appSem;


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
    jclass pointClazz = env->FindClass("java/awt/Point");
    pointXField = env->GetFieldID(pointClazz, "x", "I");
    pointYField = env->GetFieldID(pointClazz, "y", "I");

    jclass rectClazz = env->FindClass("java/awt/Rectangle");
    rectXField = env->GetFieldID(rectClazz, "x", "I");
    rectYField = env->GetFieldID(rectClazz, "y", "I");
    rectWidthField = env->GetFieldID(rectClazz, "width", "I");
    rectHeightField = env->GetFieldID(rectClazz, "height", "I");

    jclass insetsClazz = env->FindClass("java/awt/Insets");
    insetsLeftField = env->GetFieldID(insetsClazz, "left", "I");
    insetsTopField = env->GetFieldID(insetsClazz, "top", "I");
    insetsRightField = env->GetFieldID(insetsClazz, "right", "I");
    insetsBottomField = env->GetFieldID(insetsClazz, "bottom", "I");
}


JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeInit(JNIEnv *env, jobject thiz,
	jboolean simpleWindow)
{
	// Wait for be_app to get created
	acquire_sem(appSem);
	release_sem(appSem);

	jobject javaWindow = env->NewWeakGlobalRef(thiz);
	PlatformWindow* window = new PlatformWindow(javaWindow,	simpleWindow);
	return ptr_to_jlong(window);
}


JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeGetDrawable
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	return ptr_to_jlong(window->GetDrawable());
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetBounds(JNIEnv *env, jobject thiz,
	jlong nativeWindow, jint x, jint y, jint width, jint height)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	BRect frameRect = BRect(x, y, x + width - 1, y + height - 1);

	if (!window->LockLooper())
		return;

	// given coordinates include the decorator frame, transform to
	// the client area
	BRect rect = window->TransformFromFrame(frameRect);
	window->MoveTo(rect.left, rect.top);
	window->ResizeTo(rect.IntegerWidth(), rect.IntegerHeight());
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetVisible(JNIEnv *env, jobject thiz,
	jlong nativeWindow, jboolean visible)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;

	if (visible) {
		while (window->IsHidden())
			window->Show();
	} else {
		while (!window->IsHidden())
			window->Hide();
	}
	window->UnlockLooper();
}

JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeGetLocation
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jobject point)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	
	if (!window->LockLooper())
		return;
	BRect frame = window->Frame();
    BPoint location = window->TranslateToFrame(BPoint(frame.left, frame.top));
	window->UnlockLooper();

	env->SetIntField(point, pointXField, (jint)location.x);
	env->SetIntField(point, pointYField, (jint)location.y);
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeDispose
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	
	window->LockLooper();
	window->Dispose(env);
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeFocus
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;
	window->Focus();
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetWindowState(JNIEnv *env,
	jobject thiz, jlong nativeWindow, jint windowState)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;
	window->SetState(windowState);
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetResizable
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jboolean resizable)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;
	if (resizable)
		window->SetFlags(window->Flags() | B_NOT_RESIZABLE);
	else
		window->SetFlags(window->Flags() & ~B_NOT_RESIZABLE);
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetTitle
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jstring title)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	const char* name = env->GetStringUTFChars(title, NULL);
	if (name == NULL)
		return;

	if (!window->LockLooper())
		return;
	window->SetTitle(name);
	window->UnlockLooper();
	env->ReleaseStringUTFChars(title, name);
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeToFront
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;
	window->Activate();
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeToBack
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;
	window->SendBehind(NULL);
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetMenuBar(JNIEnv *env, jobject thiz,
	jlong nativeWindow, jlong menuBarItemPtr)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	BMenuItem* menuBarItem = (BMenuItem*)jlong_to_ptr(menuBarItemPtr);
	BMenuBar* menuBar = (BMenuBar*)menuBarItem->Submenu();

	if (!window->LockLooper())
		return;
	window->SetMenuBar(menuBar);
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetMinimumSize(JNIEnv *env,
	jobject thiz, jlong nativeWindow, jint width, jint height)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;
	float maxWidth, maxHeight;
	window->GetSizeLimits(NULL, &maxWidth, NULL, &maxHeight);
	window->SetSizeLimits(width, maxWidth, height, maxHeight);
	window->UnlockLooper();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeGetInsets(JNIEnv *env,
	jobject thiz, jlong nativeWindow, jobject javaInsets)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);

	if (!window->LockLooper())
		return;
	Insets insets = window->GetInsets();
	window->UnlockLooper();

	env->SetIntField(javaInsets, insetsLeftField, (jint)insets.left);
	env->SetIntField(javaInsets, insetsTopField, (jint)insets.top);
	env->SetIntField(javaInsets, insetsRightField, (jint)insets.right);
	env->SetIntField(javaInsets, insetsBottomField, (jint)insets.bottom);
}


}


PlatformWindow::PlatformWindow(jobject platformWindow, bool simpleWindow)
	:
	BWindow(BRect(0, 0, 0, 0), NULL, simpleWindow ? B_NO_BORDER_WINDOW_LOOK
		: B_TITLED_WINDOW_LOOK, B_NORMAL_WINDOW_FEEL,
		simpleWindow ? B_AVOID_FOCUS : 0),
	fView(platformWindow),
	fPlatformWindow(platformWindow),
	fMenuBar(NULL),
	fInsets(GetInsets())
{
	AddChild(&fView);

	// After this initial bounds set the view will size itself
	// to match the frame
	BRect frame = Bounds();
	fView.MoveTo(0, 0);
	fView.ResizeTo(frame.IntegerWidth(), frame.IntegerHeight());
}


Drawable*
PlatformWindow::GetDrawable()
{
	return fView.GetDrawable();
}


void
PlatformWindow::SetState(int state)
{
	// Should a maximize cancel out a minimize?
	// Or should it be 'behind-the-scenes' maximized,
	// so it shows as maximized when it becomes unminimized?
	
	if ((state & java_awt_Frame_ICONIFIED) != 0)
		Minimize(true);

	if ((state & java_awt_Frame_MAXIMIZED_BOTH) != 0) {
		if (!fMaximized)
			BWindow::Zoom();
	}

	// Normal should cancel out the two other states
	if ((state & java_awt_Frame_NORMAL) != 0) {
		Minimize(false);
		if (fMaximized)
			BWindow::Zoom();
	}
}


void
PlatformWindow::Dispose(JNIEnv* env)
{
	RemoveChild(&fView);
	env->DeleteWeakGlobalRef(fPlatformWindow);
	Quit();
}


void
PlatformWindow::SetMenuBar(BMenuBar* menuBar)
{
	if (menuBar != NULL) {
		AddChild(menuBar);
		BRect bounds = menuBar->Bounds();
		fView.MoveTo(0, bounds.bottom + 1);
	} else {
		fView.MoveTo(0, 0);
	}

	if (fMenuBar != NULL)
		RemoveChild(fMenuBar);
	fMenuBar = menuBar;

	// The insets probably changed
	_UpdateInsets();
}


Insets
PlatformWindow::GetInsets()
{
	float borderWidth = 5.0;
	float tabHeight = 21.0;

	BMessage settings;
	if (GetDecoratorSettings(&settings) == B_OK) {
		BRect tabRect;
		if (settings.FindRect("tab frame", &tabRect) == B_OK)
			tabHeight = tabRect.Height();
		settings.FindFloat("border width", &borderWidth);
	} else {
		// probably no-border window look
		if (Look() == B_NO_BORDER_WINDOW_LOOK) {
			borderWidth = 0.0;
			tabHeight = 0.0;
		}
		// else use fall-back values from above
	}

	int menuHeight = 0;
	if (fMenuBar != NULL) {
		BRect bounds = fMenuBar->Bounds();
		menuHeight = bounds.IntegerHeight() + 1;
	}

	// +1's here?
	return Insets(borderWidth, tabHeight + borderWidth + menuHeight,
		borderWidth, borderWidth);
}

void
PlatformWindow::Focus()
{
	Activate();
	fView.Focus();
}


void
PlatformWindow::FrameMoved(BPoint origin)
{
	_Reshape(false);
	BWindow::FrameMoved(origin);
}


void
PlatformWindow::FrameResized(float width, float height)
{
	_Reshape(true);
	BWindow::FrameResized(width, height);
}


void
PlatformWindow::Minimize(bool minimize)
{
	DoCallback(fPlatformWindow, "eventMinimize", "(Z)V", minimize);
	BWindow::Minimize(minimize);
}


bool
PlatformWindow::QuitRequested()
{
	DoCallback(fPlatformWindow, "eventWindowClosing", "()V");
	
	// According to WindowEvent docs, we should ignore the
	// user's request to quit and send an event to the peer.
	// AWT will then decide what to do.
	return false;
}


void
PlatformWindow::Zoom(BPoint origin, float width, float height)
{
	// For whatever reason, there is no getter for this
	// so we record the state ourselves.
	fMaximized = !fMaximized;
	DoCallback(fPlatformWindow, "eventMaximize", "(Z)V", fMaximized);
	
	BWindow::Zoom(origin, width, height);
}


BRect
PlatformWindow::ViewFromFrame(BRect rect)
{
	return BRect(rect.left - fInsets.left, rect.top - fInsets.top,
		rect.right - fInsets.left, rect.bottom - fInsets.top);
}


BRect
PlatformWindow::ViewToFrame(BRect rect)
{
	return BRect(rect.left + fInsets.left, rect.top + fInsets.top,
		rect.right + fInsets.left, rect.bottom + fInsets.top);
}


BRect
PlatformWindow::TransformToFrame(BRect rect)
{
	int topInsets = fInsets.top;
	if (fMenuBar != NULL)
		topInsets -= fMenuBar->Bounds().IntegerHeight() + 1;

	return BRect(rect.left - fInsets.left, rect.top - topInsets,
		rect.right + fInsets.right, rect.bottom + fInsets.bottom);
}


BRect
PlatformWindow::TransformFromFrame(BRect rect)
{
	int topInsets = fInsets.top;
	if (fMenuBar != NULL)
		topInsets -= fMenuBar->Bounds().IntegerHeight() + 1;

	return BRect(rect.left + fInsets.left, rect.top + topInsets,
		rect.right - fInsets.left, rect.bottom - topInsets);
}


BPoint
PlatformWindow::TranslateToFrame(BPoint point)
{
	int topInsets = fInsets.top;
	if (fMenuBar != NULL)
		topInsets -= fMenuBar->Bounds().IntegerHeight() + 1;

	return BPoint(point.x - fInsets.left, point.y - topInsets);
}

void
PlatformWindow::_Reshape(bool resize)
{
	BRect bounds = Frame();

	// transform bounds to include the decorations
	BRect frame = TransformToFrame(bounds);
	int x = frame.left;
	int y = frame.top;
	int width = frame.IntegerWidth() + 1;
	int height = frame.IntegerHeight() + 1;

	int w = width + 1;
	int h = height + 1;

	if (resize) {
		Drawable* drawable = fView.GetDrawable();
		if (drawable->Lock()) {
			if (!drawable->IsValid()
					|| w > drawable->Width()
					|| h > drawable->Height()
					|| w + kResizeBuffer * 2 < drawable->Width()
					|| h + kResizeBuffer * 2 < drawable->Height()) {
				drawable->Allocate(w + kResizeBuffer, h + kResizeBuffer);
			}
			drawable->Unlock();
		}
	}

	DoCallback(fPlatformWindow, "eventReshape", "(IIII)V", x, y, width, height);
}


void
PlatformWindow::_UpdateInsets()
{
	fInsets = GetInsets();
	DoCallback(fPlatformWindow, "updateInsets", "(IIII)V", fInsets.left,
		fInsets.top, fInsets.right, fInsets.bottom);
}
