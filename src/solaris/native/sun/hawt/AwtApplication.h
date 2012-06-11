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
#ifndef AWT_APPLICATION_H
#define	AWT_APPLICATION_H

#include <jni.h>

#include <Application.h>

const uint32 kMenuMessage = 'menu';
const uint32 kFileMessage = 'file';

class AwtApplication : public BApplication {
public:
							AwtApplication(const char* signature);
	virtual	void			MessageReceived(BMessage* msg);
	virtual	bool			QuitRequested();
	virtual	void			ReadyToRun();
private:
			void			_HandleFileMessage(BMessage* msg);
			jobjectArray	_HandleOpenMessage(BMessage* msg, JNIEnv* env,
								jobject peer, jclass stringClazz);
			jobjectArray	_HandleSaveMessage(BMessage* msg, JNIEnv* env,
								jobject peer, jclass stringClazz);
};

#endif	/* AWT_APPLICATION_H */
