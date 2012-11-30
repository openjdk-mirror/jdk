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

#ifndef __SYSCALLS_FP_H__
#define __SYSCALLS_FP_H__

#include <dirent.h>
#include <errno.h>
#include <unistd.h>
#include <sys/resource.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/stat.h>

/**
 * System calls that may not be available at run time.
 */
typedef size_t fgetxattr_func(int fd, const char* name, void* value, size_t size);
typedef int fsetxattr_func(int fd, const char* name, void* value, size_t size, int flags);
typedef int fremovexattr_func(int fd, const char* name);
typedef int flistxattr_func(int fd, char* list, size_t size);
typedef int openat64_func(int, const char *, int, ...);
typedef int fstatat64_func(int, const char *, struct stat64 *, int);
typedef int unlinkat_func(int, const char*, int);
typedef int renameat_func(int, const char*, int, const char*);
typedef int futimesat_func(int, const char *, const struct timeval *);
typedef DIR* fdopendir_func(int);

extern openat64_func* my_openat64_func;
extern fstatat64_func* my_fstatat64_func;
extern unlinkat_func* my_unlinkat_func;
extern renameat_func* my_renameat_func;
extern futimesat_func* my_futimesat_func;
extern fdopendir_func* my_fdopendir_func;
extern fgetxattr_func* my_fgetxattr_func;
extern fsetxattr_func* my_fsetxattr_func;
extern fremovexattr_func* my_fremovexattr_func;
extern flistxattr_func* my_flistxattr_func;

void syscalls_init();
int atsyscalls_init();
size_t fgetxattr_dl(int fd, const char* name, void* value, size_t size);
int fsetxattr_dl(int fd, const char* name, void* value, size_t size, int flags);
int fremovexattr_dl(int fd, const char* name);
int flistxattr_dl(int fd, char* list, size_t size);

#define openat64 (*my_openat64_func)
#define fstatat64 (*my_fstatat64_func)
#define unlinkat (*my_unlinkat_func)
#define renameat (*my_renameat_func)
#define futimesat (*my_futimesat_func)
#define fdopendir (*my_fdopendir_func)
#define fgetxattr fgetxattr_dl
#define fsetxattr fsetxattr_dl
#define fremovexattr fremovexattr_dl
#define flistxattr flistxattr_dl
#endif
