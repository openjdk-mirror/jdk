/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <fcntl.h>
#include <dirent.h>
#include <unistd.h>
#include <pwd.h>
#include <grp.h>
#include <errno.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/statvfs.h>
#include <sys/time.h>

#ifdef __solaris__
#include <strings.h>
#include <sys/mnttab.h>
#include <sys/mkdev.h>
#endif

#ifdef __linux__
#include <string.h>
#include <mntent.h>
#endif

#ifdef _ALLBSD_SOURCE
#include <string.h>

#define stat64 stat
#define statvfs64 statvfs

#define open64 open
#define fstat64 fstat
#define lstat64 lstat
#define dirent64 dirent
#define readdir64_r readdir_r
#endif

/* Needed for strerror */
#if defined(_AIX) || defined (__hpux__)
#include <string.h>
#endif

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"

#include "sun_nio_fs_UnixNativeDispatcher.h"

/**
 * Size of password or group entry when not available via sysconf
 */
#define ENT_BUF_SIZE   1024

#define RESTARTABLE(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == -1) && (errno == EINTR)); \
} while(0)

#define RESTARTABLE_RETURN_PTR(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == NULL) && (errno == EINTR)); \
} while(0)

static jfieldID attrs_st_mode;
static jfieldID attrs_st_ino;
static jfieldID attrs_st_dev;
static jfieldID attrs_st_rdev;
static jfieldID attrs_st_nlink;
static jfieldID attrs_st_uid;
static jfieldID attrs_st_gid;
static jfieldID attrs_st_size;
static jfieldID attrs_st_atime;
static jfieldID attrs_st_mtime;
static jfieldID attrs_st_ctime;

static jfieldID attrs_f_frsize;
static jfieldID attrs_f_blocks;
static jfieldID attrs_f_bfree;
static jfieldID attrs_f_bavail;

/* Needed for getmntctl */
#ifdef _AIX
static jclass entry_cls;
#endif

static jfieldID entry_name;
static jfieldID entry_dir;
static jfieldID entry_fstype;
static jfieldID entry_options;
static jfieldID entry_dev;

/* AIX and HP-UX need a couple more references for futimes emulation */
#if defined(_AIX) || defined (__hpux__)
static jmethodID disp_copy2buf;
static jfieldID buffer_address;
static jmethodID buffer_release;
#endif

/**
 * System calls that may not be available at run time.
 */
typedef int openat64_func(int, const char *, int, ...);
typedef int fstatat64_func(int, const char *, struct stat64 *, int);
typedef int unlinkat_func(int, const char*, int);
typedef int renameat_func(int, const char*, int, const char*);
typedef int futimesat_func(int, const char *, const struct timeval *);
typedef DIR* fdopendir_func(int);

static openat64_func* my_openat64_func = NULL;
static fstatat64_func* my_fstatat64_func = NULL;
static unlinkat_func* my_unlinkat_func = NULL;
static renameat_func* my_renameat_func = NULL;
static futimesat_func* my_futimesat_func = NULL;
static fdopendir_func* my_fdopendir_func = NULL;

/**
 * fstatat missing from glibc on Linux. Temporary workaround
 * for x86/x64.
 */
#if defined(__linux__) && defined(__i386)
#define FSTATAT64_SYSCALL_AVAILABLE
static int fstatat64_wrapper(int dfd, const char *path,
                             struct stat64 *statbuf, int flag)
{
    #ifndef __NR_fstatat64
    #define __NR_fstatat64  300
    #endif
    return syscall(__NR_fstatat64, dfd, path, statbuf, flag);
}
#endif

#if defined(__linux__) && defined(__x86_64__)
#define FSTATAT64_SYSCALL_AVAILABLE
static int fstatat64_wrapper(int dfd, const char *path,
                             struct stat64 *statbuf, int flag)
{
    #ifndef __NR_newfstatat
    #define __NR_newfstatat  262
    #endif
    return syscall(__NR_newfstatat, dfd, path, statbuf, flag);
}
#endif

/**
 * Call this to throw an internal UnixException when a system/library
 * call fails
 */
static void throwUnixException(JNIEnv* env, int errnum) {
    jobject x = JNU_NewObjectByName(env, "sun/nio/fs/UnixException",
        "(I)V", errnum);
    if (x != NULL) {
        (*env)->Throw(env, x);
    }
}

/**
 * Initialization
 */
JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_init(JNIEnv* env, jclass this)
{
    jint flags = 0;
    jclass clazz;

    clazz = (*env)->FindClass(env, "sun/nio/fs/UnixFileAttributes");
    if (clazz == NULL) {
        return 0;
    }
    attrs_st_mode = (*env)->GetFieldID(env, clazz, "st_mode", "I");
    attrs_st_ino = (*env)->GetFieldID(env, clazz, "st_ino", "J");
    attrs_st_dev = (*env)->GetFieldID(env, clazz, "st_dev", "J");
    attrs_st_rdev = (*env)->GetFieldID(env, clazz, "st_rdev", "J");
    attrs_st_nlink = (*env)->GetFieldID(env, clazz, "st_nlink", "I");
    attrs_st_uid = (*env)->GetFieldID(env, clazz, "st_uid", "I");
    attrs_st_gid = (*env)->GetFieldID(env, clazz, "st_gid", "I");
    attrs_st_size = (*env)->GetFieldID(env, clazz, "st_size", "J");
    attrs_st_atime = (*env)->GetFieldID(env, clazz, "st_atime", "J");
    attrs_st_mtime = (*env)->GetFieldID(env, clazz, "st_mtime", "J");
    attrs_st_ctime = (*env)->GetFieldID(env, clazz, "st_ctime", "J");

    clazz = (*env)->FindClass(env, "sun/nio/fs/UnixFileStoreAttributes");
    if (clazz == NULL) {
        return 0;
    }
    attrs_f_frsize = (*env)->GetFieldID(env, clazz, "f_frsize", "J");
    attrs_f_blocks = (*env)->GetFieldID(env, clazz, "f_blocks", "J");
    attrs_f_bfree = (*env)->GetFieldID(env, clazz, "f_bfree", "J");
    attrs_f_bavail = (*env)->GetFieldID(env, clazz, "f_bavail", "J");

    clazz = (*env)->FindClass(env, "sun/nio/fs/UnixMountEntry");
    if (clazz == NULL) {
        return 0;
    }
    entry_name = (*env)->GetFieldID(env, clazz, "name", "[B");
    entry_dir = (*env)->GetFieldID(env, clazz, "dir", "[B");
    entry_fstype = (*env)->GetFieldID(env, clazz, "fstype", "[B");
    entry_options = (*env)->GetFieldID(env, clazz, "opts", "[B");
    entry_dev = (*env)->GetFieldID(env, clazz, "dev", "J");

    /* UnixMountEntry class is needed for getmntctl */
#ifdef _AIX
    entry_cls = (*env)->NewGlobalRef(env, clazz);
#endif

    /* AIX and HP-UX need a couple more references for futimes emulation */
#if defined(_AIX) || defined (__hpux__)
    disp_copy2buf = (*env)->GetStaticMethodID(env, this, "copyToNativeBuffer", "(Lsun/nio/fs/UnixPath;)Lsun/nio/fs/NativeBuffer;");
    clazz = (*env)->FindClass(env, "sun/nio/fs/NativeBuffer");
    if (clazz == NULL) {
        return 0;
    }
    buffer_address = (*env)->GetFieldID(env, clazz, "address", "J");
    buffer_release = (*env)->GetMethodID(env, clazz, "release", "()V");
#endif

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

#if defined(FSTATAT64_SYSCALL_AVAILABLE)
    /* fstatat64 missing from glibc */
    if (my_fstatat64_func == NULL)
        my_fstatat64_func = (fstatat64_func*)&fstatat64_wrapper;
#endif

    if (my_openat64_func != NULL &&  my_fstatat64_func != NULL &&
        my_unlinkat_func != NULL && my_renameat_func != NULL &&
        my_futimesat_func != NULL && my_fdopendir_func != NULL)
    {
        flags |= sun_nio_fs_UnixNativeDispatcher_HAS_AT_SYSCALLS;
    }

    return flags;
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_getcwd(JNIEnv* env, jclass this) {
    jbyteArray result = NULL;
    char buf[PATH_MAX+1];

    /* EINTR not listed as a possible error */
    char* cwd = getcwd(buf, sizeof(buf));
    if (cwd == NULL) {
        throwUnixException(env, errno);
    } else {
        jsize len = (jsize)strlen(buf);
        result = (*env)->NewByteArray(env, len);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)buf);
        }
    }
    return result;
}

JNIEXPORT jbyteArray
Java_sun_nio_fs_UnixNativeDispatcher_strerror(JNIEnv* env, jclass this, jint error)
{
    char* msg;
    jsize len;
    jbyteArray bytes;

    /* strerror is not thread-safe on AIX */
#ifdef _AIX
    char buffer[256];
    msg = (strerror_r((int)error, buffer, 256) == 0) ? buffer : "Error while calling strerror_r";
#else
    msg = strerror((int)error);
#endif
    len = strlen(msg);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes != NULL) {
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)msg);
    }
    return bytes;
}

JNIEXPORT jint
Java_sun_nio_fs_UnixNativeDispatcher_dup(JNIEnv* env, jclass this, jint fd) {

    int res = -1;

    RESTARTABLE(dup((int)fd), res);
    if (fd == -1) {
        throwUnixException(env, errno);
    }
    return (jint)res;
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fopen0(JNIEnv* env, jclass this,
    jlong pathAddress, jlong modeAddress)
{
    FILE* fp = NULL;
    const char* path = (const char*)jlong_to_ptr(pathAddress);
    const char* mode = (const char*)jlong_to_ptr(modeAddress);

    do {
        fp = fopen(path, mode);
    } while (fp == NULL && errno == EINTR);

    if (fp == NULL) {
        throwUnixException(env, errno);
    }

    return ptr_to_jlong(fp);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fclose(JNIEnv* env, jclass this, jlong stream)
{
    int res;
    FILE* fp = jlong_to_ptr(stream);

    do {
        res = fclose(fp);
    } while (res == EOF && errno == EINTR);
    if (res == EOF) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_open0(JNIEnv* env, jclass this,
    jlong pathAddress, jint oflags, jint mode)
{
    jint fd;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(open64(path, (int)oflags, (mode_t)mode), fd);
    if (fd == -1) {
        throwUnixException(env, errno);
    }
    return fd;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_openat0(JNIEnv* env, jclass this, jint dfd,
    jlong pathAddress, jint oflags, jint mode)
{
    jint fd;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    if (my_openat64_func == NULL) {
        JNU_ThrowInternalError(env, "should not reach here");
        return -1;
    }

    RESTARTABLE((*my_openat64_func)(dfd, path, (int)oflags, (mode_t)mode), fd);
    if (fd == -1) {
        throwUnixException(env, errno);
    }
    return fd;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_close(JNIEnv* env, jclass this, jint fd) {
    int err;
    /* TDB - need to decide if EIO and other errors should cause exception */
    RESTARTABLE(close((int)fd), err);
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_read(JNIEnv* env, jclass this, jint fd,
    jlong address, jint nbytes)
{
    ssize_t n;
    void* bufp = jlong_to_ptr(address);
    RESTARTABLE(read((int)fd, bufp, (size_t)nbytes), n);
    if (n == -1) {
        throwUnixException(env, errno);
    }
    return (jint)n;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_write(JNIEnv* env, jclass this, jint fd,
    jlong address, jint nbytes)
{
    ssize_t n;
    void* bufp = jlong_to_ptr(address);
    RESTARTABLE(write((int)fd, bufp, (size_t)nbytes), n);
    if (n == -1) {
        throwUnixException(env, errno);
    }
    return (jint)n;
}

/**
 * Copy stat64 members into sun.nio.fs.UnixFileAttributes
 */
static void prepAttributes(JNIEnv* env, struct stat64* buf, jobject attrs) {
    (*env)->SetIntField(env, attrs, attrs_st_mode, (jint)buf->st_mode);
    (*env)->SetLongField(env, attrs, attrs_st_ino, (jlong)buf->st_ino);
    (*env)->SetLongField(env, attrs, attrs_st_dev, (jlong)buf->st_dev);
    (*env)->SetLongField(env, attrs, attrs_st_rdev, (jlong)buf->st_rdev);
    (*env)->SetIntField(env, attrs, attrs_st_nlink, (jint)buf->st_nlink);
    (*env)->SetIntField(env, attrs, attrs_st_uid, (jint)buf->st_uid);
    (*env)->SetIntField(env, attrs, attrs_st_gid, (jint)buf->st_gid);
    (*env)->SetLongField(env, attrs, attrs_st_size, (jlong)buf->st_size);
    (*env)->SetLongField(env, attrs, attrs_st_atime, (jlong)buf->st_atime);
    (*env)->SetLongField(env, attrs, attrs_st_mtime, (jlong)buf->st_mtime);
    (*env)->SetLongField(env, attrs, attrs_st_ctime, (jlong)buf->st_ctime);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_stat0(JNIEnv* env, jclass this,
    jlong pathAddress, jobject attrs)
{
    int err;
    struct stat64 buf;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(stat64(path, &buf), err);
    if (err == -1) {
        throwUnixException(env, errno);
    } else {
        prepAttributes(env, &buf, attrs);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_lstat0(JNIEnv* env, jclass this,
    jlong pathAddress, jobject attrs)
{
    int err;
    struct stat64 buf;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(lstat64(path, &buf), err);
    if (err == -1) {
        throwUnixException(env, errno);
    } else {
        prepAttributes(env, &buf, attrs);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fstat(JNIEnv* env, jclass this, jint fd,
    jobject attrs)
{
    int err;
    struct stat64 buf;

    RESTARTABLE(fstat64((int)fd, &buf), err);
    if (err == -1) {
        throwUnixException(env, errno);
    } else {
        prepAttributes(env, &buf, attrs);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fstatat0(JNIEnv* env, jclass this, jint dfd,
    jlong pathAddress, jint flag, jobject attrs)
{
    int err;
    struct stat64 buf;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    if (my_fstatat64_func == NULL) {
        JNU_ThrowInternalError(env, "should not reach here");
        return;
    }
    RESTARTABLE((*my_fstatat64_func)((int)dfd, path, &buf, (int)flag), err);
    if (err == -1) {
        throwUnixException(env, errno);
    } else {
        prepAttributes(env, &buf, attrs);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_chmod0(JNIEnv* env, jclass this,
    jlong pathAddress, jint mode)
{
    int err;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(chmod(path, (mode_t)mode), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fchmod(JNIEnv* env, jclass this, jint filedes,
    jint mode)
{
    int err;

    RESTARTABLE(fchmod((int)filedes, (mode_t)mode), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_chown0(JNIEnv* env, jclass this,
    jlong pathAddress, jint uid, jint gid)
{
    int err;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(chown(path, (uid_t)uid, (gid_t)gid), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_lchown0(JNIEnv* env, jclass this, jlong pathAddress, jint uid, jint gid)
{
    int err;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(lchown(path, (uid_t)uid, (gid_t)gid), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fchown(JNIEnv* env, jclass this, jint filedes, jint uid, jint gid)
{
    int err;

    RESTARTABLE(fchown(filedes, (uid_t)uid, (gid_t)gid), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_utimes0(JNIEnv* env, jclass this,
    jlong pathAddress, jlong accessTime, jlong modificationTime)
{
    int err;
    struct timeval times[2];
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    times[0].tv_sec = accessTime / 1000000;
    times[0].tv_usec = accessTime % 1000000;

    times[1].tv_sec = modificationTime / 1000000;
    times[1].tv_usec = modificationTime % 1000000;

    RESTARTABLE(utimes(path, &times[0]), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}

/* Added path of file "filedes" for platform ports (only needed on AIX and HPUX) */
JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_futimes(JNIEnv* env, jclass this, jint filedes,
    jlong accessTime, jlong modificationTime, jobject path)
{
    struct timeval times[2];
    int err = 0;

    times[0].tv_sec = accessTime / 1000000;
    times[0].tv_usec = accessTime % 1000000;

    times[1].tv_sec = modificationTime / 1000000;
    times[1].tv_usec = modificationTime % 1000000;

#ifdef _ALLBSD_SOURCE
    RESTARTABLE(futimes(filedes, &times[0]), err);

    if (err == -1) {
        throwUnixException(env, errno);
    }
#else
    if (my_futimesat_func != NULL) {
        RESTARTABLE((*my_futimesat_func)(filedes, NULL, &times[0]), err);

        if (err == -1) {
            throwUnixException(env, errno);
        }
    }
    /* AIX and HP-UX does not provide futimes as system call => use utimes instead */
#if defined(_AIX) || defined (__hpux__)
    else {
        jobject buffer = (*env)->CallStaticObjectMethod(env, this, disp_copy2buf, path);
        jlong path_address = (*env)->GetLongField(env, buffer, buffer_address);
        const char* path = (const char*)jlong_to_ptr(path_address);
        RESTARTABLE(utimes(path, &times[0]), err);
        if (err == -1) {
            throwUnixException(env, errno);
        }
        (*env)->CallVoidMethod(env, buffer, buffer_release);
    }
#else
    else {
        JNU_ThrowInternalError(env, "my_ftimesat_func is NULL");
        return;
    }
#endif

#endif
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_opendir0(JNIEnv* env, jclass this,
    jlong pathAddress)
{
    DIR* dir;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    /* EINTR not listed as a possible error */
    dir = opendir(path);
    if (dir == NULL) {
        throwUnixException(env, errno);
    }
    return ptr_to_jlong(dir);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fdopendir(JNIEnv* env, jclass this, int dfd) {
    DIR* dir;

    if (my_fdopendir_func == NULL) {
        JNU_ThrowInternalError(env, "should not reach here");
        return (jlong)-1;
    }

    /* EINTR not listed as a possible error */
    dir = (*my_fdopendir_func)((int)dfd);
    if (dir == NULL) {
        throwUnixException(env, errno);
    }
    return ptr_to_jlong(dir);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_closedir(JNIEnv* env, jclass this, jlong dir) {
    int err;
    DIR* dirp = jlong_to_ptr(dir);

    RESTARTABLE(closedir(dirp), err);
    if (errno == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_readdir(JNIEnv* env, jclass this, jlong value) {
    struct dirent64* result;
    struct {
        struct dirent64 buf;
        char name_extra[PATH_MAX + 1 - sizeof result->d_name];
    } entry;
    struct dirent64* ptr = &entry.buf;

    int res;
    DIR* dirp = jlong_to_ptr(value);

    /* EINTR not listed as a possible error */
    /* TDB: reentrant version probably not required here */
    res = readdir64_r(dirp, ptr, &result);

    /* AIX returns EBADF for directory stream end which is no error. */
#ifdef _AIX
    if (res != 0) {
        res = (result == NULL && res == EBADF) ? 0 : errno;
    }
#endif

    if (res != 0) {
        throwUnixException(env, res);
        return NULL;
    } else {
        if (result == NULL) {
            return NULL;
        } else {
            jsize len = strlen(ptr->d_name);
            jbyteArray bytes = (*env)->NewByteArray(env, len);
            if (bytes != NULL) {
                (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)(ptr->d_name));
            }
            return bytes;
        }
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_mkdir0(JNIEnv* env, jclass this,
    jlong pathAddress, jint mode)
{
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    /* EINTR not listed as a possible error */
    if (mkdir(path, (mode_t)mode) == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_rmdir0(JNIEnv* env, jclass this,
    jlong pathAddress)
{
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    /* EINTR not listed as a possible error */
    if (rmdir(path) == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_link0(JNIEnv* env, jclass this,
    jlong existingAddress, jlong newAddress)
{
    int err;
    const char* existing = (const char*)jlong_to_ptr(existingAddress);
    const char* newname = (const char*)jlong_to_ptr(newAddress);

    RESTARTABLE(link(existing, newname), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_unlink0(JNIEnv* env, jclass this,
    jlong pathAddress)
{
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    /* EINTR not listed as a possible error */
    if (unlink(path) == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_unlinkat0(JNIEnv* env, jclass this, jint dfd,
                                               jlong pathAddress, jint flags)
{
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    if (my_unlinkat_func == NULL) {
        JNU_ThrowInternalError(env, "should not reach here");
        return;
    }

    /* EINTR not listed as a possible error */
    if ((*my_unlinkat_func)((int)dfd, path, (int)flags) == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_rename0(JNIEnv* env, jclass this,
    jlong fromAddress, jlong toAddress)
{
    const char* from = (const char*)jlong_to_ptr(fromAddress);
    const char* to = (const char*)jlong_to_ptr(toAddress);

    /* EINTR not listed as a possible error */
    if (rename(from, to) == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_renameat0(JNIEnv* env, jclass this,
    jint fromfd, jlong fromAddress, jint tofd, jlong toAddress)
{
    const char* from = (const char*)jlong_to_ptr(fromAddress);
    const char* to = (const char*)jlong_to_ptr(toAddress);

    if (my_renameat_func == NULL) {
        JNU_ThrowInternalError(env, "should not reach here");
        return;
    }

    /* EINTR not listed as a possible error */
    if ((*my_renameat_func)((int)fromfd, from, (int)tofd, to) == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_symlink0(JNIEnv* env, jclass this,
    jlong targetAddress, jlong linkAddress)
{
    const char* target = (const char*)jlong_to_ptr(targetAddress);
    const char* link = (const char*)jlong_to_ptr(linkAddress);

    /* EINTR not listed as a possible error */
    if (symlink(target, link) == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_readlink0(JNIEnv* env, jclass this,
    jlong pathAddress)
{
    jbyteArray result = NULL;
    char target[PATH_MAX+1];
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    /* EINTR not listed as a possible error */
    int n = readlink(path, target, sizeof(target));
    if (n == -1) {
        throwUnixException(env, errno);
    } else {
        jsize len;
        if (n == sizeof(target)) {
            n--;
        }
        target[n] = '\0';
        len = (jsize)strlen(target);
        result = (*env)->NewByteArray(env, len);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)target);
        }
    }
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_realpath0(JNIEnv* env, jclass this,
    jlong pathAddress)
{
    jbyteArray result = NULL;
    char resolved[PATH_MAX+1];
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    /* EINTR not listed as a possible error */
    if (realpath(path, resolved) == NULL) {
        throwUnixException(env, errno);
    } else {
        jsize len = (jsize)strlen(resolved);
        result = (*env)->NewByteArray(env, len);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)resolved);
        }
    }
    return result;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_access0(JNIEnv* env, jclass this,
    jlong pathAddress, jint amode)
{
    int err;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(access(path, (int)amode), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_statvfs0(JNIEnv* env, jclass this,
    jlong pathAddress, jobject attrs)
{
    int err;
    struct statvfs64 buf;
    const char* path = (const char*)jlong_to_ptr(pathAddress);


    RESTARTABLE(statvfs64(path, &buf), err);
    if (err == -1) {
        throwUnixException(env, errno);
    } else {
        /* Number of blocks (f_blocks) may be too big for signed long on AIX for /proc. */
#ifdef _AIX
        if (buf.f_blocks == ULONG_MAX) {
            buf.f_blocks = 0;
        }
        /* Number of free or available blocks can never exceed total number of blocks (seen on /QOpt as400) */
        if (buf.f_blocks == 0) {
        	buf.f_bfree = 0;
            buf.f_bavail = 0;
        }
#endif
        (*env)->SetLongField(env, attrs, attrs_f_frsize, long_to_jlong(buf.f_frsize));
        (*env)->SetLongField(env, attrs, attrs_f_blocks, long_to_jlong(buf.f_blocks));
        (*env)->SetLongField(env, attrs, attrs_f_bfree,  long_to_jlong(buf.f_bfree));
        (*env)->SetLongField(env, attrs, attrs_f_bavail, long_to_jlong(buf.f_bavail));
    }
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_pathconf0(JNIEnv* env, jclass this,
    jlong pathAddress, jint name)
{
    long err;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    err = pathconf(path, (int)name);
    if (err == -1) {
        throwUnixException(env, errno);
    }
    return (jlong)err;
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_fpathconf(JNIEnv* env, jclass this,
    jint fd, jint name)
{
    long err;

    err = fpathconf((int)fd, (int)name);
    if (err == -1) {
        throwUnixException(env, errno);
    }
    return (jlong)err;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_mknod0(JNIEnv* env, jclass this,
    jlong pathAddress, jint mode, jlong dev)
{
    int err;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    RESTARTABLE(mknod(path, (mode_t)mode, (dev_t)dev), err);
    if (err == -1) {
        throwUnixException(env, errno);
    }
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_getpwuid(JNIEnv* env, jclass this, jint uid)
{
    jbyteArray result = NULL;
    int buflen;
    char* pwbuf;

    /* allocate buffer for password record */
    buflen = (int)sysconf(_SC_GETPW_R_SIZE_MAX);
    if (buflen == -1)
        buflen = ENT_BUF_SIZE;
    pwbuf = (char*)malloc(buflen);
    if (pwbuf == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native heap");
    } else {
        struct passwd pwent;
        struct passwd* p = NULL;
        int res = 0;

        errno = 0;
        #ifdef __solaris__
            RESTARTABLE_RETURN_PTR(getpwuid_r((uid_t)uid, &pwent, pwbuf, (size_t)buflen), p);
        #else
            RESTARTABLE(getpwuid_r((uid_t)uid, &pwent, pwbuf, (size_t)buflen, &p), res);
        #endif

        if (res != 0 || p == NULL || p->pw_name == NULL || *(p->pw_name) == '\0') {
            /* not found or error */
            if (errno == 0)
	        /* getpwuid_r returns ESRCH if user does not exist on AIX */
#ifdef _AIX
	        errno = ESRCH;
#else
	        errno = ENOENT;
#endif
            throwUnixException(env, errno);
        } else {
            jsize len = strlen(p->pw_name);
            result = (*env)->NewByteArray(env, len);
            if (result != NULL) {
                (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)(p->pw_name));
            }
        }
        free(pwbuf);
    }

    return result;
}


JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_getgrgid(JNIEnv* env, jclass this, jint gid)
{
    jbyteArray result = NULL;
    int buflen;
    int retry;

    /* initial size of buffer for group record */
    buflen = (int)sysconf(_SC_GETGR_R_SIZE_MAX);
    if (buflen == -1)
        buflen = ENT_BUF_SIZE;

    do {
        struct group grent;
        struct group* g = NULL;
        int res = 0;

        char* grbuf = (char*)malloc(buflen);
        if (grbuf == NULL) {
            JNU_ThrowOutOfMemoryError(env, "native heap");
            return NULL;
        }

        errno = 0;
        #ifdef __solaris__
            RESTARTABLE_RETURN_PTR(getgrgid_r((gid_t)gid, &grent, grbuf, (size_t)buflen), g);
        #else
            RESTARTABLE(getgrgid_r((gid_t)gid, &grent, grbuf, (size_t)buflen, &g), res);
        #endif

        retry = 0;
        if (res != 0 || g == NULL || g->gr_name == NULL || *(g->gr_name) == '\0') {
            /* not found or error */
            if (errno == ERANGE) {
                /* insufficient buffer size so need larger buffer */
                buflen += ENT_BUF_SIZE;
                retry = 1;
            } else {
                if (errno == 0)
		    /* getgrgid_r returns ESRCH if group does not exist on AIX */
#ifdef _AIX
                    errno = ESRCH;
#else
                    errno = ENOENT;
#endif
                throwUnixException(env, errno);
            }
        } else {
            jsize len = strlen(g->gr_name);
            result = (*env)->NewByteArray(env, len);
            if (result != NULL) {
                (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)(g->gr_name));
            }
        }

        free(grbuf);

    } while (retry);

    return result;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_getpwnam0(JNIEnv* env, jclass this,
    jlong nameAddress)
{
    jint uid = -1;
    int buflen;
    char* pwbuf;

    /* allocate buffer for password record */
    buflen = (int)sysconf(_SC_GETPW_R_SIZE_MAX);
    if (buflen == -1)
        buflen = ENT_BUF_SIZE;
    pwbuf = (char*)malloc(buflen);
    if (pwbuf == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native heap");
    } else {
        struct passwd pwent;
        struct passwd* p = NULL;
        int res = 0;
        const char* name = (const char*)jlong_to_ptr(nameAddress);

        errno = 0;
        #ifdef __solaris__
            RESTARTABLE_RETURN_PTR(getpwnam_r(name, &pwent, pwbuf, (size_t)buflen), p);
        #else
            RESTARTABLE(getpwnam_r(name, &pwent, pwbuf, (size_t)buflen, &p), res);
        #endif

        if (res != 0 || p == NULL || p->pw_name == NULL || *(p->pw_name) == '\0') {
            /* not found or error */
            /* getpwnam_r returns ESRCH if user does not exist on AIX */
#ifdef _AIX
            if (errno != 0 && errno != ESRCH)
#else
            if (errno != 0 && errno != ENOENT)
#endif
                throwUnixException(env, errno);
        } else {
            uid = p->pw_uid;
        }
        free(pwbuf);
    }

    return uid;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_getgrnam0(JNIEnv* env, jclass this,
    jlong nameAddress)
{
    jint gid = -1;
    int buflen, retry;

    /* initial size of buffer for group record */
    buflen = (int)sysconf(_SC_GETGR_R_SIZE_MAX);
    if (buflen == -1)
        buflen = ENT_BUF_SIZE;

    do {
        struct group grent;
        struct group* g = NULL;
        int res = 0;
        char *grbuf;
        const char* name = (const char*)jlong_to_ptr(nameAddress);

        grbuf = (char*)malloc(buflen);
        if (grbuf == NULL) {
            JNU_ThrowOutOfMemoryError(env, "native heap");
            return -1;
        }

        errno = 0;
        #ifdef __solaris__
            RESTARTABLE_RETURN_PTR(getgrnam_r(name, &grent, grbuf, (size_t)buflen), g);
        #else
            RESTARTABLE(getgrnam_r(name, &grent, grbuf, (size_t)buflen, &g), res);
        #endif

        retry = 0;
        if (res != 0 || g == NULL || g->gr_name == NULL || *(g->gr_name) == '\0') {
            /* not found or error */
            /* getgrnam_r returns ESRCH if group does not exist on AIX */
#ifdef _AIX
            if (errno != 0 && errno != ESRCH) {
#else
            if (errno != 0 && errno != ENOENT) {
#endif
                if (errno == ERANGE) {
                    /* insufficient buffer size so need larger buffer */
                    buflen += ENT_BUF_SIZE;
                    retry = 1;
                } else {
                    throwUnixException(env, errno);
                }
            }
        } else {
            gid = g->gr_gid;
        }

        free(grbuf);

    } while (retry);

    return gid;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_getextmntent(JNIEnv* env, jclass this,
    jlong value, jobject entry)
{
/* AIX uses getmntctl instead of this method. */
#ifdef _AIX
    return 0;
#else
#ifdef __solaris__
    struct extmnttab ent;
#elif defined(_ALLBSD_SOURCE)
    char buf[1024];
    char *str;
    char *last;
#else
    struct mntent ent;
    char buf[1024];
    int buflen = sizeof(buf);
    struct mntent* m;
#endif
    FILE* fp = jlong_to_ptr(value);
    jsize len;
    jbyteArray bytes;
    char* name;
    char* dir;
    char* fstype;
    char* options;
    dev_t dev;

#ifdef __solaris__
    if (getextmntent(fp, &ent, 0))
        return -1;
    name = ent.mnt_special;
    dir = ent.mnt_mountp;
    fstype = ent.mnt_fstype;
    options = ent.mnt_mntopts;
    dev = makedev(ent.mnt_major, ent.mnt_minor);
    if (dev == NODEV) {
        /* possible bug on Solaris 8 and 9 */
        throwUnixException(env, errno);
        return -1;
    }
#elif defined(_ALLBSD_SOURCE)
again:
    if (!(str = fgets(buf, sizeof(buf), fp)))
        return -1;

    name = strtok_r(str, " \t\n", &last);
    if (name == NULL)
        return -1;

    // skip comments
    if (*name == '#')
        goto again;

    dir = strtok_r((char *)NULL, " \t\n", &last);
    fstype = strtok_r((char *)NULL, " \t\n", &last);
    options = strtok_r((char *)NULL, " \t\n", &last);
    if (options == NULL)
        return -1;
    dev = 0;
#else
    m = getmntent_r(fp, &ent, (char*)&buf, buflen);
    if (m == NULL)
        return -1;
    name = m->mnt_fsname;
    dir = m->mnt_dir;
    fstype = m->mnt_type;
    options = m->mnt_opts;
    dev = 0;
#endif

    len = strlen(name);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)name);
    (*env)->SetObjectField(env, entry, entry_name, bytes);

    len = strlen(dir);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)dir);
    (*env)->SetObjectField(env, entry, entry_dir, bytes);

    len = strlen(fstype);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)fstype);
    (*env)->SetObjectField(env, entry, entry_fstype, bytes);

    len = strlen(options);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)options);
    (*env)->SetObjectField(env, entry, entry_options, bytes);

    if (dev != 0)
        (*env)->SetLongField(env, entry, entry_dev, (jlong)dev);

    return 0;
#endif
}

/* Special implementation of getextmntent that returns all entries at once */
JNIEXPORT jobjectArray JNICALL
Java_sun_nio_fs_UnixNativeDispatcher_getmntctl(JNIEnv* env, jclass this)
{
#ifndef _AIX
    return NULL;
#else
    int must_free_buf = 0;
    char stack_buf[1024];
    char* buffer = stack_buf;
    size_t buffer_size = 1024;
    int num_entries;
    int i;
    jobjectArray ret;
    struct vmount * vm;

    for (i = 0; i < 5; i++) {
        num_entries = mntctl(MCTL_QUERY, buffer_size, buffer);
        if (num_entries != 0) {
            break;
        }
        if (must_free_buf) {
            free(buffer);
        }
        buffer_size *= 8;
        buffer = malloc(buffer_size);
        must_free_buf = 1;
    }
    /* Treat zero entries like errors. */
    if (num_entries <= 0) {
        if (must_free_buf) {
            free(buffer);
        }
        throwUnixException(env, errno);
        return NULL;
    }
    ret = (*env)->NewObjectArray(env, num_entries, entry_cls, NULL);
    if (ret == NULL) {
        if (must_free_buf) {
            free(buffer);
        }
        return NULL;
    }
    vm = (struct vmount*)buffer;
    for (i = 0; i < num_entries; i++) {
        jsize len;
        jbyteArray bytes;
        const char* fstype;
        /* We set all relevant attributes so there is no need to call constructor. */
        jobject entry = (*env)->AllocObject(env, entry_cls);
        if (entry == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetObjectArrayElement(env, ret, i, entry);

        /* vm->vmt_data[...].vmt_size is 32 bit aligned and also includes NULL byte. */
        /* Since we only need the characters, it is necessary to check string size manually. */
        len = strlen((char*)vm + vm->vmt_data[VMT_OBJECT].vmt_off);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)((char *)vm + vm->vmt_data[VMT_OBJECT].vmt_off));
        (*env)->SetObjectField(env, entry, entry_name, bytes);

        len = strlen((char*)vm + vm->vmt_data[VMT_STUB].vmt_off);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)((char *)vm + vm->vmt_data[VMT_STUB].vmt_off));
        (*env)->SetObjectField(env, entry, entry_dir, bytes);

        switch (vm->vmt_gfstype) {
            case MNT_J2:
                fstype = "jfs2";
                break;
            case MNT_NAMEFS:
                fstype = "namefs";
                break;
            case MNT_NFS:
                fstype = "nfs";
                break;
            case MNT_JFS:
                fstype = "jfs";
                break;
            case MNT_CDROM:
                fstype = "cdrom";
                break;
            case MNT_PROCFS:
                fstype = "procfs";
                break;
            case MNT_NFS3:
                fstype = "nfs3";
                break;
            case MNT_AUTOFS:
                fstype = "autofs";
                break;
            case MNT_UDF:
                fstype = "udfs";
                break;
            case MNT_NFS4:
                fstype = "nfs4";
                break;
            case MNT_CIFS:
                fstype = "smbfs";
                break;
            default:
                fstype = "unknown";
        }
        len = strlen(fstype);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)fstype);
        (*env)->SetObjectField(env, entry, entry_fstype, bytes);

        len = strlen((char*)vm + vm->vmt_data[VMT_ARGS].vmt_off);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)((char *)vm + vm->vmt_data[VMT_ARGS].vmt_off));
        (*env)->SetObjectField(env, entry, entry_options, bytes);

        /* goto the next vmount structure: */
        vm = (struct vmount *)((char *)vm + vm->vmt_length);
    }

    if (must_free_buf) {
        free(buffer);
    }
    return ret;
#endif
}
