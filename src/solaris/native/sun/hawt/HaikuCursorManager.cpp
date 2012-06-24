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
#include <jni_util.h>

#include "java_awt_Cursor.h"

#include <Application.h>
#include <Cursor.h>
#include <InterfaceDefs.h>
#include <Point.h>

#include "Utilities.h"

static jfieldID pointXField;
static jfieldID pointYField;

extern "C" {

/*
 * Class:     sun_hawt_HaikuCursorManager
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuCursorManager_initIDs(JNIEnv *env, jclass clazz)
{
    jclass pointClazz = env->FindClass("java/awt/Point");
    pointXField = env->GetFieldID(pointClazz, "x", "I");
    pointYField = env->GetFieldID(pointClazz, "y", "I");
}

/*
 * Class:     sun_hawt_HaikuCursorManager
 * Method:    nativeGetCursorPosition
 * Signature: (Ljava/awt/Point;)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuCursorManager_nativeGetCursorPosition(JNIEnv* env,
    jclass clazz, jobject point)
{
    BPoint location;
    if (get_mouse(&location, NULL) == B_OK) {
        env->SetIntField(point, pointXField, (jint)location.x);
        env->SetIntField(point, pointYField, (jint)location.y);
    }
}

/*
 * Class:     sun_hawt_HaikuCursorManager
 * Method:    nativeSetCursor
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuCursorManager_nativeSetCursor(JNIEnv* env, jclass clazz,
	jint type)
{
	BCursorID id;

	switch (type) {
		case java_awt_Cursor_CROSSHAIR_CURSOR:
			id = B_CURSOR_ID_CROSS_HAIR;
			break;
		case java_awt_Cursor_DEFAULT_CURSOR:
			id = B_CURSOR_ID_SYSTEM_DEFAULT;
			break;
		case java_awt_Cursor_E_RESIZE_CURSOR:
		case java_awt_Cursor_W_RESIZE_CURSOR:
			id = B_CURSOR_ID_RESIZE_EAST_WEST;
			break;
		case java_awt_Cursor_HAND_CURSOR:
			// Not sure which one to go with here...
			id = B_CURSOR_ID_FOLLOW_LINK;
			break;
		case java_awt_Cursor_MOVE_CURSOR:
			id = B_CURSOR_ID_MOVE;
			break;
		case java_awt_Cursor_N_RESIZE_CURSOR:
		case java_awt_Cursor_S_RESIZE_CURSOR:
			id = B_CURSOR_ID_RESIZE_NORTH_SOUTH;
			break;
		case java_awt_Cursor_NE_RESIZE_CURSOR:
		case java_awt_Cursor_SW_RESIZE_CURSOR:
			id = B_CURSOR_ID_RESIZE_NORTH_EAST_SOUTH_WEST;
			break;
		case java_awt_Cursor_NW_RESIZE_CURSOR:
		case java_awt_Cursor_SE_RESIZE_CURSOR:
			id = B_CURSOR_ID_RESIZE_NORTH_WEST_SOUTH_EAST;
			break;
		case java_awt_Cursor_TEXT_CURSOR:
			id = B_CURSOR_ID_I_BEAM;
			break;
		case java_awt_Cursor_WAIT_CURSOR:
			id = B_CURSOR_ID_PROGRESS;
			break;
		default:
			id = B_CURSOR_ID_SYSTEM_DEFAULT;
			break;
	}

	BCursor cursor(id);
	be_app->SetCursor(&cursor);
}

}
