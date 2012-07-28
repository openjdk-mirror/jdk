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

#define USE_ERROR
#define USE_TRACE

#if USE_PLATFORM_MIDI_OUT == TRUE

extern "C" {
#include "PlatformMidi.h"
}

#include "PLATFORM_API_HaikuOS_Utils.h"


extern MidiDeviceCache midiCache;


char* MIDI_OUT_GetErrorStr(INT32 err) {
    // TODO
}


INT32 MIDI_OUT_GetNumDevices() {
    return cache.ConsumerCount();
}


INT32 MIDI_OUT_GetDeviceName(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiConsumer* consumer;
    if (cache.GetConsumer(deviceIndex, &consumer) == B_OK) {
        strlcpy(name, consumer->Name(), nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_OUT_GetDeviceVendor(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiConsumer* consumer;
    if (cache.GetConsumer(deviceIndex, &consumer) == B_OK) {
        strlcpy(name, "", nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_OUT_GetDeviceDescription(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiConsumer* consumer;
    if (cache.GetConsumer(deviceIndex, &consumer) == B_OK) {
        strlcpy(name, "", nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_OUT_GetDeviceVersion(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiConsumer* consumer;
    if (cache.GetConsumer(deviceIndex, &consumer) == B_OK) {
        strlcpy(name, "", nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_OUT_OpenDevice(INT32 deviceIndex, MidiDeviceHandle** handle) {
}


INT32 MIDI_OUT_CloseDevice(MidiDeviceHandle* handle) {
}


INT64 MIDI_OUT_GetTimeStamp(MidiDeviceHandle* handle) {
}


INT32 MIDI_OUT_SendShortMessage(MidiDeviceHandle* handle, UINT32 packedMsg,
                                UINT32 timestamp) {
}


INT32 MIDI_OUT_SendLongMessage(MidiDeviceHandle* handle, UBYTE* data,
                               UINT32 size, UINT32 timestamp) {
}


#endif /* USE_PLATFORM_MIDI_OUT */
