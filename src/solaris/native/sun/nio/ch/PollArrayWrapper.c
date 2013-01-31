/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "sun_nio_ch_PollArrayWrapper.h"
#include <poll.h>
#include <unistd.h>
#include <sys/time.h>

#ifdef _AIX
/* hard coded values in java code do not match system header values */
#include <string.h>
#include <stdlib.h>
#define JAVA_POLLIN        0x0001
#define JAVA_POLLOUT       0x0004
#define JAVA_POLLERR       0x0008
#define JAVA_POLLHUP       0x0010
#define JAVA_POLLNVAL      0x0020
#define JAVA_POLLREMOVE    0x0800  /* not used yet in Java but ment to deregister fd */
#endif

#define RESTARTABLE(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == -1) && (errno == EINTR)); \
} while(0)

static int
ipoll(struct pollfd fds[], unsigned int nfds, int timeout)
{
    jlong start, now;
    int remaining = timeout;
    struct timeval t;
    int diff;

    gettimeofday(&t, NULL);
    start = t.tv_sec * 1000 + t.tv_usec / 1000;

    for (;;) {
        int res = poll(fds, nfds, remaining);
        if (res < 0 && errno == EINTR) {
            if (remaining >= 0) {
                gettimeofday(&t, NULL);
                now = t.tv_sec * 1000 + t.tv_usec / 1000;
                diff = now - start;
                remaining -= diff;
                if (diff < 0 || remaining <= 0) {
                    return 0;
                }
                start = now;
            }
        } else {
            return res;
        }
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_PollArrayWrapper_poll0(JNIEnv *env, jobject this,
                                       jlong address, jint numfds,
                                       jlong timeout)
{
    struct pollfd *a;
    int err = 0;

    a = (struct pollfd *) jlong_to_ptr(address);

#ifdef _AIX
    /* hard coded values in java code do not match system header values */
    {
        int i;
        struct pollfd *aa = (struct pollfd*) malloc(numfds * sizeof(struct pollfd));
        if (aa == NULL) {
            JNU_ThrowIOExceptionWithLastError(env, "Poll failed");
            return (jint)-1;
        }
        memcpy(aa, a, numfds * sizeof(struct pollfd));
        /* translate hardcoded java (SOLARIS) event flags to AIX event flags */
        for (i=0; i<numfds; i++) {
            aa[i].events = 0;
            if (a[i].events & JAVA_POLLIN)     aa[i].events |= POLLIN;
            if (a[i].events & JAVA_POLLOUT)    aa[i].events |= POLLOUT;
            if (a[i].events & JAVA_POLLERR)    aa[i].events |= POLLERR;
            if (a[i].events & JAVA_POLLHUP)    aa[i].events |= POLLHUP;
            if (a[i].events & JAVA_POLLNVAL)   aa[i].events |= POLLNVAL;
            /* For the time being we don't support JAVA_POLLREMOVE. */
        }
        if (timeout <= 0) {           /* Indefinite or no wait */
            RESTARTABLE (poll(aa, numfds, timeout), err);
        } else {                      /* Bounded wait; bounded restarts */
            err = ipoll(aa, numfds, timeout);
        }
        /* transfer resulting event flags back to the hardcoded java event flags */
        for (i=0; i<numfds; i++) {
            if (aa[i].revents & POLLIN)   a[i].revents |= JAVA_POLLIN;
            if (aa[i].revents & POLLOUT)  a[i].revents |= JAVA_POLLOUT;
            if (aa[i].revents & POLLERR)  a[i].revents |= JAVA_POLLERR;
            if (aa[i].revents & POLLHUP)  a[i].revents |= JAVA_POLLHUP;
            if (aa[i].revents & POLLNVAL) a[i].revents |= JAVA_POLLNVAL;
        }
        free(aa);
    }
#else
    if (timeout <= 0) {           /* Indefinite or no wait */
        RESTARTABLE (poll(a, numfds, timeout), err);
    } else {                      /* Bounded wait; bounded restarts */
        err = ipoll(a, numfds, timeout);
    }
#endif

    if (err < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Poll failed");
    }
    return (jint)err;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_PollArrayWrapper_interrupt(JNIEnv *env, jobject this, jint fd)
{
    int fakebuf[1];
    fakebuf[0] = 1;
    if (write(fd, fakebuf, 1) < 0) {
         JNU_ThrowIOExceptionWithLastError(env,
                                          "Write to interrupt fd failed");
    }
}
