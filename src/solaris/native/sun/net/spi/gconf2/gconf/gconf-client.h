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

#ifndef GCONF_GCONF_H
#define GCONF_GCONF_H

/**
 * These functions are used by the sun.net.spi.DefaultProxySelector class
 * to access some platform specific settings.
 * This is the Solaris/Linux Gnome 2.x code using the GConf-2 library.
 * Everything is loaded dynamically so no hard link with any library exists.
 */
typedef void* fp_client_get_default_func();
typedef char* fp_client_get_string_func(void *, char *, void**);
typedef int   fp_client_get_int_func(void*, char *, void**);
typedef int   fp_client_get_bool_func(void*, char *, void**);
typedef int   fp_conf_init_func(int, char**, void**);
typedef void  fp_type_init_func ();

#endif
