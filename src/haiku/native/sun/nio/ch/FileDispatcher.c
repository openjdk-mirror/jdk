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
#include "sun_nio_ch_FileDispatcher.h"
#include <sys/types.h>
#include <sys/socket.h>
#include <fcntl.h>
#include <sys/uio.h>
#include "nio_util.h"

//#include "socketpair.h"

//#define USE_SOCKET

static int preCloseFD = -1;        /* File descriptor to which we dup other fd's
                                   before closing them for real */

/*
 * The pipe() function creates a pipe, which is an object
 * allowing unidirectional data flow, and allocates a pair
 * of file descriptors. The first descriptor connects to 
 * the read end of the pipe, and the second connects to 
 * the write end, so that data written to fildes[1] appears
 * on (i.e., can be read from) fildes[0]. This allows the
 * output of one program to be sent to another program: the
 * source's standard output is set up to be the write end
 * of the pipe, and the sink's standard input is set up to
 * be the read end of the pipe. The pipe itself persists
 * until all its associated descriptors are closed.
 *
 * A pipe whose read or write end has been closed is
 * considered widowed. Writing on such a pipe causes the
 * writing process to receive a SIGPIPE signal. Widowing a
 * pipe is the only way to deliver end-of-file to a reader:
 * after the reader consumes any buffered data, reading a
 * widowed pipe returns a zero count.
 */

/*
 * pipe(READER, WRITER);
 */

/*
 * Because piped filehandles are not bidirectional, each 
 * process uses just one of the pair and closes the
 * filehandle it doesn't use. The reason is subtle; picture
 * the situation where the reader does not close the
 * writable filehandle. If the writer then exits while the
 * reader is trying to read something, the reader will hang
 * forever. This is because the system won't tell the 
 * reader that there's no more data to be read until all
 * copies of the writable filehandle are closed.
 */

/*
 * perlipc:
 * Some systems defined pipe in terms of socketpair, in 
 * which a call to pipe(Rdr, Wtr) is essentially:
 *   use Socket;
 *   socketpair(Rdr, Wtr, AF_UNIX, SOCK_STREAM, PF_UNSPEC);
 *   shutdown(Rdr, 1);        # no more writing for reader
 *   shutdown(Wtr, 0);        # no more reading for writer
 */

JNIEXPORT void JNICALL
Java_sun_nio_ch_FileDispatcher_init(JNIEnv *env, jclass cl)
{
	int fd;
    int sp[2];

#ifdef HAS_SOCKETPAIR
	fprintf(stderr, "%s: using socketpair\n", __func__);
    if (socketpair(AF_INET, SOCK_STREAM, 0, sp) < 0)
        JNU_ThrowIOExceptionWithLastError(env, "socketpair failed");
    preCloseFD = sp[0];
    close(sp[1]);
#elif USE_SOCKET
	fprintf(stderr, "%s: using socket\n", __func__);
	preCloseFD = socket(AF_INET, SOCK_STREAM, 0);
#else
	fprintf(stderr, "%s: using pipe\n", __func__);
	if (pipe(sp) < 0)
		JNU_ThrowIOExceptionWithLastError(env, "pipe failed");
    preCloseFD = sp[0];
    close(sp[1]);
#endif
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcher_read0(JNIEnv *env, jclass clazz,
                             jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    void *buf = (void *)jlong_to_ptr(address);

    return convertReturnVal(env, read(fd, buf, len), JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcher_pread0(JNIEnv *env, jclass clazz, jobject fdo,
                            jlong address, jint len, jlong offset)
{
    jint fd = fdval(env, fdo);
    void *buf = (void *)jlong_to_ptr(address);

    return convertReturnVal(env, read_pos(fd, offset, buf, len), JNI_TRUE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileDispatcher_readv0(JNIEnv *env, jclass clazz,
                              jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    struct iovec *iov = (struct iovec *)jlong_to_ptr(address);
    if (len > 16) {
        len = 16;
    }
    return convertLongReturnVal(env, readv(fd, iov, len), JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcher_write0(JNIEnv *env, jclass clazz,
                              jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    void *buf = (void *)jlong_to_ptr(address);

    return convertReturnVal(env, write(fd, buf, len), JNI_FALSE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcher_pwrite0(JNIEnv *env, jclass clazz, jobject fdo,
                            jlong address, jint len, jlong offset)
{
    jint fd = fdval(env, fdo);
    void *buf = (void *)jlong_to_ptr(address);

    return convertReturnVal(env, write_pos(fd, offset, buf, len), JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileDispatcher_writev0(JNIEnv *env, jclass clazz,
                                       jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    struct iovec *iov = (struct iovec *)jlong_to_ptr(address);
    if (len > 16) {
        len = 16;
    }
    return convertLongReturnVal(env, writev(fd, iov, len), JNI_FALSE);
}

static void closeFileDescriptor(JNIEnv *env, int fd) {
    if (fd != -1) {
        int result = close(fd);
        if (result < 0)
            JNU_ThrowIOExceptionWithLastError(env, "Close failed");
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_FileDispatcher_close0(JNIEnv *env, jclass clazz, jobject fdo)
{
    jint fd = fdval(env, fdo);
	fprintf(stderr, "%s: check\n", __func__);
    closeFileDescriptor(env, fd);
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_FileDispatcher_preClose0(JNIEnv *env, jclass clazz, jobject fdo)
{
    jint fd = fdval(env, fdo);
	fprintf(stderr, "%s: check\n", __func__);
    if (preCloseFD >= 0) {
        if (dup2(preCloseFD, fd) < 0)
            JNU_ThrowIOExceptionWithLastError(env, "dup2 failed");
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_FileDispatcher_closeIntFD(JNIEnv *env, jclass clazz, jint fd)
{
	fprintf(stderr, "%s: check\n", __func__);
    closeFileDescriptor(env, fd);
}
