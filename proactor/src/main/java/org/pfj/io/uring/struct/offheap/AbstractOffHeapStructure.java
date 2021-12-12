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

package org.pfj.io.uring.struct.offheap;

import org.pfj.io.raw.RawMemory;
import org.pfj.io.uring.struct.AbstractRawStructure;
import org.pfj.io.uring.struct.OffHeapStructure;

public abstract class AbstractOffHeapStructure<T extends AbstractOffHeapStructure<?>> extends AbstractRawStructure<T>
        implements OffHeapStructure<T>, AutoCloseable {
    private boolean released = false;

    protected AbstractOffHeapStructure(final int size) {
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
