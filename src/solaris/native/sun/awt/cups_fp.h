/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __CUPS_FP_H__
#define __CUPS_FP_H__

typedef const char* (*fn_cupsServer)(void);
typedef int (*fn_ippPort)(void);
typedef http_t* (*fn_httpConnect)(const char *, int);
typedef void (*fn_httpClose)(http_t *);
typedef char* (*fn_cupsGetPPD)(const char *);
typedef ppd_file_t* (*fn_ppdOpenFile)(const char *);
typedef void (*fn_ppdClose)(ppd_file_t *);
typedef ppd_option_t* (*fn_ppdFindOption)(ppd_file_t *, const char *);
typedef ppd_size_t* (*fn_ppdPageSize)(ppd_file_t *, char *);

fn_cupsServer j2d_cupsServer;
fn_ippPort j2d_ippPort;
fn_httpConnect j2d_httpConnect;
fn_httpClose j2d_httpClose;
fn_cupsGetPPD j2d_cupsGetPPD;
fn_ppdOpenFile j2d_ppdOpenFile;
fn_ppdClose j2d_ppdClose;
fn_ppdFindOption j2d_ppdFindOption;
fn_ppdPageSize j2d_ppdPageSize;

#define cupsServer (*j2d_cupsServer)
#define ippPort (*j2d_ippPort)
#define httpConnect (*j2d_httpConnect)
#define httpClose (*j2d_httpClose)
#define cupsGetPPD (*j2d_cupsGetPPD)
#define ppdOpenFile (*j2d_ppdOpenFile)
#define ppdClose (*j2d_ppdClose)
#define ppdFindOption (*j2d_ppdFindOption)
#define ppdPageSize (*j2d_ppdPageSize)

jboolean cups_init();

#endif
