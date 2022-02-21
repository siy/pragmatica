/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.io.async.util.Units._1KiB;
import static org.pragmatica.io.async.util.allocator.ChunkedAllocator.allocator;

class ChunkedAllocatorTest {
    @Test
    void allocationCanBeDoneSuccessfully() {
        try(var allocator = allocator(128 * _1KiB)) {
            assertEquals("........", allocator.allocationMap());

            var buf1 = allocator.allocate(1);
            assertEquals("U.......", allocator.allocationMap());

            buf1.onSuccess(FixedBuffer::dispose);
            assertEquals("........", allocator.allocationMap());

            var buf2 = allocator.allocate(1);
            assertEquals("U.......", allocator.allocationMap());

            var buf3 = allocator.allocate(ChunkedAllocator.CHUNK_SIZE + 1);
            assertEquals("UUU.....", allocator.allocationMap());

            buf2.onSuccess(FixedBuffer::dispose);
            assertEquals(".UU.....", allocator.allocationMap());

            var buf4 = allocator.allocate(ChunkedAllocator.CHUNK_SIZE + 1);
            assertEquals(".UUUU...", allocator.allocationMap());

            var buf5 = allocator.allocate(1);
            assertEquals("UUUUU...", allocator.allocationMap());

            buf3.onSuccess(FixedBuffer::dispose);
            assertEquals("U..UU...", allocator.allocationMap());
        }
    }

    @Test
    void allocationFailsIfNotEnoughSpace() {
        try(var allocator = allocator(128 * _1KiB)) {
            assertEquals("........", allocator.allocationMap());

            var buf1 = allocator.allocate(128 * _1KiB);
            assertEquals("UUUUUUUU", allocator.allocationMap());

            allocator.allocate(1)
                     .onSuccessDo(Assertions::fail);

            assertEquals("UUUUUUUU", allocator.allocationMap());
        }
    }
}