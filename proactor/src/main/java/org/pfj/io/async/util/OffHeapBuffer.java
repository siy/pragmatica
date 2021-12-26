/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.io.async.util;

import org.pfj.io.async.uring.struct.offheap.AbstractOffHeapStructure;
import org.pfj.io.async.util.raw.RawMemory;

public class OffHeapBuffer extends AbstractOffHeapStructure<OffHeapBuffer> {
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

    public static OffHeapBuffer fromBytes(byte[] input) {
        return new OffHeapBuffer(input);
    }

    public static OffHeapBuffer fixedSize(int size) {
        return new OffHeapBuffer(size);
    }

    public int used() {
        return used;
    }

    public OffHeapBuffer used(int used) {
        this.used = Math.min(size(), used);
        return this;
    }

    public byte[] export() {
        return RawMemory.getByteArray(address(), used);
    }
}
