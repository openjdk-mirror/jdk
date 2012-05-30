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
#include <View.h>
#include <Window.h>


static jfieldID pointXField;
static jfieldID pointYField;
static jfieldID rectXField;
static jfieldID rectYField;
static jfieldID rectWidthField;
static jfieldID rectHeightField;

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

	if (!window->LockLooper())
		return;
	window->MoveTo(x, y);
	window->ResizeTo(width - 1, height - 1);
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
	window->UnlockLooper();

	env->SetIntField(point, pointXField, (jint)frame.left);
	env->SetIntField(point, pointYField, (jint)frame.top);
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
	jlong nativeWindow, jlong menuBarPtr)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	BMenuBar* menuBar = (BMenuBar*)menuBarPtr;

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

}


/*
 * TODO:
 * Get/set maximize bounds
 * Blocking
 */

PlatformWindow::PlatformWindow(jobject platformWindow, bool decorated)
	:
	BWindow(BRect(0, 0, 0, 0), NULL, decorated ? B_TITLED_WINDOW_LOOK
		: B_NO_BORDER_WINDOW_LOOK, B_NORMAL_WINDOW_FEEL,
		decorated ? 0 : B_AVOID_FOCUS),
	fView(platformWindow),
	fPlatformWindow(platformWindow)
{
	AddChild(&fView);

	// After this initial bounds set the view will size itself
	// to match the frame
	BRect frame = Bounds();
	fView.MoveTo(frame.left, frame.top);
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
	if (fMenuBar != NULL)
		RemoveChild(fMenuBar);

	// todo use layout? how easy is it to change layout?
	if (menuBar != NULL) {
		AddChild(fMenuBar);
		BRect bounds = menuBar->Bounds();
		fView.MoveTo(0, bounds.bottom + 1);
	} else {
		fView.MoveTo(0, 0);
	}

	fMenuBar = menuBar;
	
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
	int x = origin.x;
	int y = origin.y;
	UnlockLooper();
	DoCallback(fPlatformWindow, "eventMove", "(II)V", x, y);
	LockLooper();
	BWindow::FrameMoved(origin);
}


void
PlatformWindow::FrameResized(float width, float height)
{
	int w = width + 1;
	int h = height + 1;
	UnlockLooper();
	DoCallback(fPlatformWindow, "eventResize", "(II)V", w, h);
	LockLooper();
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
PlatformWindow::WindowActivated(bool active)
{
	DoCallback(fPlatformWindow, "eventActivate", "(Z)V", active);
	BWindow::WindowActivated(active);
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
