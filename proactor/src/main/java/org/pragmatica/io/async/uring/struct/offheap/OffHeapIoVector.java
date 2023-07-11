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

package org.pragmatica.io.async.uring.struct.offheap;

import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.uring.struct.raw.IoVector;
import org.pragmatica.io.async.uring.struct.shape.IoVectorOffsets;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Option;

import java.util.function.BiConsumer;

/**
 * Representation of I/O vector for vector-based read/write operations (see {@link org.pragmatica.io.async.Proactor#readVector(BiConsumer, FileDescriptor, OffsetT, Option, OffHeapSlice...)} and
 * {@link org.pragmatica.io.async.Proactor#writeVector(BiConsumer, FileDescriptor, OffsetT, Option, OffHeapSlice...)} methods).
 */
public class OffHeapIoVector extends AbstractOffHeapStructure<OffHeapIoVector> {
    private final IoVector shape;
    private final int count;

    private enum Mode {
        READ, WRITE
    }

    private OffHeapIoVector(int count) {
        super(count * IoVectorOffsets.SIZE);
        this.count = count;
        clear();
        shape = IoVector.at(address());
    }

    private void addBuffer(Mode mode, OffHeapSlice buffer) {
        shape.base(buffer.address())
             .len(mode == Mode.READ ? buffer.size() : buffer.used())
             .reposition(shape.address() + IoVectorOffsets.SIZE);
    }

    private void resetShape() {
        shape.reposition(address());
    }

    public static OffHeapIoVector withReadBuffers(OffHeapSlice... buffers) {
        return withBuffers(Mode.READ, buffers);
    }

    public static OffHeapIoVector withWriteBuffers(OffHeapSlice... buffers) {
        return withBuffers(Mode.WRITE, buffers);
    }

    private static OffHeapIoVector withBuffers(Mode mode, OffHeapSlice[] buffers) {
        var vector = new OffHeapIoVector(buffers.length);

        for (var buffer : buffers) {
            vector.addBuffer(mode, buffer);
        }
        vector.resetShape();
        return vector;
    }

    public int length() {
        return count;
    }
}
