/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GCONF_GCONF_FP_H
#define GCONF_GCONF_FP_H

#include <glib_fp.h>
#include <gconf/gconf-client.h>
#include <jni.h>
#include <stddef.h>

extern fp_client_get_default_func* my_get_default_func;
extern fp_client_get_string_func* my_get_string_func;
extern fp_client_get_int_func* my_get_int_func;
extern fp_client_get_bool_func* my_get_bool_func;
extern fp_conf_init_func* my_gconf_init_func;

#define gconf_client_get_default (*my_get_default_func)
#define gconf_client_get_string (*my_get_string_func)
#define gconf_client_get_int (*my_get_int_func)
#define gconf_client_get_bool (*my_get_bool_func)
#define gconf_init (*my_gconf_init_func)

jboolean init_gconf(int* gconf_ver, void** gconf_client);

#endif
