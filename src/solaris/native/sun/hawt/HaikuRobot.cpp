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

#include <Bitmap.h>
#include <Screen.h>

#include "Utilities.h"

extern "C" {

/*
 * Class:     sun_hawt_HaikuRobot
 * Method:    nativeGetPixels
 * Signature: (IIIII[I)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuRobot_nativeGetPixels(JNIEnv *env, jclass clazz,
    jint displayID, jint x, jint y, jint width, jint height, jintArray pixels)
{
    if (env->GetArrayLength(pixels) < width * height)
        return;

    jint* pixelData = env->GetIntArrayElements(pixels, NULL);
    if (pixelData == NULL)
        return;

    screen_id id;
    id.id = displayID;
    BScreen screen(id);
    if (!screen.IsValid())
        return;

    BRect bounds(x, y, x + width - 1, y + height - 1);
    // We allocate our own bitmap to ensure we get the right colour space
    BBitmap bitmap(BRect(0, 0, width - 1, height - 1), B_RGBA32);
    if (!bitmap.IsValid())
        return;

    status_t result = screen.ReadBitmap(&bitmap, false, &bounds);
    if (result != B_OK)
        return;
    uint8* bitmapData = (uint8*)bitmap.Bits();
    int bytesPerRow = bitmap.BytesPerRow();

    for (int i = 0; i < height; i++) {
         for (int j = 0; j < width; j++) {
            // RobotPeer doesn't mention any endianness so I'm just
            // going to copy the integer value of the pixel
            *pixelData++ = bitmapData[j * 4 + i * bytesPerRow];
        }
    }
}

}
