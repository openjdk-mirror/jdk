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

#if USE_PLATFORM_MIDI_IN == TRUE

extern "C" {
#include "PlatformMidi.h"
}

#include "PLATFORM_API_HaikuOS_Utils.h"


extern MidiDeviceCache midiCache;


char* MIDI_IN_GetErrorStr(INT32 err) {
    // TODO
}


INT32 MIDI_IN_GetNumDevices() {
    return cache.ProducerCount();
}


INT32 MIDI_IN_GetDeviceName(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiProducer* producer;
    if (cache.GetProducer(deviceIndex, &producer) == B_OK) {
        strlcpy(name, producer->Name(), nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_IN_GetDeviceVendor(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiProducer* producer;
    if (cache.GetProducer(deviceIndex, &producer) == B_OK) {
        // cannot fill in
        strlcpy(name, "", nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_IN_GetDeviceDescription(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiProducer* producer;
    if (cache.GetProducer(deviceIndex, &producer) == B_OK) {
        // cannot fill in
        strlcpy(name, "", nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_IN_GetDeviceVersion(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiProducer* producer;
    if (cache.GetProducer(deviceIndex, &producer) == B_OK) {
        // cannot fill in
        strlcpy(name, "", nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_IN_OpenDevice(INT32 deviceIndex, MidiDeviceHandle** handle) {
}


INT32 MIDI_IN_CloseDevice(MidiDeviceHandle* handle) {
}


INT32 MIDI_IN_StartDevice(MidiDeviceHandle* handle) {
}


INT32 MIDI_IN_StopDevice(MidiDeviceHandle* handle) {
}


INT64 MIDI_IN_GetTimeStamp(MidiDeviceHandle* handle) {
}


MidiMessage* MIDI_IN_GetMessage(MidiDeviceHandle* handle) {
}


void MIDI_IN_ReleaseMessage(MidiDeviceHandle* handle, MidiMessage* msg) {
}

#endif /* USE_PLATFORM_MIDI_IN */
