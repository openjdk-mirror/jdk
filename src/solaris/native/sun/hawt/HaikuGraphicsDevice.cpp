/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_hawt_HaikuGraphicsDevice.h"

#include <Screen.h>

static void getScreenDPI(screen_id displayID, double* _horizontal,
        double* _vertical) {
	BScreen screen(displayID);
    if (!screen.IsValid()) {
    	return;
    }

	// This doesn't work with the VirtualBox screen, but
	// I'm guessing it's OK elsewhere.
    monitor_info info;
    if (screen.GetMonitorInfo(&info) != B_OK) {
    	return;
    }

	// 2.54 cm == 1 inch
	double width = info.width / 2.54;
	double height = info.height / 2.54;

	if (width != 0 && _horizontal != NULL)
        *_horizontal = (screen.Frame().Width() + 1) / width;
	if (height != 0 && _vertical != NULL)
		*_vertical = (screen.Frame().Width() + 1) / height;
}

extern "C" {

/*
 * Class:     sun_hawt_HaikuGraphicsDevice
 * Method:    nativeGetXResolution
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL
Java_sun_hawt_HaikuGraphicsDevice_nativeGetXResolution
  (JNIEnv *env, jclass clazz, jint displayID)
{
	double horz = 0;
	getScreenDPI(B_MAIN_SCREEN_ID, &horz, NULL);
    return horz;
}

/*
 * Class:     sun_hawt_HaikuGraphicsDevice
 * Method:    nativeGetYResolution
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL
Java_sun_hawt_HaikuGraphicsDevice_nativeGetYResolution
  (JNIEnv *env, jclass clazz, jint displayID)
{
	double vert = 0;
	getScreenDPI(B_MAIN_SCREEN_ID, NULL, &vert);
	return vert;
}

}
