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

extern "C" {
#include "DirectAudio.h"
}

#include "PLATFORM_API_HaikuOS_Utils.h"

#include <MediaDefs.h>
#include <MediaNode.h>
#include <MediaRoster.h>
#include <SoundPlayer.h>

#include <vector>

#if USE_DAUDIO == TRUE

DeviceCache cache;

extern "C" {

INT32 DAUDIO_GetDirectAudioDeviceCount() {
    return cache.DeviceCount();
}

INT32 DAUDIO_GetDirectAudioDeviceDescription(INT32 mixerIndex,
                                             DirectAudioDeviceDescription* description) {
    live_node_info info;
    
    if (cache.GetDevice(mixerIndex, &info) != B_OK)
        return FALSE;

    strlcpy(description->name, info.name, DAUDIO_STRING_LENGTH);
    // Mac OS X port sets this to -1; unsure exactly what it means
    description->maxSimulLines = -1;

    // We don't have any info to fill out the other fields
    return TRUE;
}

static const int maxIOs = 64;

static void EnumerateInputs(media_node node, std::vector<media_input>* inputs,
        std::vector<media_multi_audio_format>* formats) {
    BMediaRoster* roster = BMediaRoster::Roster();

    media_input inputArray[maxIOs];
    int32 inputCount;
    roster->GetFreeInputsFor(node, inputArray, maxIOs, &inputCount);
    for (int i = 0; i < inputCount; i++) {
        media_multi_audio_format* format = (media_multi_audio_format*)
            &inputArray[i].format.u;
        if (inputs != NULL)
            inputs->push_back(inputArray[i]);
        if (formats != NULL)
            formats->push_back(*format);
    }
}

static void EnumerateOutputs(media_node node, std::vector<media_output>* outputs,
        std::vector<media_multi_audio_format>* formats) {
    BMediaRoster* roster = BMediaRoster::Roster();

    media_output outputArray[maxIOs];
    int32 outputCount;
    roster->GetFreeOutputsFor(node, outputArray, maxIOs, &outputCount);
    for (int i = 0; i < outputCount; i++) {
        media_multi_audio_format* format = (media_multi_audio_format*)
            &outputArray[i].format.u;
        if (outputs != NULL)
            outputs->push_back(outputArray[i]);
        if (formats != NULL)
            formats->push_back(*format);
    }
}

static int AudioFormatToBits(uint32 format) {
    switch (format) {
        case media_raw_audio_format::B_AUDIO_FLOAT:
            // TODO
        case media_raw_audio_format::B_AUDIO_INT:
            return 32;
        case media_raw_audio_format::B_AUDIO_SHORT:
            return 16;
        case media_raw_audio_format::B_AUDIO_UCHAR:
        case media_raw_audio_format::B_AUDIO_CHAR:
            return 8;
        default:
            return 0;
   }
}

void DAUDIO_GetFormats(INT32 mixerIndex, INT32 deviceID, int isSource, void* creator) {
    live_node_info info;
    
    if (cache.GetDevice(mixerIndex, &info) != B_OK)
        return;

    std::vector<media_multi_audio_format> formats;

    if (isSource == TRUE) {
        // We're looking for input formats, so we want to get the
        // node's outputs
        EnumerateOutputs(info.node, NULL, &formats);
      } else {
        // We're looking for output formats, so we want to get the
        // node's inputs
        EnumerateInputs(info.node, NULL, &formats);
    }

    for (size_t i = 0; i < formats.size(); i++) {
        media_multi_audio_format format = formats[i];

        int bitCount = AudioFormatToBits(format.format);

        DAUDIO_AddAudioFormat(creator,
            bitCount, // bits per sample
            -1, // auto frame size
            format.channel_count, // channel count
            format.frame_rate, // sample rate
            DAUDIO_PCM, // pcm encoding
            format.format == media_raw_audio_format::B_AUDIO_UCHAR // is signed
                ? FALSE : TRUE,
            format.byte_order == B_MEDIA_BIG_ENDIAN // is big endian
                ? TRUE : FALSE);
    }
}


typedef struct {
    BSoundPlayer* sound_player;
    // TODO BBufferConsumer* sound_recorder
    RingBuffer buffer;
} HaikuPCMInfo;

static void PlayBuffer(void* cookie, void* buffer, size_t size,
        const media_raw_audio_format& format) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)cookie;

    // assume that the format is the one we requested for now

    // try to read size bytes from the buffer
    size_t read = info->buffer.Read(buffer, size);
    if (read < size) {
        // buffer underrun
        // ...
    }
}

void* DAUDIO_Open(INT32 mixerIndex, INT32 deviceID, int isSource,
                  int encoding, float sampleRate, int sampleSizeInBits,
                  int frameSize, int channels,
                  int isSigned, int isBigEndian, int bufferSizeInBytes) {

    live_node_info info;
    if (cache.GetDevice(mixerIndex, &info) != B_OK)
        return NULL;

    std::vector<media_input> inputs;
    std::vector<media_output> outputs;
    std::vector<media_multi_audio_format> formats;

    static const int maxIOs = 64;
    if (isSource == TRUE) {
        // We're looking for input formats, so we want to get the
        // node's outputs
        EnumerateOutputs(info.node, &outputs, &formats);
    } else {
        // We're looking for output formats, so we want to get the
        // node's inputs
        EnumerateInputs(info.node, &inputs, &formats);
    }

    // find the matching media_input/media_output
    int foundIndex = -1;
    for (size_t i = 0; i < formats.size(); i++) {
    	media_multi_audio_format format = formats[i];
        int bits = AudioFormatToBits(format.format);
        if (format.frame_rate == sampleRate && bits == sampleSizeInBits
                && (int)format.channel_count == channels
                && (format.byte_order == B_MEDIA_BIG_ENDIAN) == (isSigned == TRUE)
                && (format.format == media_raw_audio_format::B_AUDIO_UCHAR)
                    == (isSigned == TRUE)) {
            foundIndex = i;
            break;
        }
    }

    if (foundIndex == -1) {
        ERROR0("DAUDIO_Open: ERROR: format doesn't match format of any input/output!\n");
        return NULL;
    }

    if (isSource == TRUE) {
        // TODO
    } else {
    	HaikuPCMInfo* info = new HaikuPCMInfo();
        // We're outputting audio so we need a media_input
        media_input input = inputs[foundIndex];
        BSoundPlayer* player = new BSoundPlayer(input.node, &formats[foundIndex],
            "jsoundSoundPlayer", &input, PlayBuffer, NULL, info);
        if (player->InitCheck() != B_OK) {
        	delete info;
        	delete player;
        	return NULL;
        }

        info->sound_player = player;
        return (void*)info;
    }

    return NULL;
}


int DAUDIO_Start(void* id, int isSource) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;
    status_t result;

    if (isSource == TRUE) {
        // TODO
    } else {
        result = info->sound_player->Start();
    }

    return result == B_OK ? TRUE : FALSE;
}

int DAUDIO_Stop(void* id, int isSource) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;

    if (isSource == TRUE) {
        // TODO
    } else {
        info->sound_player->Stop();
    }

    return TRUE;
}

void DAUDIO_Close(void* id, int isSource) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;

    if (isSource == TRUE) {
        // TODO
    } else {
        delete info->sound_player;
        delete info;
    }
}

int DAUDIO_Write(void* id, char* data, int byteSize) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;
    TRACE1(">>DAUDIO_Write: %d bytes to write\n", byteSize);

    int result = info->buffer.Write(data, byteSize, true);

    TRACE1("<<DAUDIO_Write: %d bytes written\n", result);
    return result;
}

int DAUDIO_Read(void* id, char* data, int byteSize) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;
    TRACE1(">>DAUDIO_Read: %d bytes to read\n", byteSize);

    int result = info->buffer.Read(data, byteSize);

    TRACE1("<<DAUDIO_Read: %d bytes has been read\n", result);
    return result;
}

int DAUDIO_GetBufferSize(void* id, int isSource) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;

    int bufferSizeInBytes = info->buffer.GetBufferSize();

    TRACE1("DAUDIO_GetBufferSize returns %d\n", bufferSizeInBytes);
    return bufferSizeInBytes;
}

int DAUDIO_StillDraining(void* id, int isSource) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;

    int draining = info->buffer.GetValidByteCount() > 0 ? TRUE : FALSE;

    TRACE1("DAUDIO_StillDraining returns %d\n", draining);
    return draining;
}

int DAUDIO_Flush(void* id, int isSource) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;
    TRACE0("DAUDIO_Flush\n");

    info->buffer.Flush();

    return TRUE;
}

int DAUDIO_GetAvailable(void* id, int isSource) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;

    int bytesInBuffer = info->buffer.GetValidByteCount();
    if (isSource) {
        return info->buffer.GetBufferSize() - bytesInBuffer;
    } else {
        return bytesInBuffer;
    }
}

INT64 DAUDIO_GetBytePosition(void* id, int isSource, INT64 javaBytePos) {
    HaikuPCMInfo* info = (HaikuPCMInfo*)id;
    INT64 position;

    if (isSource) {
        position = javaBytePos - info->buffer.GetValidByteCount();
    } else {
        position = javaBytePos + info->buffer.GetValidByteCount();
    }

    TRACE2("DAUDIO_GetBytePosition returns %lld (javaBytePos = %lld)\n", (long long)position, (long long)javaBytePos);
    return position;
}

void DAUDIO_SetBytePosition(void* id, int isSource, INT64 javaBytePos) {
}

int DAUDIO_RequiresServicing(void* id, int isSource) {
    return FALSE;
}

void DAUDIO_Service(void* id, int isSource) {
    // unused
}

}

#endif // USE_DAUDIO
