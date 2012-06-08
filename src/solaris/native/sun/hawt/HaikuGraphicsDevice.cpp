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

#include <jni.h>

#include <Screen.h>

#include "Utilities.h"

extern "C" {

/*
 * Class:     sun_hawt_HaikuGraphicsDevice
 * Method:    nativeGetYResolution
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuGraphicsDevice_nativeGetScreenResolution(JNIEnv *env,
	jclass clazz, jint displayID, jdoubleArray resolution)
{
	screen_id id;
	id.id = displayID;
	BScreen screen(id);
	if (!screen.IsValid())
		return;

	// This doesn't work with the VirtualBox screen, but
	// I'm guessing it's OK elsewhere.
	monitor_info info;
	if (screen.GetMonitorInfo(&info) != B_OK)
		return;

	// 2.54 cm == 1 inch
	double width = info.width / 2.54;
	double height = info.height / 2.54;

	if (width != 0) {
		jdouble xRes = (screen.Frame().Width() + 1) / width;
		env->SetDoubleArrayRegion(resolution, 0, 1, &xRes);
	}
	if (height != 0) {
		jdouble yRes = (screen.Frame().Width() + 1) / height;
		env->SetDoubleArrayRegion(resolution, 1, 1, &yRes);
	}
}

}
