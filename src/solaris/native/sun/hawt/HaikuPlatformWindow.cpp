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

#include "sun_hawt_HaikuPlatformWindow.h"

#include <kernel/OS.h>
#include <View.h>
#include <Window.h>

#include "HaikuPlatformWindow.h"
#include "PlatformFrame.h"
#include "PlatformView.h"

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
    if (env->ExceptionCheck())
    	return;
    pointXField = env->GetFieldID(pointClazz, "x", "I");
    if (env->ExceptionCheck()) 
    	return;
    pointYField = env->GetFieldID(pointClazz, "y", "I");
    if (env->ExceptionCheck()) 
    	return;

    jclass rectClazz = env->FindClass("java/awt/Rectangle");
    if (env->ExceptionCheck())
    	return;
    rectXField = env->GetFieldID(rectClazz, "x", "I");
    if (env->ExceptionCheck()) 
    	return;
    rectYField = env->GetFieldID(rectClazz, "y", "I");
    if (env->ExceptionCheck())
    	return;
    rectWidthField = env->GetFieldID(rectClazz, "width", "I");
    if (env->ExceptionCheck()) 
    	return;
    rectHeightField = env->GetFieldID(rectClazz, "height", "I");
}

JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeInitFrame
  (JNIEnv *env, jobject thiz, jint x, jint y, jint width, jint height,
   jboolean decorated)
{
	// Wait for be_app to get created
	acquire_sem(appSem);
	release_sem(appSem);

	// TODO release global ref in frame/view dispose
	jobject javaWindow = env->NewWeakGlobalRef(thiz);
	PlatformWindow* window = new PlatformFrame(javaWindow,
		decorated == JNI_TRUE);
	window->SetBounds(Rectangle(x, y, width, height));
	return ptr_to_jlong(window);
}


JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeInitView
  (JNIEnv *env, jobject thiz, jint x, jint y, jint width, jint height,
   jlong parent)
{
	// Wait for be_app to get created
	acquire_sem(appSem);
	release_sem(appSem);

	// TODO release global ref in frame/view dispose
	jobject javaWindow = env->NewWeakGlobalRef(thiz);
	PlatformWindow* window = new PlatformView(javaWindow, false);

	PlatformWindow* parentWindow = (PlatformWindow*)jlong_to_ptr(parent);
	if (parentWindow != NULL) {
		PlatformView* parentContainer = parentWindow->GetContainer();
		window->SetParent(parentContainer);
	}

	window->SetBounds(Rectangle(x, y, width, height));
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
Java_sun_hawt_HaikuPlatformWindow_nativeGetBounds
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jobject bounds)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	Rectangle rect = window->GetBounds();

	env->SetIntField(bounds, rectXField, rect.x);
	env->SetIntField(bounds, rectYField, rect.y);
	env->SetIntField(bounds, rectWidthField, rect.width);
	env->SetIntField(bounds, rectHeightField, rect.height);
}

JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetBounds
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jint x, jint y,
   jint width, jint height)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	window->SetBounds(Rectangle(x, y, width, height));
}


JNIEXPORT jboolean JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeGetVisible
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	return window->GetVisible();
}

JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeSetVisible
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jboolean visible)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	window->SetVisible(visible == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeGetLocation
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jobject point)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	Point location = window->GetLocation();

	env->SetIntField(point, pointXField, location.x);
	env->SetIntField(point, pointYField, location.y);
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeGetLocationOnScreen
  (JNIEnv *env, jobject thiz, jlong nativeWindow, jobject point)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	Point location = window->GetLocationOnScreen();

	env->SetIntField(point, pointXField, location.x);
	env->SetIntField(point, pointYField, location.y);
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeDispose
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	window->Dispose();
}


JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPlatformWindow_nativeFocus
  (JNIEnv *env, jobject thiz, jlong nativeWindow)
{
	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	printf("Native focusing for: %p\n", window);
	window->Focus();
}


}
