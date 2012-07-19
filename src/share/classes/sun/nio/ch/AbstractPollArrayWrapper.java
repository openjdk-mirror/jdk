/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import sun.misc.*;


/**
 * Manipulates a native array of pollfd structs.
 *
 * @author Mike McCloskey
 * @since 1.4
 */

abstract class AbstractPollArrayWrapper {

    private static final int POLLIN_INDEX = 0;
    private static final int POLLOUT_INDEX = 1;
    private static final int POLLERR_INDEX = 2;
    private static final int POLLHUP_INDEX = 3;
    private static final int POLLNVAL_INDEX = 4;
    private static final int POLLREMOVE_INDEX = 5;

    private static final int SIZE_POLLFD_INDEX = 6;
    private static final int FD_OFFSET_INDEX = 7;
    private static final int EVENT_OFFSET_INDEX = 8;
    private static final int REVENT_OFFSET_INDEX = 9;

    private static final int CONSTANTS_ARRAY_SIZE = 10;

    private static native void getPollConstants(short[] pollConstants);

    static {
        System.loadLibrary("nio");

        short[] pollConstants = new short[CONSTANTS_ARRAY_SIZE];
        getPollConstants(pollConstants);

        POLLIN = pollConstants[POLLIN_INDEX];
        POLLOUT = pollConstants[POLLOUT_INDEX];
        POLLERR = pollConstants[POLLERR_INDEX];
        POLLHUP = pollConstants[POLLHUP_INDEX];
        POLLNVAL = pollConstants[POLLNVAL_INDEX];
        POLLREMOVE = pollConstants[POLLREMOVE_INDEX];
        SIZE_POLLFD = pollConstants[SIZE_POLLFD_INDEX];
        FD_OFFSET = pollConstants[FD_OFFSET_INDEX];
        EVENT_OFFSET = pollConstants[EVENT_OFFSET_INDEX];
        REVENT_OFFSET = pollConstants[REVENT_OFFSET_INDEX];
    }

    // Event masks
    static final short POLLIN;
    static final short POLLOUT;
    static final short POLLERR;
    static final short POLLHUP;
    static final short POLLNVAL;
    static final short POLLREMOVE;

    // Miscellaneous constants
    static final short SIZE_POLLFD;
    static final short FD_OFFSET;
    static final short EVENT_OFFSET;
    static final short REVENT_OFFSET;

    // The poll fd array
    protected AllocatedNativeObject pollArray;

    // Number of valid entries in the pollArray
    protected int totalChannels = 0;

    // Base address of the native pollArray
    protected long pollArrayAddress;

    // Access methods for fd structures
    int getEventOps(int i) {
        int offset = SIZE_POLLFD * i + EVENT_OFFSET;
        return pollArray.getShort(offset);
    }

    int getReventOps(int i) {
        int offset = SIZE_POLLFD * i + REVENT_OFFSET;
        return pollArray.getShort(offset);
    }

    int getDescriptor(int i) {
        int offset = SIZE_POLLFD * i + FD_OFFSET;
        return pollArray.getInt(offset);
    }

    void putEventOps(int i, int event) {
        int offset = SIZE_POLLFD * i + EVENT_OFFSET;
        pollArray.putShort(offset, (short)event);
    }

    void putReventOps(int i, int revent) {
        int offset = SIZE_POLLFD * i + REVENT_OFFSET;
        pollArray.putShort(offset, (short)revent);
    }

    void putDescriptor(int i, int fd) {
        int offset = SIZE_POLLFD * i + FD_OFFSET;
        pollArray.putInt(offset, fd);
    }

}
