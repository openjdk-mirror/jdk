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

#include "PLATFORM_API_HaikuOS_Utils.h"

AudioDeviceCache::AudioDeviceCache()
{
    //BMediaRoster* roster = BMediaRoster::Roster();
    //roster->StartWatching(BMessenger(this));

    _Refresh();
}

int
AudioDeviceCache::DeviceCount()
{
    int count = -1;
    if (lock.Lock()) {
        count = nodes.size();
        lock.Unlock();
    }

    return count;
}

status_t
AudioDeviceCache::GetDevice(int index, live_node_info* _nodeInfo)
{
    if (!lock.Lock())
        return B_ERROR;

    if (index >= 0 && (size_t)index < nodes.size()) {
        *_nodeInfo = nodes[index];

        lock.Unlock();
        return B_OK;
    } else {
        lock.Unlock();
        return B_ERROR;
    }
}

void
AudioDeviceCache::MessageReceived(BMessage* msg)
{
    _Refresh();
}

void
AudioDeviceCache::_Refresh()
{
    nodes.clear();
    BMediaRoster* roster = BMediaRoster::Roster();

    /*
     * Picking a specific audio device to output on seems to be a no-go
     * because the node doesn't have any free inputs (because it's used
     * by the mixer?)
     */
#if 0
    static const int maxCount = 128;

    live_node_info liveNodes[maxCount];
    int32 count = maxCount;

    media_format nodeFormat;
    nodeFormat.type = B_MEDIA_RAW_AUDIO;

    roster->GetLiveNodes(liveNodes, &count, &nodeFormat, NULL, NULL, B_PHYSICAL_OUTPUT);

    for (int i = 0; i < count; i++) {
        nodes.push_back(liveNodes[i]);
    }

    count = maxCount;
    roster->GetLiveNodes(liveNodes, &count, NULL, &nodeFormat, NULL, B_PHYSICAL_INPUT);

    for (int i = 0; i < count; i++) {
        nodes.push_back(liveNodes[i]);
    }
#endif

    live_node_info info;
    media_node audioMixer;
    if (roster->GetAudioMixer(&audioMixer) == B_OK) {
        info.node = audioMixer;
        const char mixerName[] = "System Mixer";
        strncpy(info.name, mixerName, sizeof(mixerName));
        nodes.push_back(info);
    }
}

MidiDeviceCache::MidiDeviceCache()
{
    //BMediaRoster* roster = BMediaRoster::Roster();
    //roster->StartWatching(BMessenger(this));

    _Refresh();
}

int
MidiDeviceCache::ConsumerCount()
{
    int count = -1;
    if (lock.Lock()) {
        count = consumers.size();
        lock.Unlock();
    }

    return count;
}

int
MidiDeviceCache::ProducerCount()
{
    int count = -1;
    if (lock.Lock()) {
        count = producers.size();
        lock.Unlock();
    }

    return count;
}

status_t
MidiDeviceCache::GetConsumer(int index, BMidiConsumer** _consumer)
{
    if (!lock.Lock())
        return B_ERROR;

    if (index >= 0 && (size_t)index < consumers.size()) {
        *_consumer = consumers[index];

        lock.Unlock();
        return B_OK;
    } else {
        lock.Unlock();
        return B_ERROR;
    }
}

status_t
MidiDeviceCache::GetProducer(int index, BMidiProducer** _producer)
{
    if (!lock.Lock())
        return B_ERROR;

    if (index >= 0 && (size_t)index < producers.size()) {
        *_producer = producers[index];

        lock.Unlock();
        return B_OK;
    } else {
        lock.Unlock();
        return B_ERROR;
    }
}

void
MidiDeviceCache::MessageReceived(BMessage* msg)
{
    _Refresh();
}

void
MidiDeviceCache::_Refresh()
{
    consumers.clear();
    producers.clear();

    BMidiRoster* roster = BMidiRoster::MidiRoster();

    int32 id = 0;

    BMidiConsumer* consumer;
    while ((consumer = roster->NextConsumer(&id)) != NULL) {
        consumers.push_back(consumer);
    }

    id = 0;
    BMidiProducer* producer;
    while ((producer = roster->NextProducer(&id)) != NULL) {
        producers.push_back(producer);
    }
}

extern "C" {

void* MIDI_CreateLock() {
    BLocker* locker = new BLocker();
    return (void*)locker;
}

void MIDI_DestroyLock(void* lock) {
    if (lock != NULL) {
        BLocker* locker = (BLocker*)lock;
        delete locker;
    }
}

void MIDI_Lock(void* lock) {
    if (lock != NULL) {
        BLocker* locker = (BLocker*)lock;
        locker->Lock();
    }
}

void MIDI_Unlock(void* lock) {
    if (lock) {
        BLocker* locker = (BLocker*)lock;
        locker->Unlock();
    }
}

}
