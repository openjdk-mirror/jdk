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

#include <stdlib.h>
#include <dlfcn.h>
#ifndef __APPLE__
#include <link.h>
#endif

#ifdef __solaris__
#include <strings.h>
#endif

#if defined(__linux__) || defined(__APPLE__)
#include <string.h>
#endif

#ifdef USE_SYSTEM_GIO
#include <gio/gio.h>
#else
#include <gio_fp.h>
#endif

/* Definitions for GNOME VFS */

typedef gboolean (*gnome_vfs_init_function)(void);
typedef const char* (*gnome_vfs_mime_type_from_name_function)
    (const char* filename);

static gnome_vfs_init_function gnome_vfs_init;
static gnome_vfs_mime_type_from_name_function gnome_vfs_mime_type_from_name;

#include "sun_nio_fs_GnomeFileTypeDetector.h"

JNIEXPORT jboolean JNICALL
Java_sun_nio_fs_GnomeFileTypeDetector_initializeGio
    (JNIEnv* env, jclass this)
{
    jboolean ret;

#ifdef USE_SYSTEM_GIO
    ret = JNI_TRUE;
#else
    ret = gio_init();
#endif

    // If there was an error initializing GIO, return false immediately
    if (ret == JNI_FALSE)
    {
        return ret;
    }

    g_type_init ();
    return ret;
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_GnomeFileTypeDetector_probeUsingGio
    (JNIEnv* env, jclass this, jlong pathAddress)
{
    char* path = (char*)jlong_to_ptr(pathAddress);
    GFile* gfile;
    GFileInfo* gfileinfo;
    jbyteArray result = NULL;

    gfile = g_file_new_for_path (path);
    gfileinfo = g_file_query_info (gfile, G_FILE_ATTRIBUTE_STANDARD_CONTENT_TYPE,
        G_FILE_QUERY_INFO_NONE, NULL, NULL);
    if (gfileinfo != NULL) {
        const char* mime = g_file_info_get_content_type (gfileinfo);
        if (mime != NULL) {
            jsize len = strlen(mime);
            result = (*env)->NewByteArray(env, len);
            if (result != NULL) {
                (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)mime);
            }
        }
        g_object_unref (gfileinfo);
    }
    g_object_unref (gfile);

    return result;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_fs_GnomeFileTypeDetector_initializeGnomeVfs
    (JNIEnv* env, jclass this)
{
    void* vfs_handle;

    vfs_handle = dlopen("libgnomevfs-2.so", RTLD_LAZY);
    if (vfs_handle == NULL) {
        vfs_handle = dlopen("libgnomevfs-2.so.0", RTLD_LAZY);
    }
    if (vfs_handle == NULL) {
        return JNI_FALSE;
    }

    gnome_vfs_init = (gnome_vfs_init_function)dlsym(vfs_handle, "gnome_vfs_init");
    gnome_vfs_mime_type_from_name = (gnome_vfs_mime_type_from_name_function)
        dlsym(vfs_handle, "gnome_vfs_mime_type_from_name");

    if (gnome_vfs_init == NULL ||
        gnome_vfs_mime_type_from_name == NULL)
    {
        dlclose(vfs_handle);
        return JNI_FALSE;
    }

    (*gnome_vfs_init)();
    return JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_GnomeFileTypeDetector_probeUsingGnomeVfs
    (JNIEnv* env, jclass this, jlong pathAddress)
{
    char* path = (char*)jlong_to_ptr(pathAddress);
    const char* mime = (*gnome_vfs_mime_type_from_name)(path);

    if (mime == NULL) {
        return NULL;
    } else {
        jbyteArray result;
        jsize len = strlen(mime);
        result = (*env)->NewByteArray(env, len);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)mime);
        }
        return result;
    }
}
