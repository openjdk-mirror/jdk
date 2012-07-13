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

#include "DirectAudio.h"

#if USE_DAUDIO == TRUE

DeviceCache cache;

extern "C" {

INT32 DAUDIO_GetDirectAudioDeviceCount() {
	return cache.DeviceCount();
}

INT32 DAUDIO_GetDirectAudioDeviceDescription(INT32 mixerIndex,
                                             DirectAudioDeviceDescription* description) {
	live_node_info node;
	
	if (cache.GetDevice(mixerIndex, &node) != B_OK)
		return FALSE;

	strlcpy(description->name, cache.name, DAUDIO_STRING_LENGTH);
	
	// Mac OS X port sets this to -1 unsure exactly what it means
	description->maxSimulLines = -1;

	// We don't have any info to fill out the other fields
	
	return TRUE;
}

void DAUDIO_GetFormats(INT32 mixerIndex, INT32 deviceID, int isSource, void* creator) {
    live_node_info node;
    
    if (cache.GetDevice(mixerIndex, &node) != B_OK)
    	return;

	vector<media_multi_audio_format> audioFormats;

	static const int maxIOs = 64;
  	if (isSource == TRUE) {
  		// We're looking for input formats, so we want to get the
  		// node's outputs
		media_output outputs[maxIOs];
		int32 outputCount;
		roster->GetAllOutputsFor(node.node, outputs, maxIOs, &outputCount);
		for (int i = 0; i < outputCount; i++) {
			media_multi_audio_format* format = (media_multi_audio_format*)
				&outputs[i].format.u;
			audioFormats.push_back(*format);
		}
  	} else {
  		// We're looking for output formats, so we want to get the
  		// node's inputs
		media_input inputs[maxIOs];
		int32 intputCount;
		roster->GetAllOutputsFor(node.node, inputs, maxIOs, &inputCount);
		for (int i = 0; i < inputCount; i++) {
			media_multi_audio_format* format = (media_multi_audio_format*)
				&inputs[i].format.u;
			audioFormats.push_back(*format);
		}
  	}

	for (int i = 0; i < audioFormats.size(); i++) {
		media_multi_audio_format format = audioFormats.at(i);

		bool isSigned = true;
		int bitCount;
		switch (format.format) {
			case B_AUDIO_FLOAT:
				// ehhm?
			case B_AUDIO_INT:
				bitCount = 32;
				break;
			case B_AUDIO_SHORT:
				bitCount = 16;
				break;
			case B_AUDIO_UCHAR:
				isSigned = false;
			case B_AUDIO_CHAR:
				bitCount = 8;
				break;
		}

		DAUDIO_AddAudioFormat(creator,
			bitCount, // bits per sample
			-1, // auto frame size
			format.channel_count, // channel count
			format.frame_rate, // sample rate
			DAUDIO_PCM, // pcm encoding
			isSigned, // is signed
			format.byte_order == B_MEDIA_BIG_ENDIAN // is big endian
				? TRUE : FALSE);
	}
}


typedef struct {
    // return this from DAUDIO_Open to keep track of things
} Cookie;


void* DAUDIO_Open(INT32 mixerIndex, INT32 deviceID, int isSource,
                  int encoding, float sampleRate, int sampleSizeInBits,
                  int frameSize, int channels,
                  int isSigned, int isBigEndian, int bufferSizeInBytes) {
    // open device
    // return (void*)Cookie* whatever
}


int DAUDIO_Start(void* id, int isSource) {
    // Cookie* cookie = (Cookie*)id
    // start playback
}

int DAUDIO_Stop(void* id, int isSource) {
    // Cookie* cookie = (Cookie*)id
    // stop playback
}

void DAUDIO_Close(void* id, int isSource) {
}

int DAUDIO_Write(void* id, char* data, int byteSize) {
}

int DAUDIO_Read(void* id, char* data, int byteSize) {
}

int DAUDIO_GetBufferSize(void* id, int isSource) {
}

int DAUDIO_StillDraining(void* id, int isSource) {
}

int DAUDIO_Flush(void* id, int isSource) {
}

int DAUDIO_GetAvailable(void* id, int isSource) {
}

INT64 DAUDIO_GetBytePosition(void* id, int isSource, INT64 javaBytePos) {
}


void DAUDIO_SetBytePosition(void* id, int isSource, INT64 javaBytePos) {
}

int DAUDIO_RequiresServicing(void* id, int isSource) {
}

void DAUDIO_Service(void* id, int isSource) {
}

}

#endif // USE_DAUDIO
