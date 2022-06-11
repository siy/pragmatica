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

package org.pragmatica.io.async.util;

import org.pragmatica.io.async.util.raw.RawMemory;

import java.util.HexFormat;

class OffHeapSliceImpl implements OffHeapSlice {
    private final long address;
    private final int size;
    private int used;

    OffHeapSliceImpl(long address, int size) {
        this.address = address;
        this.size = size;
        this.used = 0;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public OffHeapSlice clear() {
        RawMemory.clear(address, size);
        used = 0;
        return this;
    }

    @Override
    public int used() {
        return used;
    }

    @Override
    public OffHeapSlice used(int used) {
        this.used = Math.min(size(), used);
        return this;
    }

    @Override
    public OffHeapSlice slice(int offset, int length) {
        assert offset < size && (offset + length) <= size;

        return new OffHeapSliceImpl(address() + offset, length);
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
    public void close() {
        // Do nothing, slice does not hold any resources
    }
}
