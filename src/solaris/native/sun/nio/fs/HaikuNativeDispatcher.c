/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"

#include <errno.h>
#include <fs_info.h>
#include <gnu/sys/xattr.h>
#include <kernel/OS.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "sun_nio_fs_HaikuNativeDispatcher.h"

static jfieldID entry_name;
static jfieldID entry_dir;
static jfieldID entry_fstype;
static jfieldID entry_options;
static jfieldID entry_dev;

JNIEXPORT void JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_init(JNIEnv* env, jclass this)
{
    jclass clazz = (*env)->FindClass(env, "sun/nio/fs/UnixMountEntry");
    if (clazz == NULL) {
        return;
    }
    entry_name = (*env)->GetFieldID(env, clazz, "name", "[B");
    entry_dir = (*env)->GetFieldID(env, clazz, "dir", "[B");
    entry_fstype = (*env)->GetFieldID(env, clazz, "fstype", "[B");
    entry_options = (*env)->GetFieldID(env, clazz, "opts", "[B");
    entry_dev = (*env)->GetFieldID(env, clazz, "dev", "J");
    return;
}

static void throwUnixException(JNIEnv* env, int errnum) {
    jobject x = JNU_NewObjectByName(env, "sun/nio/fs/UnixException",
        "(I)V", errnum);
    if (x != NULL) {
        (*env)->Throw(env, x);
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_fgetxattr0(JNIEnv* env, jclass clazz,
    jint fd, jlong nameAddress, jlong valueAddress, jint valueLen)
{
    size_t res = -1;
    const char* name = jlong_to_ptr(nameAddress);
    void* value = jlong_to_ptr(valueAddress);

    res = fgetxattr(fd, name, value, valueLen);

    if (res == (size_t)-1)
        throwUnixException(env, errno);
    return (jint)res;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_fsetxattr0(JNIEnv* env, jclass clazz,
    jint fd, jlong nameAddress, jlong valueAddress, jint valueLen)
{
    int res = -1;
    const char* name = jlong_to_ptr(nameAddress);
    void* value = jlong_to_ptr(valueAddress);

    res = fsetxattr(fd, name, value, valueLen, 0);

    if (res == -1)
        throwUnixException(env, errno);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_fremovexattr0(JNIEnv* env, jclass clazz,
    jint fd, jlong nameAddress)
{
    int res = -1;
    const char* name = jlong_to_ptr(nameAddress);

    res = fremovexattr(fd, name);

    if (res == -1)
        throwUnixException(env, errno);
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_flistxattr(JNIEnv* env, jclass clazz,
    jint fd, jlong listAddress, jint size)
{
    size_t res = -1;
    char* list = jlong_to_ptr(listAddress);

    res = flistxattr(fd, list, (size_t)size);

    if (res == (size_t)-1)
        throwUnixException(env, errno);
    return (jint)res;
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_getcookie(JNIEnv* env, jclass clazz)
{
    int32* cookie = malloc(sizeof(int32));
    if (cookie == NULL)
        JNU_ThrowOutOfMemoryError(env, "native heap");
    else
	    *cookie = 0;

    return ptr_to_jlong(cookie);
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_nextentry(JNIEnv* env, jclass clazz,
    jlong cookie, jobject entry)
{
    dev_t dev = next_dev(jlong_to_ptr(cookie));

    if (dev < 0)
        return -1;

    return Java_sun_nio_fs_HaikuNativeDispatcher_entryfordev(env, clazz, dev,
        entry);
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_entryfordev(JNIEnv* env, jclass clazz,
    jlong dev, jobject entry)
{
    fs_info info;
    if (fs_stat_dev((dev_t)dev, &info) != B_OK)
        return -1;

    // simulate mntent on Haiku
    const char* name = info.volume_name;
    const char* fstype = info.fsh_name;

    // TODO: won't work if two volumes have the same name.
    // BPath can convert entry ref to path but there's no
    // C function to do it.
    char dir[B_FILE_NAME_LENGTH];
    snprintf(dir, sizeof(dir), "/%s", name);

    const char* options;
    // only one that matters
    if (info.flags & B_FS_IS_READONLY)
        options = "ro";
    else
        options = "rw";

    jsize len = strlen(name);
    jbyteArray bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)name);
    (*env)->SetObjectField(env, entry, entry_name, bytes);

    len = strlen(dir);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)dir);
    (*env)->SetObjectField(env, entry, entry_dir, bytes);

    len = strlen(fstype);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)fstype);
    (*env)->SetObjectField(env, entry, entry_fstype, bytes);

    len = strlen(options);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)options);
    (*env)->SetObjectField(env, entry, entry_options, bytes);

    if (dev != 0)
        (*env)->SetLongField(env, entry, entry_dev, (jlong)dev);

    return 0;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_HaikuNativeDispatcher_disposecookie(JNIEnv* env, jclass clazz,
    jlong cookie)
{
    free(jlong_to_ptr(cookie));
}
