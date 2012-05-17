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

#include "sun_hawt_HaikuEventPump.h"

#include <Application.h>
#include <kernel/OS.h>
#include <stdio.h>

extern "C" {

// What's the correct synchronization primitive to
// use here?
sem_id appSem = -1;

JNIEXPORT void JNICALL
Java_sun_hawt_HaikuEventPump_initIDs
  (JNIEnv *env, jclass clazz)
{
	appSem = create_sem(0, "awtAppSem");
}

/*
 * Class:     sun_hawt_HaikuEventPump
 * Method:    runApplication
 * Signature: ()
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuEventPump_runApplication
  (JNIEnv *env, jobject thiz)
{
	// A bit of a hack, we just start the app and the windows/views
	// do all the work of delivering the events back to AWT.
    BApplication* awtApp = new BApplication("application/x-vnd.java-awt-app");
    release_sem(appSem);
    awtApp->Run();
    delete awtApp;
    return;
}

}
