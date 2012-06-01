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

#include <MenuItem.h>

#include "Utilities.h"

AwtApplication::AwtApplication(const char* signature)
	:
	BApplication(signature)
{
}

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
	}
	BApplication::MessageReceived(message);
}


void
AwtApplication::_HandleFileMessage(BMessage* msg)
{
	JNIEnv* env = GetEnv();

	// Try to get out early
	jclass stringClazz = env->FindClass("Ljava/lang/String;");
	void* peerPointer;
	if (message->FindPointer("peer", &peerPointer) != B_OK
			|| stringClazz == NULL) {
		void* panelPtr;
		if (message->FindPointer("panel", &panelPtr)) {
			BFilePanel* panel = (BFilePanel*)panelPtr;
			// alert ref filter not deleted!
			delete panel;
		}
		return;
	}
	
	jobject peer = (jobject)peerPointer; // PEER WEAK GLOBAL REF

	// Managing JNI refs in here is bad...
	jobjectArray result = NULL;
	if (msg->what == kFileMessage) {
 // REF STRING CLAZZ #1
		if (stringClazz != NULL) {

			bool save = false;
			msg->FindBool("save", &save);
			if (save) {
				// File saved, we get a dir ref in "directory" and a leaf
				// string in "name".
				entry_ref dir;
				const char* leaf;
				if (msg->FindRef("directory", &dir) == B_OK
						&& msg->FindString("name", &leaf) == B_OK) {
					BPath path = BPath(dir);
					path.append(leaf);
						result = env->NewObjectArray(1, stringClazz, NULL); // REF OBJ ARR #1
						if (result != NULL) {
							jstring file = env->NewStringUTF(path.Path()); // REF NEW STR #1
							if (file != NULL) {
								env->SetObjectArrayElement(result, 0, file);
								env->DeleteLocalRef(file); // DEL NEW STR # 1
							}
						}
					}
				} else {
					// Files opened, we get some number of refs (hopefully)
					type_code typeFound;
					int32 count;
					if (msg->GetInfo("refs", &typeFound, &count) == B_OK) {
						if (typeFound == B_REF_TYPE && count > 0) {
							result = env->NewObjectArray(count, stringClazz, NULL) // REF OBJ ARR #2
							if (result != NULL) {
								entry_ref file;
								for (int i = 0; i < count; i++) {
									msg->FindRef("refs", i, &file);
									BPath path = BPath(file);
									jstring file = env->NewStringUTF(path.Path()); // REF NEW STR #2
									if (file != NULL) {
										env->SetObjectArrayElement(result, i, file);
										env->DeleteLocalRef(file); // DEL NEW STR #2
									}
								}
							}
						}
					}
				}
				
				env->DeleteLocalRef(stringClazz); // DEL STRING CLAZZ #1
			}
			env->DeleteWeakGlobalRef(peer); // DEL PEER GLOBAL WEAK
		}
	}
	
	// so in the end, if we failed for some reason above or the dialog
	// was cancelled, we pass back null
	DoCallback(peer, "done", "([Ljava/lang/String;)V", result);
	
	if (result != NULL)
		env->DeleteLocalRef(result); // DEL OBJ ARR #1/#2
}
