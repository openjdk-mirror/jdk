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

#include <syscalls_fp.h>

#include <dlfcn.h>
#include <jni_util.h>

openat64_func* my_openat64_func = NULL;
fstatat64_func* my_fstatat64_func = NULL;
unlinkat_func* my_unlinkat_func = NULL;
renameat_func* my_renameat_func = NULL;
futimesat_func* my_futimesat_func = NULL;
fdopendir_func* my_fdopendir_func = NULL;
fgetxattr_func* my_fgetxattr_func = NULL;
fsetxattr_func* my_fsetxattr_func = NULL;
fremovexattr_func* my_fremovexattr_func = NULL;
flistxattr_func* my_flistxattr_func = NULL;

void syscalls_init()
{
    my_fgetxattr_func = (fgetxattr_func*)dlsym(RTLD_DEFAULT, "fgetxattr");
    my_fsetxattr_func = (fsetxattr_func*)dlsym(RTLD_DEFAULT, "fsetxattr");
    my_fremovexattr_func = (fremovexattr_func*)dlsym(RTLD_DEFAULT, "fremovexattr");
    my_flistxattr_func = (flistxattr_func*)dlsym(RTLD_DEFAULT, "flistxattr");
}

int atsyscalls_init()
{
    /* system calls that might not be available at run time */

#if (defined(__solaris__) && defined(_LP64)) || defined(_ALLBSD_SOURCE)
    /* Solaris 64-bit does not have openat64/fstatat64 */
    my_openat64_func = (openat64_func*)dlsym(RTLD_DEFAULT, "openat");
    my_fstatat64_func = (fstatat64_func*)dlsym(RTLD_DEFAULT, "fstatat");
#else
    my_openat64_func = (openat64_func*) dlsym(RTLD_DEFAULT, "openat64");
    my_fstatat64_func = (fstatat64_func*) dlsym(RTLD_DEFAULT, "fstatat64");
#endif
    my_unlinkat_func = (unlinkat_func*) dlsym(RTLD_DEFAULT, "unlinkat");
    my_renameat_func = (renameat_func*) dlsym(RTLD_DEFAULT, "renameat");
    my_futimesat_func = (futimesat_func*) dlsym(RTLD_DEFAULT, "futimesat");
    my_fdopendir_func = (fdopendir_func*) dlsym(RTLD_DEFAULT, "fdopendir");

#if defined(_ATFILE_SOURCE)
    /* fstatat64 from glibc requires a define */
    if (my_fstatat64_func == NULL)
        my_fstatat64_func = (fstatat64_func*)&fstatat64;
#endif

    if (my_openat64_func != NULL &&  my_fstatat64_func != NULL &&
        my_unlinkat_func != NULL && my_renameat_func != NULL &&
        my_futimesat_func != NULL && my_fdopendir_func != NULL)
    {
        return 0;
    }

    return -1;
}

size_t fgetxattr_dl(int fd, const char* name, void* value, size_t size)
{
    if (my_fgetxattr_func == NULL) {
        errno = ENOTSUP;
	return -1;
    }
    /* EINTR not documented */
    return (*my_fgetxattr_func)(fd, name, value, size);
}

int fsetxattr_dl(int fd, const char* name, void* value, size_t size, int flags)
{
    if (my_fsetxattr_func == NULL) {
        errno = ENOTSUP;
	return -1;
    }
    /* EINTR not documented */
    return (*my_fsetxattr_func)(fd, name, value, size, flags);
}

int fremovexattr_dl(int fd, const char* name)
{
    if (my_fremovexattr_func == NULL) {
        errno = ENOTSUP;
	return -1;
    }
    /* EINTR not documented */
    return (*my_fremovexattr_func)(fd, name);
}

int flistxattr_dl(int fd, char* list, size_t size)
{
    if (my_flistxattr_func == NULL) {
        errno = ENOTSUP;
	return -1;
    }
    /* EINTR not documented */
    return (*my_flistxattr_func)(fd, list, size);
}

