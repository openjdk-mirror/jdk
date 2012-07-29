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
#ifndef PLATFORM_API_HAIKUOS_UTILS_H_INCLUDED
#define PLATFORM_API_HAIKUOS_UTILS_H_INCLUDED

extern "C" {
#include <DirectAudio.h>
}

#include <Handler.h>
#include <Locker.h>
#include <MediaRoster.h>
#include <MidiRoster.h>

#include <stdlib.h>

#include <vector>

class AudioDeviceCache : public BHandler {
public:
    AudioDeviceCache();

    int DeviceCount();
    status_t GetDevice(int index, live_node_info* info);

    void MessageReceived(BMessage* msg);

private:
    void _Refresh();

    std::vector<live_node_info> nodes;
    BLocker lock;
};

class MidiDeviceCache : public BHandler {
public:
    MidiDeviceCache();

    int ConsumerCount();
    int ProducerCount();

    status_t GetConsumer(int index, BMidiConsumer** consumer);
    status_t GetProducer(int index, BMidiProducer** consumer);

    void MessageReceived(BMessage* msg);

private:
    void _Refresh();

    std::vector<BMidiConsumer*> consumers;
    std::vector<BMidiProducer*> producers;
    BLocker lock;
};

class RingBuffer {
public:
    RingBuffer() : pBuffer(NULL), nBufferSize(0) {
        pthread_mutex_init(&lockMutex, NULL);
    }
    ~RingBuffer() {
        Deallocate();
        pthread_mutex_destroy(&lockMutex);
    }

    // extraBytes: number of additionally allocated bytes to prevent data
    // overlapping when almost whole buffer is filled
    // (required only if Write() can override the buffer)
    bool Allocate(int requestedBufferSize, int extraBytes) {
        int fullBufferSize = requestedBufferSize + extraBytes;
        int powerOfTwo = 1;
        while (powerOfTwo < fullBufferSize) {
            powerOfTwo <<= 1;
        }
        pBuffer = (UBYTE*)malloc(powerOfTwo);
        if (pBuffer == NULL) {
            ERROR0("RingBuffer::Allocate: OUT OF MEMORY\n");
            return false;
        }

        nBufferSize = requestedBufferSize;
        nAllocatedBytes = powerOfTwo;
        nPosMask = powerOfTwo - 1;
        nWritePos = 0;
        nReadPos = 0;
        nFlushPos = -1;

        TRACE2("RingBuffer::Allocate: OK, bufferSize=%d, allocated:%d\n", nBufferSize, nAllocatedBytes);
        return true;
    }

    void Deallocate() {
        if (pBuffer) {
            free(pBuffer);
            pBuffer = NULL;
            nBufferSize = 0;
        }
    }

    inline int GetBufferSize() {
        return nBufferSize;
    }

    inline int GetAllocatedSize() {
        return nAllocatedBytes;
    }

    // gets number of bytes available for reading
    int GetValidByteCount() {
        lock();
        INT64 result = nWritePos - (nFlushPos >= 0 ? nFlushPos : nReadPos);
        unlock();
        return result > (INT64)nBufferSize ? nBufferSize : (int)result;
    }

    int Write(void *srcBuffer, int len, bool preventOverflow) {
        lock();
        TRACE2("RingBuffer::Write (%d bytes, preventOverflow=%d)\n", len, preventOverflow ? 1 : 0);
        TRACE2("  writePos = %lld (%d)", (long long)nWritePos, Pos2Offset(nWritePos));
        TRACE2("  readPos=%lld (%d)", (long long)nReadPos, Pos2Offset(nReadPos));
        TRACE2("  flushPos=%lld (%d)\n", (long long)nFlushPos, Pos2Offset(nFlushPos));

        INT64 writePos = nWritePos;
        if (preventOverflow) {
            INT64 avail_read = writePos - (nFlushPos >= 0 ? nFlushPos : nReadPos);
            if (avail_read >= (INT64)nBufferSize) {
                // no space
                TRACE0("  preventOverlow: OVERFLOW => len = 0;\n");
                len = 0;
            } else {
                int avail_write = nBufferSize - (int)avail_read;
                if (len > avail_write) {
                    TRACE2("  preventOverlow: desrease len: %d => %d\n", len, avail_write);
                    len = avail_write;
                }
            }
        }
        unlock();

        if (len > 0) {

            write((UBYTE *)srcBuffer, Pos2Offset(writePos), len);

            lock();
            TRACE4("--RingBuffer::Write writePos: %lld (%d) => %lld, (%d)\n",
                (long long)nWritePos, Pos2Offset(nWritePos), (long long)nWritePos + len, Pos2Offset(nWritePos + len));
            nWritePos += len;
            unlock();
        }
        return len;
    }

    int Read(void *dstBuffer, int len) {
        lock();
        TRACE1("RingBuffer::Read (%d bytes)\n", len);
        TRACE2("  writePos = %lld (%d)", (long long)nWritePos, Pos2Offset(nWritePos));
        TRACE2("  readPos=%lld (%d)", (long long)nReadPos, Pos2Offset(nReadPos));
        TRACE2("  flushPos=%lld (%d)\n", (long long)nFlushPos, Pos2Offset(nFlushPos));

        applyFlush();
        INT64 avail_read = nWritePos - nReadPos;
        // check for overflow
        if (avail_read > (INT64)nBufferSize) {
            nReadPos = nWritePos - nBufferSize;
            avail_read = nBufferSize;
            TRACE0("  OVERFLOW\n");
        }
        INT64 readPos = nReadPos;
        unlock();

        if (len > (int)avail_read) {
            TRACE2("  RingBuffer::Read - don't have enough data, len: %d => %d\n", len, (int)avail_read);
            len = (int)avail_read;
        }

        if (len > 0) {

            read((UBYTE *)dstBuffer, Pos2Offset(readPos), len);

            lock();
            if (applyFlush()) {
                // just got flush(), results became obsolete
                TRACE0("--RingBuffer::Read, got Flush, return 0\n");
                len = 0;
            } else {
                TRACE4("--RingBuffer::Read readPos: %lld (%d) => %lld (%d)\n",
                    (long long)nReadPos, Pos2Offset(nReadPos), (long long)nReadPos + len, Pos2Offset(nReadPos + len));
                nReadPos += len;
            }
            unlock();
        } else {
            // underrun!
        }
        return len;
    }

    // returns number of the flushed bytes
    int Flush() {
        lock();
        INT64 flushedBytes = nWritePos - (nFlushPos >= 0 ? nFlushPos : nReadPos);
        nFlushPos = nWritePos;
        unlock();
        return flushedBytes > (INT64)nBufferSize ? nBufferSize : (int)flushedBytes;
    }

private:
    UBYTE *pBuffer;
    int nBufferSize;
    int nAllocatedBytes;
    INT64 nPosMask;

    pthread_mutex_t lockMutex;

    volatile INT64 nWritePos;
    volatile INT64 nReadPos;
    // Flush() sets nFlushPos value to nWritePos;
    // next Read() sets nReadPos to nFlushPos and resests nFlushPos to -1
    volatile INT64 nFlushPos;

    inline void lock() {
        pthread_mutex_lock(&lockMutex);
    }
    inline void unlock() {
        pthread_mutex_unlock(&lockMutex);
    }

    inline bool applyFlush() {
        if (nFlushPos >= 0) {
            nReadPos = nFlushPos;
            nFlushPos = -1;
            return true;
        }
        return false;
    }

    inline int Pos2Offset(INT64 pos) {
        return (int)(pos & nPosMask);
    }

    void write(UBYTE *srcBuffer, int dstOffset, int len) {
        int dstEndOffset = dstOffset + len;

        int lenAfterWrap = dstEndOffset - nAllocatedBytes;
        if (lenAfterWrap > 0) {
            // dest.buffer does wrap
            len = nAllocatedBytes - dstOffset;
            memcpy(pBuffer+dstOffset, srcBuffer, len);
            memcpy(pBuffer, srcBuffer+len, lenAfterWrap);
        } else {
            // dest.buffer does not wrap
            memcpy(pBuffer+dstOffset, srcBuffer, len);
        }
    }

    void read(UBYTE *dstBuffer, int srcOffset, int len) {
        int srcEndOffset = srcOffset + len;

        int lenAfterWrap = srcEndOffset - nAllocatedBytes;
        if (lenAfterWrap > 0) {
            // need to unwrap data
            len = nAllocatedBytes - srcOffset;
            memcpy(dstBuffer, pBuffer+srcOffset, len);
            memcpy(dstBuffer+len, pBuffer, lenAfterWrap);
        } else {
            // source buffer is not wrapped
            memcpy(dstBuffer, pBuffer+srcOffset, len);
        }
    }
};

#endif // PLATFORM_API_HAIKUOS_UTILS_H_INCLUDED
