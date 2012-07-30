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

#include <MidiConsumer.h>
#include <MidiProducer.h>

extern "C" {
#include "PlatformMidi.h"
}

#include "PLATFORM_API_HaikuOS_Utils.h"


extern MidiDeviceCache midiCache;


char* MIDI_IN_GetErrorStr(INT32 err) {
    return strerror(err);
}


INT32 MIDI_IN_GetNumDevices() {
    return midiCache.ProducerCount();
}


INT32 MIDI_IN_GetDeviceName(INT32 deviceIndex, char *name, UINT32 nameLength) {
    BMidiProducer* producer;
    if (midiCache.GetProducer(deviceIndex, &producer) == B_OK) {
        strlcpy(name, producer->Name(), nameLength);
        return MIDI_SUCCESS;
    }

    return MIDI_INVALID_DEVICEID;
}


INT32 MIDI_IN_GetDeviceVendor(INT32 deviceIndex, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


INT32 MIDI_IN_GetDeviceDescription(INT32 deviceIndex, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


INT32 MIDI_IN_GetDeviceVersion(INT32 deviceIndex, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


struct MidiInHandle {
    BMidiLocalConsumer* localConsumer;
    BMidiProducer* remoteProducer;
    sem_id queueSemaphore;
    bool started;
};


class MidiQueueConsumer : public BMidiLocalConsumer {
public:
    MidiQueueConsumer(MidiDeviceHandle* handle) : fHandle(handle),
        fInHandle((MidiInHandle*)handle->deviceHandle) {
    }

    virtual void Data(uchar* data, size_t length, bool atomic, bigtime_t time) {
        if (!atomic || length <= 0) {
            return;
        }

        // TODO locking? the Windows version doesn't
        if (fInHandle->started) {
            // "short" message is <= 3 bytes, otherwise "long" message
            if (length <= 3) {
                // store platform-endian status | data1<<8 | data2<<16
                UINT32 packedMsg = (data[0] & 0xff) 
                    | (length >= 2 ? ((data[1] & 0xff) << 8) : 0)
                    | (length >= 3 ? ((data[2] & 0xff) << 16) : 0);
                MIDI_QueueAddShort(fHandle->queue, packedMsg, (INT64)time, TRUE);
            } else {
                // we need to make a copy of the data
                UBYTE* dataCopy = (UBYTE*)malloc(length);
                if (dataCopy == NULL) {
                    return;
                }

                memcpy(dataCopy, data, length);
                MIDI_QueueAddLong(fHandle->queue, dataCopy, length, 0, (INT64)time,
                    TRUE);
            }
            release_sem(fInHandle->queueSemaphore);
        }
    }

private:
    MidiDeviceHandle* fHandle;
    MidiInHandle* fInHandle;
};


INT32 MIDI_IN_OpenDevice(INT32 deviceIndex, MidiDeviceHandle** handle) {
    BMidiProducer* producer;
    if (midiCache.GetProducer(deviceIndex, &producer) != B_OK) {
        return MIDI_INVALID_DEVICEID;
    }

    *handle = new(std::nothrow) MidiDeviceHandle();
    if (*handle == NULL) {
        return MIDI_OUT_OF_MEMORY;
    }
    
    BMidiLocalConsumer* consumer = new(std::nothrow) MidiQueueConsumer(*handle);
    if (consumer == NULL) {
        delete *handle;
        *handle = NULL;
        return MIDI_OUT_OF_MEMORY;
    }

    // is this check necessary?
    if (!consumer->IsValid()) {
        consumer->Release();
        delete *handle;
        *handle = NULL;
        return MIDI_INVALID_DEVICEID;
    }

    MidiInHandle* inHandle = new(std::nothrow) MidiInHandle();
    if (inHandle == NULL) {
        consumer->Release();
        delete *handle;
        *handle = NULL;
        return MIDI_OUT_OF_MEMORY;
    }

    inHandle->started = false;
    inHandle->localConsumer = consumer;
    inHandle->remoteProducer = producer;

    inHandle->queueSemaphore = create_sem(0, "javaMidiQueueSem");
    if (inHandle->queueSemaphore < B_OK) {
        delete inHandle;
        consumer->Release();
        delete *handle;
        *handle = NULL;

        if (inHandle->queueSemaphore == B_NO_MEMORY) {
            return MIDI_OUT_OF_MEMORY;
        } else {
            return inHandle->queueSemaphore;
        }
    }

    (*handle)->deviceHandle = (void*)inHandle;
    (*handle)->queue = MIDI_CreateQueue(MIDI_IN_MESSAGE_QUEUE_SIZE);

    if ((*handle)->queue == NULL) {
        delete_sem(inHandle->queueSemaphore);
        delete inHandle;
        consumer->Release();
        delete *handle;
        *handle = NULL;
        return MIDI_OUT_OF_MEMORY;
    }

    (*handle)->startTime = system_time();
    return MIDI_SUCCESS;
}


INT32 MIDI_IN_CloseDevice(MidiDeviceHandle* handle) {
    MidiInHandle* inHandle = (MidiInHandle*)handle->deviceHandle;

    inHandle->started = false;
    inHandle->remoteProducer->Disconnect(inHandle->localConsumer);
    inHandle->localConsumer->Release();
    delete_sem(inHandle->queueSemaphore);
    delete inHandle;

    MIDI_DestroyQueue(handle->queue);
    delete handle;

    return MIDI_SUCCESS;
}


INT32 MIDI_IN_StartDevice(MidiDeviceHandle* handle) {
    MidiInHandle* inHandle = (MidiInHandle*)handle->deviceHandle;
    inHandle->started = true;
    status_t result = inHandle->remoteProducer->Connect(inHandle->localConsumer);

    if (result == B_OK) {
        return MIDI_SUCCESS;
    } else {
        return result;
    }
}


INT32 MIDI_IN_StopDevice(MidiDeviceHandle* handle) {
    MidiInHandle* inHandle = (MidiInHandle*)handle->deviceHandle;
    inHandle->started = false;
    inHandle->remoteProducer->Disconnect(inHandle->localConsumer);

    return MIDI_SUCCESS;
}


INT64 MIDI_IN_GetTimeStamp(MidiDeviceHandle* handle) {
    return system_time() - handle->startTime;
}


MidiMessage* MIDI_IN_GetMessage(MidiDeviceHandle* handle) {
    MidiInHandle* inHandle = (MidiInHandle*)handle->deviceHandle;

    // timeout after 2 seconds like the Windows version
    if (acquire_sem_etc(inHandle->queueSemaphore, 1, B_RELATIVE_TIMEOUT,
            2 * 1000 * 1000) == B_OK) {
        MidiMessage* msg = MIDI_QueueRead(handle->queue);
        return msg;
    }

    return NULL;
}


void MIDI_IN_ReleaseMessage(MidiDeviceHandle* handle, MidiMessage* msg) {
    if (msg->type == LONG_MESSAGE) {
        // we allocated the LONG_MESSAGE data buffer with malloc
        free(msg->data.l.data);
    }

    MIDI_QueueRemove(handle->queue, TRUE);
}

#endif /* USE_PLATFORM_MIDI_IN */
