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
 */

package org.pragmatica.io.async.uring.struct.offheap;

import org.pragmatica.io.async.uring.struct.AbstractRawStructure;
import org.pragmatica.io.async.uring.struct.OffHeapStructure;
import org.pragmatica.io.async.uring.struct.RawStructure;
import org.pragmatica.io.async.util.raw.RawMemory;

/**
 * Base class for the classes which are used for exchanging data with JNI side.
 */
public abstract class AbstractOffHeapStructure<T extends RawStructure<T>>
    extends AbstractRawStructure<T> implements OffHeapStructure<T>, AutoCloseable {
    private boolean released = false;

    protected AbstractOffHeapStructure(int size) {
        super(RawMemory.allocate(size), size);
    }

    @Override
    public void dispose() {
        if (released) {
            return;
        }

        RawMemory.dispose(address());
        released = true;
    }

    @Override
    public void close() {
        dispose();
    }
}
