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

#include <Message.h>

#include "HaikuPlatformWindow.h"

extern "C" {

/*
 * Class:     sun_hawt_DragSourceContextPeer
 * Method:    nativeStartDrag
 * Signature: (J[Ljava/lang/String;[[B)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_hawt_HaikuDragSourceContextPeer_nativeStartDrag(JNIEnv *env,
	jobject thiz, jlong nativeWindow, jobjectArray mimeArray,
	jobjectArray dataArray)
{
	// Sanity check
	int count = env->GetArrayLength(mimeArray);
	if (count != env->GetArrayLength(dataArray))
		return JNI_FALSE;

	BMessage message(B_MIME_DATA);

	for (int i = 0; i < count; i++) {
		jstring mimeType = (jstring)env->GetObjectArrayElement(mimeArray, i);
		const char* mime = env->GetStringUTFChars(mimeType, NULL);
		if (mime == NULL)
			return JNI_FALSE;

		jbyteArray data = (jbyteArray)env->GetObjectArrayElement(dataArray, i);
		jbyte* bytes = env->GetByteArrayElements(data, NULL);
		if (bytes == NULL) {
			env->ReleaseStringUTFChars(mimeType, mime);
			return JNI_FALSE;
		}

		int byteCount = env->GetArrayLength(data);
		message.AddData(mime, B_MIME_TYPE, (void*)bytes, byteCount, false);

		env->ReleaseStringUTFChars(mimeType, mime);
		env->DeleteLocalRef(mimeType);

		env->ReleaseByteArrayElements(data, bytes, 0);
		env->DeleteLocalRef(data);
	}

	PlatformWindow* window = (PlatformWindow*)jlong_to_ptr(nativeWindow);
	if (!window->LockLooper())
		return JNI_FALSE;

	// Use a 64x64 outline
	window->StartDrag(&message, env->NewGlobalRef(thiz));
	window->UnlockLooper();

	return JNI_TRUE;
}

}
