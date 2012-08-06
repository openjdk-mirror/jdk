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

#ifndef __GIO_FP_H__
#define __GIO_FP_H__

#include <gio/gio_typedefs.h>

#include <glib_fp.h>

extern object_unref_func object_unref;
extern file_new_for_path_func file_new_for_path;
extern file_query_info_func file_query_info;
extern file_info_get_content_type_func file_info_get_content_type;
extern app_info_launch_default_for_uri_func app_info_launch_default_for_uri;
extern settings_new_func settings_new;
extern settings_get_boolean_func settings_get_boolean;
extern settings_get_string_func settings_get_string;
extern settings_get_strv_func settings_get_strv;
extern settings_get_int_func settings_get_int;
extern settings_get_child_func settings_get_child;
extern strfreev_func gstrfreev;

#define g_object_unref (*object_unref)
#define g_strfreev (*gstrfreev)
#define g_file_new_for_path (*file_new_for_path)
#define g_file_query_info (*file_query_info)
#define g_file_info_get_content_type (*file_info_get_content_type)
#define g_app_info_launch_default_for_uri (*app_info_launch_default_for_uri)
#define g_settings_new (*settings_new)
#define g_settings_get_boolean (*settings_get_boolean)
#define g_settings_get_string (*settings_get_string)
#define g_settings_get_strv (*settings_get_strv)
#define g_settings_get_int (*settings_get_int)
#define g_settings_get_child (*settings_get_child)

jboolean gio_init();

#endif
