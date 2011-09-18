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

#include <dlfcn.h>
#include <jvm.h>

#include "gio_fp.h"

type_init_func type_init;
object_unref_func object_unref;
file_new_for_path_func file_new_for_path;
file_query_info_func file_query_info;
file_info_get_content_type_func file_info_get_content_type;
app_info_launch_default_for_uri_func app_info_launch_default_for_uri;
settings_new_func settings_new;
settings_get_boolean_func settings_get_boolean;
settings_get_string_func settings_get_string;
settings_get_strv_func settings_get_strv;
settings_get_int_func settings_get_int;
settings_get_child_func settings_get_child;
strfreev_func gstrfreev;
free_func gfree;

jboolean gio_init()
{
    void* gio_handle;

    gio_handle = dlopen("libgio-2.0.so", RTLD_LAZY);
    if (gio_handle == NULL) {
        gio_handle = dlopen("libgio-2.0.so.0", RTLD_LAZY);
        if (gio_handle == NULL) {
            return JNI_FALSE;
        }
    }

    type_init = (type_init_func)dlsym(gio_handle, "g_type_init");
    (*g_type_init)();

    object_unref = (object_unref_func)dlsym(gio_handle, "g_object_unref");

    file_new_for_path =
        (file_new_for_path_func)dlsym(gio_handle, "g_file_new_for_path");

    file_query_info =
        (file_query_info_func)dlsym(gio_handle, "g_file_query_info");

    file_info_get_content_type = (file_info_get_content_type_func)
        dlsym(gio_handle, "g_file_info_get_content_type");

    app_info_launch_default_for_uri = (app_info_launch_default_for_uri_func)
	dlsym (gio_handle, "g_app_info_launch_default_for_uri");

    settings_new = (settings_new_func) dlsym (gio_handle, "g_settings_new");
    settings_get_boolean = (settings_get_boolean_func)
      dlsym (gio_handle, "g_settings_get_boolean");
    settings_get_string = (settings_get_string_func)
      dlsym (gio_handle, "g_settings_get_string");
    settings_get_strv = (settings_get_strv_func)
      dlsym (gio_handle, "g_settings_get_strv");
    settings_get_int = (settings_get_int_func)
      dlsym (gio_handle, "g_settings_get_int");
    settings_get_child = (settings_get_child_func)
      dlsym (gio_handle, "g_settings_get_child");
    gstrfreev = (strfreev_func) dlsym (gio_handle, "g_strfreev");
    gfree = (free_func) dlsym (gio_handle, "g_free");

    if (type_init == NULL ||
        object_unref == NULL ||
        file_new_for_path == NULL ||
        file_query_info == NULL ||
        file_info_get_content_type == NULL ||
	app_info_launch_default_for_uri == NULL ||
	settings_new == NULL ||
	settings_get_boolean == NULL ||
	settings_get_string == NULL ||
	settings_get_strv == NULL ||
	settings_get_int == NULL ||
	settings_get_child == NULL ||
	gstrfreev == NULL ||
	gfree == NULL)
    {
        dlclose(gio_handle);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
