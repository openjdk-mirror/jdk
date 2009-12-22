/*
 * Copyright 1997-2003 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

#include "java_awt_Color.h"
#include "java_awt_Rectangle.h"
#include "sun_awt_CharsetString.h"
#include "sun_awt_image_Image.h"

/*
 * This file contains stubs for JNI field and method id initializers
 * which are used in the win32 awt.
 */

jfieldID colorValueID;


JNIEXPORT void JNICALL Java_sun_awt_image_Image_initIDs
  (JNIEnv * env, jclass klass)
{
}


#include "java_awt_event_MouseEvent.h"

JNIEXPORT void JNICALL
Java_java_awt_event_MouseEvent_initIDs
  (JNIEnv * env, jclass klass)
{
}


// TODO ADDED TO REMOVE LINK ERRORS TO FACILITATE TESTING....
#include "java_awt_event_InputEvent.h"
#include "java_awt_event_KeyEvent.h"
#include "java_awt_Event.h"
#include "jni_util.h"


// TODO: Below was added as a quick hack to get to testing Frames. 
// Remove these as you implement the classes!

JNIEXPORT void JNICALL
Java_java_awt_event_InputEvent_initIDs
  (JNIEnv * env, jclass klass)
{
}


JNIEXPORT void JNICALL 
Java_java_awt_Event_initIDs
  (JNIEnv * env, jclass klass) 
{
}
