/*
 * Copyright 2000-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
//#include <sys/mman.h>
#include <sys/stat.h>
#include "sun_nio_ch_FileChannelImpl.h"
#include "java_lang_Integer.h"
#include "nio.h"
#include "nio_util.h"
#include <OS.h>
#include <File.h>
#include <dlfcn.h>

static jfieldID chan_fd;        /* jobject 'fd' in sun.io.FileChannelImpl */

JNIEXPORT jlong JNICALL 
Java_sun_nio_ch_FileChannelImpl_initIDs(JNIEnv *env, jclass clazz)
{
    jlong pageSize = B_PAGE_SIZE;
    chan_fd = env->GetFieldID(clazz, "fd", "Ljava/io/FileDescriptor;");
    return pageSize;
}

static jlong
handle(JNIEnv *env, jlong rv, char *msg)
{
    if (rv >= 0)
        return rv;
    if (errno == EINTR)
        return IOS_INTERRUPTED;
    JNU_ThrowIOExceptionWithLastError(env, msg);
    return IOS_THROWN;
}


JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileChannelImpl_map0(JNIEnv *env, jobject object,
                                     jint prot, jlong off, jlong len)
{
    void *mapAddress = 0;
    jobject fdo = env->GetObjectField(object, chan_fd);
    jint fd = fdval(env, fdo);
	int protections = 0;
	long pos = 0;
	area_id area = -1;
	ssize_t size = -1;
	long area_len = B_PAGE_SIZE * (len / B_PAGE_SIZE + (len % B_PAGE_SIZE != 0));
    if (prot == sun_nio_ch_FileChannelImpl_MAP_RO) {
        protections = B_READ_AREA;
    } else if (prot == sun_nio_ch_FileChannelImpl_MAP_RW) {
		fprintf(stderr, "WARNING: WRITABLE MAPS UNSUPPORTED\n");
        protections = B_WRITE_AREA | B_READ_AREA;
    } else if (prot == sun_nio_ch_FileChannelImpl_MAP_PV) {
        protections = B_WRITE_AREA | B_READ_AREA;
    }
	area = create_area("FileChannelImpl.mmap", &mapAddress, B_ANY_ADDRESS, 
	                   area_len, B_NO_LOCK, B_WRITE_AREA | B_READ_AREA);
	if (area < 0) {
		return handle(env, -1, "Map failed");
	}
	size = read_pos(fd, off, mapAddress, len);
	if (size != len) {
		delete_area(area);
		return handle(env, -1, "Map failed");
	}
	set_area_protection(area, protections);
    return ptr_to_jlong(mapAddress);
}


JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileChannelImpl_unmap0(JNIEnv *env, jobject object,
                                       jlong address, jlong len)
{
    void *a = (void *)jlong_to_ptr(address);
	debugger(__func__);
    return 0;
}


JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileChannelImpl_truncate0(JNIEnv *env, jobject object,
                                          jobject fdo, jlong size)
{
	debugger(__func__);
    return 0;
}


JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileChannelImpl_force0(JNIEnv *env, jobject object,
                                       jobject fdo, jboolean md)
{
    jint fd = fdval(env, fdo);
    int result = 0;
	debugger(__func__);
    return 0;
}


JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileChannelImpl_position0(JNIEnv *env, jobject object,
                                          jobject fdo, jlong offset)
{
    jint fd = fdval(env, fdo);
	debugger(__func__);
    return offset;
}


JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileChannelImpl_size0(JNIEnv *env, jobject object, jobject fdo)
{
    struct stat fbuf;
    if (fstat(fdval(env, fdo), &fbuf) < 0)
        return handle(env, -1, "Size failed");
    return fbuf.st_size;
}


JNIEXPORT void JNICALL
Java_sun_nio_ch_FileChannelImpl_close0(JNIEnv *env, jobject object, jobject fdo)
{
    jint fd = fdval(env, fdo);
    if (fd != -1) {
        jlong result = close(fd);
        if (result < 0) {
            JNU_ThrowIOExceptionWithLastError(env, "Close failed");
        }
    }
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileChannelImpl_transferTo0(JNIEnv *env, jobject object,
                                            jint srcFD,
                                            jlong position, jlong count,
                                            jint dstFD)
{
	debugger(__func__);
#ifdef __linux__
    off_t offset = (off_t)position;
    jlong n = sendfile(dstFD, srcFD, &offset, (size_t)count);
    if (n < 0) {
        jlong max = (jlong)java_lang_Integer_MAX_VALUE;
        if (count > max) {
            n = sendfile(dstFD, srcFD, &offset, max);
            if (n >= 0) {
                return n;
            }
        }
        JNU_ThrowIOExceptionWithLastError(env, "Transfer failed");
    }
    return n;
#endif

#ifdef __solaris__
    if (my_sendfile_func == NULL) {
        return IOS_UNSUPPORTED;
    } else {
        sendfilevec_t sfv;
        size_t numBytes = 0;
        jlong result;

        sfv.sfv_fd = srcFD;
        sfv.sfv_flag = 0;
        sfv.sfv_off = position;
        sfv.sfv_len = count;

        result = (*my_sendfile_func)(dstFD, &sfv, 1, &numBytes);

        /* Solaris sendfilev() will return -1 even if some bytes have been
         * transferred, so we check numBytes first.
         */
        if (numBytes > 0)
            return numBytes;
        if (result < 0) {
            if (errno == EAGAIN)
                return IOS_UNAVAILABLE;
            if (errno == EINTR)
                return IOS_INTERRUPTED;
            JNU_ThrowIOExceptionWithLastError(env, "Transfer failed");
            return IOS_THROWN;
        }
        return result;
    }
#endif
}


typedef struct flock FLOCK;

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileChannelImpl_lock0(JNIEnv *env, jobject object, jobject fdo,
                                      jboolean block, jlong pos, jlong size,
                                      jboolean shared)
{
    jint fd = fdval(env, fdo);
    jint lockResult = 0;
    int cmd = 0;
    FLOCK fl;

	debugger(__func__);

    if (size > 0x7fffffff) {
        size = 0x7fffffff;
    }

    fl.l_whence = SEEK_SET;
    fl.l_len = (off_t)size;
    fl.l_start = (off_t)pos;
    if (shared == JNI_TRUE) {
        fl.l_type = F_RDLCK;
    } else {
        fl.l_type = F_WRLCK;
    }
    if (block == JNI_TRUE) {
        cmd = F_SETLKW;
    } else {
        cmd = F_SETLK;
    }
    lockResult = fcntl(fd, cmd, &fl);
    if (lockResult < 0) {
        if ((cmd == F_SETLK) && (errno == EAGAIN))
            return sun_nio_ch_FileChannelImpl_NO_LOCK;
        if (errno == EINTR)
            return sun_nio_ch_FileChannelImpl_INTERRUPTED;
        JNU_ThrowIOExceptionWithLastError(env, "Lock failed");
    }
    return 0;
}


JNIEXPORT void JNICALL
Java_sun_nio_ch_FileChannelImpl_release0(JNIEnv *env, jobject object,
                                         jobject fdo, jlong pos, jlong size)
{
    jint fd = fdval(env, fdo);
    jint lockResult = 0;
    FLOCK fl;
    int cmd = F_SETLK;

	debugger(__func__);

    if (size > 0x7fffffff) {
        size = 0x7fffffff;
    }

    fl.l_whence = SEEK_SET;
    fl.l_len = (off_t)size;
    fl.l_start = (off_t)pos;
    fl.l_type = F_UNLCK;
    lockResult = fcntl(fd, cmd, &fl);
    if (lockResult < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Release failed");
    }
}
