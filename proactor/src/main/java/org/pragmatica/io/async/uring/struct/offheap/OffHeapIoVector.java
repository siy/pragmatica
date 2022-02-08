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

package org.pragmatica.io.async.uring.struct.offheap;

import org.pragmatica.io.async.uring.struct.raw.IoVector;
import org.pragmatica.io.async.uring.struct.shape.IoVectorOffsets;
import org.pragmatica.io.async.util.OffHeapBuffer;

public class OffHeapIoVector extends AbstractOffHeapStructure<OffHeapIoVector> {
    private final IoVector shape;
    private final int count;

    private OffHeapIoVector(int count) {
        super(count * IoVectorOffsets.SIZE);
        this.count = count;
        clear();
        shape = IoVector.at(address());
    }

    private void addBuffer(OffHeapBuffer buffer) {
        shape.base(buffer.address())
             .len(buffer.used())
             .reposition(shape.address() + IoVectorOffsets.SIZE);
    }

    private void resetShape() {
        shape.reposition(address());
    }

    public static OffHeapIoVector withBuffers(OffHeapBuffer... buffers) {
        var vector = new OffHeapIoVector(buffers.length);
        for (var buffer : buffers) {
            vector.addBuffer(buffer);
        }
        vector.resetShape();
        return vector;
    }

    public int length() {
        return count;
    }
}
