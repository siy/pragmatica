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

import org.pragmatica.io.async.util.OffHeapSlice;

/**
 * Fixed {@code IO_URING} buffer representation.
 */
public class FixedBuffer implements OffHeapSlice {
    private final OffHeapSlice slice;
    private final ChunkedAllocator owner;

    FixedBuffer(OffHeapSlice slice, ChunkedAllocator owner) {
        this.slice = slice;
        this.owner = owner;
    }

    public void dispose() {
        owner.dispose(this);
    }

    @Override
    public long address() {
        return slice.address();
    }

    @Override
    public int size() {
        return slice.size();
    }

    @Override
    public FixedBuffer clear() {
        slice.clear();
        return this;
    }

    @Override
    public int used() {
        return slice.used();
    }

    @Override
    public FixedBuffer used(int used) {
        slice.used(used);
        return this;
    }

    @Override
    public OffHeapSlice slice(int offset, int length) {
        return slice.slice(offset, length);
    }

    @Override
    public byte[] export() {
        return slice.export();
    }

    @Override
    public String hexDump() {
        return slice.hexDump();
    }

    @Override
    public void close() {
        slice.close();
        dispose();
    }
}
