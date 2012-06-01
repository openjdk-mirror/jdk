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

#include <Clipboard.h>

#include "sun_hawt_HaikuClipboard.h"

extern "C" {

/*
 * Class:     sun_hawt_HaikuClipboard
 * Method:    nativeGetClipboardFormats
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_hawt_HaikuClipboard_nativeGetFormats(JNIEnv *env, jobject thiz)
{
	BClipboard* clipboard = new BClipboard("system");
	if (!clipboard->Lock())
		return NULL;

	jclass stringClazz = env->FindClass("java/lang/String");
	if (stringClazz == NULL)
		return NULL;

	BMessage* clip = clipboard->Data();
	int32 count = clip->CountNames(B_MIME_TYPE);

	jobjectArray result = env->NewObjectArray(count, stringClazz, NULL);
	if (result == NULL) {
		clipboard->Unlock();
		return NULL;
	}

	char* nameFound;
	for (int32 i = 0; clip->GetInfo(B_MIME_TYPE, i, &nameFound, NULL, NULL)
			== B_OK; i++) {
		jstring name = env->NewStringUTF(nameFound);
		if (name == NULL) {
			clipboard->Unlock();
			return NULL;
		}
		env->SetObjectArrayElement(result, i, name);
		env->DeleteLocalRef(name);
	}

	clipboard->Unlock();
	return result;
}

/*
 * Class:     sun_hawt_HaikuClipboard
 * Method:    nativeGetData
 * Signature: (Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_hawt_HaikuClipboard_nativeGetData(JNIEnv *env, jobject thiz,
    jstring format)
{
	BClipboard* clipboard = new BClipboard("system");
	if (!clipboard->Lock())
		return NULL;

	const char* mimeType = env->GetStringUTFChars(format, NULL);
	if (mimeType == NULL) {
		clipboard->Unlock();
		return NULL;
	}

	BMessage* clip = clipboard->Data();

	ssize_t length;
	const void* data;
	status_t result = clip->FindData(mimeType, B_MIME_TYPE, 0, &data, &length);
	env->ReleaseStringUTFChars(format, mimeType);

	if (result != B_OK) {
		clipboard->Unlock();
		return NULL;
	}

	jbyteArray bytes = env->NewByteArray(length);
	if (bytes == NULL) {
		clipboard->Unlock();
		return NULL;
	}

	env->SetByteArrayRegion(bytes, 0, length, (jbyte*)data);
	return bytes;	
}

/*
 * Class:     sun_hawt_HaikuClipboard
 * Method:    nativeLockAndClear
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuClipboard_nativeLockAndClear(JNIEnv *env, jobject thiz)
{
	BClipboard* clipboard = new BClipboard("system");
	clipboard->Lock();
	clipboard->Clear();
	return ptr_to_jlong(clipboard);
}

/*
 * Class:     sun_hawt_HaikuClipboard
 * Method:    nativeSetData
 * Signature: (J[BLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuClipboard_nativeSetData(JNIEnv *env, jobject thiz,
	jlong nativeClipboard, jbyteArray data, jstring format)
{
	BClipboard* clipboard = (BClipboard*)jlong_to_ptr(nativeClipboard);
	BMessage* clip = clipboard->Data();
	const char* mimeType = env->GetStringUTFChars(format, NULL);
	
	jsize length = env->GetArrayLength(data);
	jbyte* bytes = env->GetByteArrayElements(data, 0);
	clip->AddData(mimeType, B_MIME_TYPE, bytes, length);
	clipboard->Commit();
}

/*
 * Class:     sun_hawt_HaikuClipboard
 * Method:    nativeUnlock
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuClipboard_nativeUnlock(JNIEnv *env, jobject thiz,
	jlong nativeClipboard)
{
	BClipboard* clipboard = (BClipboard*)jlong_to_ptr(nativeClipboard);
	clipboard->Unlock();
	delete clipboard;
}

}
