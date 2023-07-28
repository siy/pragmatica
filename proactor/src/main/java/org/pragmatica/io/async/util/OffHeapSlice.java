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

import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.uring.struct.RawStructure;

/**
 * Memory buffer allocated outside Java heap.
 */
public interface OffHeapSlice extends RawStructure<OffHeapSlice> {
    static OffHeapSlice fromBytes(byte[] input) {
        return OffHeapBuffer.fromBytes(input);
    }

    static OffHeapSlice fixedSize(int size) {
        return OffHeapBuffer.fixedSize(size);
    }

    int used();

    OffHeapSlice used(int used);
    default OffHeapSlice used(SizeT usedSize) {
        return used((int) usedSize.value());
    }

    OffHeapSlice slice(int offset, int length);

    byte[] export();

    String hexDump();

    void close();
}
