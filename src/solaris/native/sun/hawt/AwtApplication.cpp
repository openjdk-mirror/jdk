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

#include "AwtApplication.h"

#include <Clipboard.h>
#include <FilePanel.h>
#include <MenuItem.h>
#include <Path.h>
#include <Window.h>

#include "Utilities.h"

AwtApplication::AwtApplication(const char* signature)
	:
	BApplication(signature)
{
}

DECLARE_JAVA_CLASS(haikuClipboardClazz, "sun/hawt/HaikuClipboard")

void
AwtApplication::MessageReceived(BMessage* message)
{
	switch (message->what) {
		case kMenuMessage: {
			void* peerPointer;
			if (message->FindPointer("peer", &peerPointer) != B_OK)
				break;
			
			jobject peer = (jobject)peerPointer;
			jint mods = ConvertInputModifiersToJava(modifiers());

			int64 when;
			message->FindInt64("when", &when);

			bool check = false;
			if (message->FindBool("checkbox", &check) == B_OK) {
				BMenuItem* item;
				message->FindPointer("source", (void**)&item);
				// We gotta flip the check on this bad boy
				check = !item->IsMarked();
				item->SetMarked(check);
			}

			DoCallback(peer, "handleAction", "(JIZ)V", (jlong)(when / 1000),
				mods, check);
			break;
		}
		case kFileMessage:
		case B_CANCEL:
			_HandleFileMessage(message);
			break;
		case B_CLIPBOARD_CHANGED: {
			JNIEnv* env = GetEnv();
			DECLARE_STATIC_VOID_JAVA_METHOD(clipChange, haikuClipboardClazz,
				"clipboardChanged", "()V");
			env->CallStaticVoidMethod(clazz, clipChange);
			break;
		}
			
	}
	BApplication::MessageReceived(message);
}

bool
AwtApplication::QuitRequested()
{
	be_clipboard->StopWatching(be_app_messenger);

	BApplication::QuitRequested();
	return true;
}

void
AwtApplication::ReadyToRun()
{
	be_clipboard->StartWatching(be_app_messenger);
}

void
AwtApplication::_HandleFileMessage(BMessage* msg)
{
	// We might as well free the and ref filter now since all
	// the interesting stuff is in the message
	void* panelPtr;
	if (msg->FindPointer("panel", &panelPtr) == B_OK) {
		BFilePanel* panel = (BFilePanel*)panelPtr;
		delete panel;
	}
	void* filterPtr;
	if (msg->FindPointer("filter", &filterPtr) == B_OK) {
		BRefFilter* filter = (BRefFilter*)filterPtr;
		if (filter != NULL) {
			delete filter;
		}
	}

	JNIEnv* env = GetEnv();

	jobject peer;

	void* peerPtr;
	if (msg->FindPointer("peer", &peerPtr) != B_OK)
		return;

	peer = (jobject)peerPtr;

	jobjectArray result = NULL;
	if (msg->what == kFileMessage) {
		jclass stringClazz = env->FindClass("Ljava/lang/String;");
		if (stringClazz != NULL) {
			bool save = false;
			if (msg->FindBool("save", &save) == B_OK) {
				if (save) {
					result = _HandleSaveMessage(msg, env, peer, stringClazz);
				} else {
					result = _HandleOpenMessage(msg, env, peer, stringClazz);
				}
			}
			env->DeleteLocalRef(stringClazz);
		}
	}

	// We do the upcall in all cases; we treat failure cases above as if
	// the user canceled the dialog
	DoCallback(peer, "done", "([Ljava/lang/String;)V", result);

	if (result != NULL) {
		env->DeleteLocalRef(result);
	}

	env->DeleteWeakGlobalRef(peer);

}

jobjectArray
AwtApplication::_HandleOpenMessage(BMessage* msg, JNIEnv* env, jobject peer,
	jclass stringClazz)
{
	// Files opened, we get some number of refs (hopefully)
	type_code typeFound;
	int32 count;
	if (msg->GetInfo("refs", &typeFound, &count) != B_OK)
		return NULL;

	if (typeFound != B_REF_TYPE || count < 1)
		return NULL;

	jobjectArray result = env->NewObjectArray(count, stringClazz, NULL);
	if (result == NULL)
		return NULL;

	entry_ref ref;
	for (int i = 0; i < count; i++) {
		msg->FindRef("refs", i, &ref);
		BPath path = BPath(&ref);
		jstring file = env->NewStringUTF(path.Path());
		if (file == NULL) {
			env->DeleteLocalRef(result);
			return NULL;
		}

		env->SetObjectArrayElement(result, i, file);
		env->DeleteLocalRef(file);
	}

	return result;
}

jobjectArray
AwtApplication::_HandleSaveMessage(BMessage* msg, JNIEnv* env, jobject peer,
	jclass stringClazz)
{
	// File saved, we get a dir ref in "directory" and a leaf
	// string in "name".
	entry_ref dir;
	const char* leaf;
	if (msg->FindRef("directory", &dir) != B_OK
			|| msg->FindString("name", &leaf) != B_OK)
		return NULL;

	BPath path = BPath(&dir);
	path.Append(leaf);
	jobjectArray result = env->NewObjectArray(1, stringClazz, NULL);

	if (result == NULL)
		return NULL;

	jstring file = env->NewStringUTF(path.Path());
	if (file == NULL) {
		env->DeleteLocalRef(result);
		return NULL;
	}

	env->SetObjectArrayElement(result, 0, file);
	env->DeleteLocalRef(file);

	return result;
}
