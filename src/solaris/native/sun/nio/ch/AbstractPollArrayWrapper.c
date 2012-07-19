/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
#include "sun_nio_ch_AbstractPollArrayWrapper.h"

#include <poll.h>
#include <stddef.h>

JNIEXPORT void JNICALL
Java_sun_nio_ch_AbstractPollArrayWrapper_getPollConstants(JNIEnv *env,
    jclass class, jshortArray pollConstants)
{
    jshort *constants = (*env)->GetShortArrayElements(env, pollConstants, NULL);
    if (constants == NULL) {
        return;
    }

    constants[sun_nio_ch_AbstractPollArrayWrapper_POLLIN_INDEX] = POLLIN;
    constants[sun_nio_ch_AbstractPollArrayWrapper_POLLOUT_INDEX] = POLLOUT;
    constants[sun_nio_ch_AbstractPollArrayWrapper_POLLERR_INDEX] = POLLERR;
    constants[sun_nio_ch_AbstractPollArrayWrapper_POLLHUP_INDEX] = POLLHUP;
    constants[sun_nio_ch_AbstractPollArrayWrapper_POLLNVAL_INDEX] = POLLNVAL;
    
#ifdef POLLREMOVE
    constants[sun_nio_ch_AbstractPollArrayWrapper_POLLREMOVE_INDEX] = POLLREMOVE;
#endif

    constants[sun_nio_ch_AbstractPollArrayWrapper_SIZE_POLLFD_INDEX] = sizeof(struct pollfd);
    constants[sun_nio_ch_AbstractPollArrayWrapper_FD_OFFSET_INDEX] = offsetof(struct pollfd, fd);
    constants[sun_nio_ch_AbstractPollArrayWrapper_EVENT_OFFSET_INDEX] = offsetof(struct pollfd, events);
    constants[sun_nio_ch_AbstractPollArrayWrapper_REVENT_OFFSET_INDEX] = offsetof(struct pollfd, revents);
    (*env)->ReleaseShortArrayElements(env, pollConstants, constants, 0);
}

