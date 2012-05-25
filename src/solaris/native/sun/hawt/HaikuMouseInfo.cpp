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

#include <InterfaceDefs.h>
#include <Point.h>

extern "C" {

/*
 * Class:     sun_awt_DefaultMouseInfoPeer
 * Method:    fillPointWithCoords
 * Signature: (Ljava/awt/Point)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_DefaultMouseInfoPeer_fillPointWithCoords(JNIEnv* env,
    jclass clazz, jobject point)
{
    static jclass pointClass = NULL;
    jclass pointClassLocal;
    static jfieldID xID, yID;

    if (pointClass == NULL) {
        pointClassLocal = env->FindClass("java/awt/Point");
        if (pointClassLocal == NULL)
            return (jint)0;

        pointClass = (jclass)env->NewGlobalRef(pointClassLocal);
        env->DeleteLocalRef(pointClassLocal);
        xID = env->GetFieldID(pointClass, "x", "I");
        yID = env->GetFieldID(pointClass, "y", "I");
    }

    BPoint location;
    if (get_mouse(&location, NULL) == B_OK) {
        env->SetIntField(point, xID, (jint)location.x);
        env->SetIntField(point, yID, (jint)location.y);
    }

    return (jint)0;
}

/*
 * Class:     sun_awt_DefaultMouseInfoPeer
 * Method:    isWindowUnderMouse
 * Signature: (Ljava/awt/Window)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_DefaultMouseInfoPeer_isWindowUnderMouse(JNIEnv* env,
    jclass clazz, jobject window)
{
    return JNI_FALSE;
}


}
