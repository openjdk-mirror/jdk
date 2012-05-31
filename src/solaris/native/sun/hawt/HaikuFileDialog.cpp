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

#include <Entry.h>
#include <FilePanel.h>
#include <RefFilter.h>

#include "AwtApplication.h"

class RefFilter : public BRefFilter {
public:
						RefFilter(jobject peer);

	virtual	bool		Filter(const entry_ref* ref, BNode* node,
							struct stat* st, const char* filetype);
private:
			jobject		fPeer;
			jmethodID	fAcceptMethod;
			JNIEnv*		fEnv;
}


RefFilter::RefFilter(jobject peer)
	:
	fPeer(peer),
	fAcceptMethod(NULL),
	fEnv(NULL)
{
}


bool
RefFilter::Filter(const entry_ref* ref, BNode* node, struct stat* st,
	const char* filetype)
{
	if (fEnv == NULL) {
    	jvm->AttachCurrentThread((void**)&fEnv, NULL);
	}

	if (fAcceptMethod == NULL) {
		jclass clazz = fEnv->GetObjectClass(fPeer);
		fAcceptMethod = fEnv->(clazz, "acceptFile", "(Ljava/lang/String;)Z");
		// if this fails just let everything through
		if (fAcceptMethod == NULL)
			return true;
	}
	
	// Get string path from entry_ref
	// Convert to jstring
	// Call method
	// Return result etc
	jboolean accept = fEnv->CallBooleanMethod(fPeer, fAcceptMethod
}
						

extern "C" {


/*
 * Class:     sun_hawt_HaikuFileDialog
 * Method:    nativeShowDialog
 * Signature: (Ljava/lang/String;ZZZLjava/lang/String;)V
 */
JNIEXPORT jvoid JNICALL
Java_sun_hawt_HaikuDesktopPeer_nativeCreate(JNIEnv *env, jobject thiz,
	jstring title, jboolean saveMode, jboolean multipleMode,
	jboolean filterFilenames, jstring directory)
{
	const char* titleString = env->GetStringUTFChars(title, NULL);
	
	entry_ref* refPtr = NULL;
	entry_ref ref;
	if (directory != NULL) {
		const char* dirString = env->GetStringUTFChars(directory, NULL);
		if (dirString == NULL)
		    return;
		if (get_ref_for_path(dirString, &ref) == B_OK)
			refPtr = &ref;
	}

	// Setup ref filter if neccessary

    BFilePanel* panel = new BFilePanel(saveMode ? B_SAVE_PANEL : B_OPEN_PANEL,
    	NULL, refPtr, 0, multipleMode, new BMessage(kFileMessage), 

	FileDialog* dialog = new FileDialog(dirString, saveMode, multipleMode, thiz); 
	env->ReleaseStringUTFChars(directory, dirString);

	return ptr_to_jlong(dialog);
}

}
