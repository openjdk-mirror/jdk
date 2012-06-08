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

#include <jni.h>

#include <Screen.h>

#include "Utilities.h"

static jfieldID rectXField;
static jfieldID rectYField;
static jfieldID rectWidthField;
static jfieldID rectHeightField;

extern "C" {

/*
 * Class:     sun_hawt_HaikuGraphicsConfig
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuGraphicsConfig_initIDs(JNIEnv *env, jclass clazz)
{
    jclass rectangleClazz = env->FindClass("java/awt/Rectangle");
    rectXField = env->GetFieldID(rectangleClazz, "x", "I");
    rectYField = env->GetFieldID(rectangleClazz, "y", "I");
    rectWidthField = env->GetFieldID(rectangleClazz, "width", "I");
    rectHeightField = env->GetFieldID(rectangleClazz, "height", "I");
}

/*
 * Class:     sun_hawt_HaikuGraphicsConfig
 * Method:    nativeGetBounds
 * Signature: (ILjava/awt/Rectangle;)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuGraphicsConfig_nativeGetBounds(JNIEnv *env,
	jclass clazz, jint displayID, jobject bounds)
{
	screen_id id;
	id.id = displayID;

    BScreen screen(id);
    if (!screen.IsValid()) {
    	return;
    }

    BRect frame = screen.Frame();
    env->SetIntField(bounds, rectXField, frame.left);
    env->SetIntField(bounds, rectYField, frame.top);
    env->SetIntField(bounds, rectWidthField, frame.IntegerWidth() + 1);
    env->SetIntField(bounds, rectHeightField, frame.IntegerHeight() + 1);
}

}
