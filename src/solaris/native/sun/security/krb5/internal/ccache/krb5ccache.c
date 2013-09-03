/*
 * Copyright (c) 2013 Red Hat Inc. and/or its affiliates.
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

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <krb5.h>
#include <stdlib.h>
#include <stdio.h>

#include "sun_security_krb5_internal_ccache_FileCredentialsCache.h"

static void handle_error(JNIEnv *env, krb5_context context, krb5_error_code err, const char *func_name);
static jint throw_Exception(JNIEnv *env, const char *class_name, const char *message);

/*
 * Class:     sun_security_krb5_internal_ccache_FileCredentialsCache
 * Method:    nativeGetDefaultCacheName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_security_krb5_internal_ccache_FileCredentialsCache_nativeGetDefaultCacheName
(JNIEnv *env, jclass krbcredsClass)
{
    krb5_context context;
    krb5_error_code err;
    krb5_ccache cache;
    const char *cc_type, *cc_name;
    char *cc_full_name;
    jstring result;

    /* Need a krb5_context to proceed further */
    err = krb5_init_context(&context);
    if (err) {
        handle_error(env, context, err, "krb5_init_context");
        return NULL;
    }

    /* Get the default credential cache.
     * We intentionally do not use krb5_cc_default_name because when the cache
     * is a collection, krb5_cc_default_name returns the collection directory.
     * By using krb5_cc_default and then krb5_cc_get_name, we get the primary
     * cache file within the collection. */
    err = krb5_cc_default(context, &cache);
    if (err) {
        handle_error(env, context, err, "krb5_cc_default");
        krb5_free_context(context);
        return NULL;
    }

    /* Get the type and name of the default cache and construct a string
     * of the form 'type:name'. */
    cc_type = krb5_cc_get_type(context, cache);
    cc_name = krb5_cc_get_name(context, cache);
    if (asprintf(&cc_full_name, "%s:%s", cc_type, cc_name) < 0) {
        throw_Exception(env, "java/lang/OutOfMemoryError", "Unable to construct credential cache string");
        krb5_free_context(context);
        return NULL;
    }

    result = (*env)->NewStringUTF(env, cc_full_name);

    free(cc_full_name);
    krb5_free_context(context);
    return result;
}

static void handle_error(JNIEnv *env, krb5_context context, krb5_error_code err, const char *func_name) {
    const char *err_msg;
    char *result;

    err_msg = krb5_get_error_message(context, err);
    if (asprintf(&result, "%s: %s", func_name, err_msg) < 0) {
        throw_Exception(env, "java/lang/OutOfMemoryError", "Unable to construct error message");
        return;
    }
    throw_Exception(env, "java/lang/Exception", result);

    free(result);
    krb5_free_error_message(context, err_msg);
}

static jint throw_Exception(JNIEnv *env, const char *class_name, const char *message) {
    jclass class;

    class = (*env)->FindClass(env, class_name);
    if (class == NULL) {
        return -1;
    }
    return (*env)->ThrowNew(env, class, message);
}
