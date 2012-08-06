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

/* Definitions for GIO */

#ifndef __GIO_H__
#define __GIO_H__

#define G_FILE_ATTRIBUTE_STANDARD_CONTENT_TYPE "standard::content-type"

typedef void* gpointer;
typedef int    gint;
typedef gint   gboolean;
typedef char   gchar;
typedef struct _GFile GFile;
typedef struct _GFileInfo GFileInfo;
typedef struct _GCancellable GCancellable;
typedef struct _GError GError;
typedef struct _GAppLaunchContext GAppLaunchContext;
typedef struct _GSettings GSettings;

typedef enum {
  G_FILE_QUERY_INFO_NONE = 0
} GFileQueryInfoFlags;

typedef void (*object_unref_func)(gpointer object);
typedef GFile* (*file_new_for_path_func)(const char* path);
typedef GFileInfo* (*file_query_info_func)(GFile *file,
    const char *attributes, GFileQueryInfoFlags flags,
    GCancellable *cancellable, GError **error);
typedef char* (*file_info_get_content_type_func)(GFileInfo *info);
typedef gboolean  (*app_info_launch_default_for_uri_func)(const char              *uri,
							  GAppLaunchContext       *launch_context,
							  GError                 **error);
typedef GSettings* (*settings_new_func) (const gchar *schema);
typedef gboolean (*settings_get_boolean_func) (GSettings *settings, const gchar *key);
typedef gchar* (*settings_get_string_func) (GSettings *settings, const gchar *key);
typedef gchar** (*settings_get_strv_func) (GSettings *settings, const gchar *key);
typedef gint (*settings_get_int_func) (GSettings *settings, const gchar *key);
typedef GSettings* (*settings_get_child_func) (GSettings *settings, const gchar *name);
typedef void (*strfreev_func) (gchar **str_array);

#endif
