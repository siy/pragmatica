/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.io.async.util.allocator;

import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.uring.UringApi;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Result;

import java.util.BitSet;
import java.util.HexFormat;

import static org.pragmatica.io.async.util.Units._1KiB;

/**
 * Simple allocator which allocates provided memory arena as chunks of fixed size.
 * <p>
 * Used algorithm is inherently susceptible to fragmentation. To prevent fragmentation, two strategies could be employed.
 * One is to always allocate same amounts of memory (usually aligned to default chunk size). Another is to allocate memory
 * once at startup.
 * <p>
 * Default chunk size is 16K, which should be good enough for most I/O buffers.
 */
public class ChunkedAllocator {
    public static final int CHUNK_SIZE = 16 * _1KiB;

    private final OffHeapSlice arena;
    private final BitSet chunkMap;
    private final int totalChunks;

    private ChunkedAllocator(OffHeapSlice arena) {
        this.arena = arena;
        this.totalChunks = arena.size() / CHUNK_SIZE;
        this.chunkMap = new BitSet(totalChunks);
    }

    public static ChunkedAllocator allocator(int size) {
        return allocator(OffHeapSlice.fixedSize(size));
    }

    public static ChunkedAllocator allocator(OffHeapSlice arena) {
        return new ChunkedAllocator(arena);
    }

    public ChunkedAllocator register(UringApi api) {
        api.registerBuffers(arena).onFailure(failure -> {
            throw new IllegalStateException("FixedBuffer registration error: " + failure.message() + " " + this);
        });
        return this;
    }

    public void close() {
        arena.close();
    }

    public Result<FixedBuffer> allocate(int size) {
        var numChunks = calculateNumChunks(size);

        synchronized (chunkMap) {
            var start = 0;

            while (start < totalChunks) {
                var from = chunkMap.nextClearBit(start);

                if (from == totalChunks) {
                    // no space left
                    return SystemError.ENOMEM.result();
                }

                int freeChunks = checkFreeChunks(from, numChunks);

                if (freeChunks == numChunks) {
                    chunkMap.set(from, from + numChunks);
                    // found segment of necessary size
                    return Result.success(new FixedBuffer(arena.slice(from * CHUNK_SIZE, size), this));
                }

                start += from + freeChunks + 1;
            }

            return SystemError.ENOMEM.result();
        }
    }

    public void dispose(FixedBuffer buffer) {
        var numChunks = calculateNumChunks(buffer.size());
        var from = (int) ((buffer.address() - arena.address()) / CHUNK_SIZE);

        synchronized (chunkMap) {
            // BitSet.clear() does most of the necessary checks
            chunkMap.clear(from, from + numChunks);
        }
    }

    public String allocationMap() {
        var builder = new StringBuilder();

        synchronized (chunkMap) {
            var counter = 0;

            for (int i = 0; i < totalChunks; i++) {
                builder.append(chunkMap.get(i) ? 'U':'.');

                if (++counter == 64) {
                    builder.append('\n');
                    counter = 0;
                }
            }
        }

        return builder.toString();
    }

    private static int calculateNumChunks(int size) {
        return (size + CHUNK_SIZE - 1) / CHUNK_SIZE;
    }

    private int checkFreeChunks(int from, int numChunks) {
        for (int i = 1; i < numChunks; i++) {
            int index = from + i;
            if (index >= totalChunks || chunkMap.get(index)) {
                return i;
            }
        }
        return numChunks;
    }

    @Override
    public String toString() {
        return "ChunkedAllocator(" + HexFormat.of().toHexDigits(arena.address()) + ")";
    }
}
