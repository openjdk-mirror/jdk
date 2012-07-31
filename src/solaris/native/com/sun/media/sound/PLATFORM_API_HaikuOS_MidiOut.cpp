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

#include <MidiConsumer.h>
#include <MidiProducer.h>

#include <string.h>

extern "C" {
#include "PlatformMidi.h"
}

#include "PLATFORM_API_HaikuOS_Utils.h"


MidiDeviceCache midiCache;


static int CHANNEL_MESSAGE_LENGTH[] = {
    -1, -1, -1, -1, -1, -1, -1, -1, 3, 3, 3, 3, 2, 2, 3 };
/*                                 8x 9x Ax Bx Cx Dx Ex */


static int SYSTEM_MESSAGE_LENGTH[] = {
    -1, 2, 3, 2, -1, -1, 1, 1, 1, -1, 1, 1, 1, -1, 1, 1 };
/*  F0 F1 F2 F3  F4  F5 F6 F7 F8  F9 FA FB FC  FD FE FF */


// the returned length includes the status byte.
// for illegal messages, -1 is returned.
static int getShortMessageLength(int status) {
        int     dataLength = 0;
        if (status < 0xF0) { // channel voice message
                dataLength = CHANNEL_MESSAGE_LENGTH[(status >> 4) & 0xF];
        } else {
                dataLength = SYSTEM_MESSAGE_LENGTH[status & 0xF];
        }
        return dataLength;
}


char* MIDI_OUT_GetErrorStr(INT32 err) {
    return strerror(err);
}


INT32 MIDI_OUT_GetNumDevices() {
    return midiCache.ConsumerCount();
}


INT32 MIDI_OUT_GetDeviceName(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiConsumer* consumer;
    if (midiCache.GetConsumer(deviceIndex, &consumer) == B_OK) {
        strlcpy(name, consumer->Name(), nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_OUT_GetDeviceVendor(INT32 deviceIndex, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


INT32 MIDI_OUT_GetDeviceDescription(INT32 deviceIndex, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


INT32 MIDI_OUT_GetDeviceVersion(INT32 deviceIndex, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


struct MidiOutHandle {
    BMidiLocalProducer* localProducer;
    BMidiConsumer* remoteConsumer;
};


INT32 MIDI_OUT_OpenDevice(INT32 deviceIndex, MidiDeviceHandle** handle) {
    BMidiConsumer* consumer;
    if (midiCache.GetConsumer(deviceIndex, &consumer) != B_OK) {
        return MIDI_INVALID_DEVICEID;
    }

    BMidiLocalProducer* producer = new(std::nothrow) BMidiLocalProducer();
    if (producer == NULL) {
        return MIDI_OUT_OF_MEMORY;
    }

    // is this check necessary?
    if (!producer->IsValid()) {
        producer->Release();
        return MIDI_INVALID_DEVICEID;
    }

    status_t result = producer->Connect(consumer);
    if (result != B_OK) {
        producer->Release();
        return result;
    }

    MidiOutHandle* outHandle = new(std::nothrow) MidiOutHandle();
    if (outHandle == NULL) {
        producer->Release();
        return MIDI_OUT_OF_MEMORY;
    }

    outHandle->localProducer = producer;
    outHandle->remoteConsumer = consumer;

    *handle = new(std::nothrow) MidiDeviceHandle();
    if (*handle == NULL) {
    	delete outHandle;
    	producer->Release();
    	return MIDI_OUT_OF_MEMORY;
    }

    (*handle)->deviceHandle = (void*)outHandle;
    (*handle)->startTime = system_time();
    return MIDI_SUCCESS;
}


INT32 MIDI_OUT_CloseDevice(MidiDeviceHandle* handle) {
    MidiOutHandle* outHandle = (MidiOutHandle*)handle->deviceHandle;

    outHandle->localProducer->Disconnect(outHandle->remoteConsumer);

    outHandle->localProducer->Release();
    delete outHandle;
    delete handle;

    return MIDI_SUCCESS;
}


INT64 MIDI_OUT_GetTimeStamp(MidiDeviceHandle* handle) {
    return system_time() - handle->startTime;
}


INT32 MIDI_OUT_SendShortMessage(MidiDeviceHandle* handle, UINT32 packedMsg,
                                UINT32 timestamp) {
    MidiOutHandle* outHandle = (MidiOutHandle*)handle->deviceHandle;

    UBYTE message[3];
    message[0] = (UBYTE)(packedMsg & 0xff); // status
    message[1] = (UBYTE)((packedMsg >> 8) & 0xff); // possible data1
    message[2] = (UBYTE)((packedMsg >> 16) & 0xff); // possible data2

    size_t length = getShortMessageLength((int)message[0]);
    if (length == -1) {
        return MIDI_INVALID_ARGUMENT;
    }

    outHandle->localProducer->SprayData((void*)message, length, true,
        (bigtime_t)timestamp);

    return MIDI_SUCCESS;
}


INT32 MIDI_OUT_SendLongMessage(MidiDeviceHandle* handle, UBYTE* data,
                               UINT32 size, UINT32 timestamp) {
    MidiOutHandle* outHandle = (MidiOutHandle*)handle->deviceHandle;

    outHandle->localProducer->SprayData((void*)data, size, true,
        (bigtime_t)timestamp);

    return MIDI_SUCCESS;
}


#endif /* USE_PLATFORM_MIDI_OUT */
