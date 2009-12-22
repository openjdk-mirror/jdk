/*
 * Copyright 1998-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

/*
 *  Author: Andrew Bachmann
 */

#include <stdio.h>

#include <jni.h>
#include <jni_util.h>
#include <sun_awt_font_NativeFontWrapper.h>

#include <sys/param.h>
#include <FindDirectory.h>

JNIEXPORT jstring JNICALL Java_sun_awt_font_NativeFontWrapper_getFontPath(JNIEnv *env, jclass obj, jboolean noType1)
{
	char userpath[MAXPATHLEN+1];
	char haikupath[MAXPATHLEN+1];
	char fontpath[(MAXPATHLEN+1)*4];

	status_t user = find_directory(B_USER_FONTS_DIRECTORY, 0, false, userpath, sizeof(userpath));
	status_t haiku = find_directory(B_SYSTEM_FONTS_DIRECTORY, 0, false, haikupath, sizeof(haikupath));

	strcpy(fontpath,"");
	if (user == B_OK) {
		if (!noType1) {
			strcat(fontpath, userpath);
			strcat(fontpath, "/PS-Type1:");
		}
		strcat(fontpath, userpath);
		strcat(fontpath, "/ttfonts");
	}
	if ((user == B_OK) && (haiku == B_OK)) {
		strcat(fontpath, ":");
	}
	if (haiku == B_OK) {
		if (!noType1) {
			strcat(fontpath, haikupath);
			strcat(fontpath, "/PS-Type1:");
		}
		strcat(fontpath, haikupath);
		strcat(fontpath, "/ttfonts");
	}
	return JNU_NewStringPlatform(env, fontpath);
}
