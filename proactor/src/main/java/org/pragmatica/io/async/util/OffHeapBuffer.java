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

package org.pragmatica.io.async.util;

import org.pragmatica.io.async.uring.struct.offheap.AbstractOffHeapStructure;
import org.pragmatica.io.async.util.raw.RawMemory;

import java.util.HexFormat;

/**
 * Memory buffer allocated outside Java heap.
 */
class OffHeapBuffer extends AbstractOffHeapStructure<OffHeapSlice> implements OffHeapSlice {
    private int used;

    private OffHeapBuffer(byte[] input) {
        super(input.length);
        RawMemory.putByteArray(address(), input);
        used = input.length;
    }

    private OffHeapBuffer(int size) {
        super(size);
        used = 0;
    }

    static OffHeapBuffer fromBytes(byte[] input) {
        return new OffHeapBuffer(input);
    }

    static OffHeapBuffer fixedSize(int size) {
        return new OffHeapBuffer(size);
    }

    @Override
    public OffHeapSlice slice(int offset, int length) {
        assert offset < size() && (offset + length) <= size();

        return new OffHeapSliceImpl(address() + offset, length);
    }

    @Override
    public int used() {
        return used;
    }

    @Override
    public OffHeapBuffer used(int used) {
        this.used = Math.min(size(), used);
        return this;
    }

    @Override
    public byte[] export() {
        return RawMemory.getByteArray(address(), used);
    }

    @Override
    public String hexDump() {
        return HexFormat.of().withUpperCase().formatHex(export());
    }

    @Override
    public String toString() {
        return "OffHeapBuffer(size = " + size() + ", used = " + used() + ')';
    }
}
