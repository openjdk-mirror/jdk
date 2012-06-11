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
#include <Path.h>
#include <Window.h>

#include "AwtApplication.h"
#include "Utilities.h"

class RefFilter : public BRefFilter {
public:
						RefFilter(jobject peer);
	virtual				~RefFilter();

	virtual	bool		Filter(const entry_ref* ref, BNode* node,
							stat_beos* st, const char* filetype);
private:
			jobject		fPeer;
			jmethodID	fAcceptMethod;
			JNIEnv*		fEnv;
};


RefFilter::RefFilter(jobject peer)
	:
	fPeer(peer),
	fAcceptMethod(NULL),
	fEnv(NULL)
{
}


RefFilter::~RefFilter()
{
	// The peer ref is released in AwtApplication when the file
	// dialog is closed.
}


bool
RefFilter::Filter(const entry_ref* ref, BNode* node, stat_beos* st,
	const char* filetype)
{
	// NOTE this code assumes that for each instance Filter will
	// always be called from the same thread.
	if (fEnv == NULL) {
		// TODO must detach thread when done
    	fEnv = GetEnv();
	}

	if (fAcceptMethod == NULL) {
		jclass clazz = fEnv->GetObjectClass(fPeer);
		// Should this method ID be made a global ref?
		fAcceptMethod = fEnv->GetMethodID(clazz, "acceptFile",
			"(Ljava/lang/String;)Z");
		fEnv->DeleteLocalRef(clazz);
		// if this fails just let everything through
		if (fAcceptMethod == NULL)
			return true;
	}

	BEntry entry = BEntry(ref);
	BPath path;
	entry.GetPath(&path);
	const char* pathString = path.Path();

	jstring javaString = fEnv->NewStringUTF(pathString);
	jboolean accept = fEnv->CallBooleanMethod(fPeer, fAcceptMethod,
		javaString);
	fEnv->DeleteLocalRef(javaString);
	return accept;
}


extern "C" {

/*
 * Class:     sun_hawt_HaikuFileDialog
 * Method:    nativeShowDialog
 * Signature: (Ljava/lang/String;ZZZLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuFileDialog_nativeShowDialog(JNIEnv *env, jobject thiz,
	jstring title, jboolean saveMode, jboolean multipleMode,
	jboolean filterFilenames, jstring directory)
{
	entry_ref* refPtr = NULL;
	entry_ref ref;
	if (directory != NULL) {
		const char* dirString = env->GetStringUTFChars(directory, NULL);
		if (dirString == NULL)
		    return;
		if (get_ref_for_path(dirString, &ref) == B_OK)
			refPtr = &ref;
		env->ReleaseStringUTFChars(directory, dirString);
	}

	const char* titleString = env->GetStringUTFChars(title, NULL);
	if (titleString == NULL)
		return;

	jobject peer = env->NewWeakGlobalRef(thiz);
	RefFilter* filter = NULL;
	if (filterFilenames)
		filter = new RefFilter(peer);
	

    BFilePanel* panel = new BFilePanel(saveMode ? B_SAVE_PANEL : B_OPEN_PANEL,
    	NULL, refPtr, 0, multipleMode, NULL, filter);

	BMessage* message = new BMessage(kFileMessage);
	message->AddPointer("peer", peer);
	message->AddPointer("panel", panel);
	message->AddPointer("filter", filter);
	message->AddBool("save", saveMode);
	panel->SetMessage(message);
	delete message;

    panel->Window()->SetTitle(titleString);
	panel->Show();
	
	env->ReleaseStringUTFChars(title, titleString);
}

}
