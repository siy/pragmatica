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

package org.pfj.io.uring;

import org.pfj.io.uring.struct.raw.CompletionQueueEntry;
import org.pfj.io.raw.RawMemory;

import java.util.function.Consumer;

class CompletionProcessor implements AutoCloseable {
    private static final long ENTRY_SIZE = 8L;    // each entry is a 64-bit pointer
    private final CompletionQueueEntry entry;
    private final long completionBuffer;
    private final int size;
    private boolean closed = false;

    private CompletionProcessor(final int numEntries) {
        size = numEntries;
        completionBuffer = RawMemory.allocate(size * ENTRY_SIZE);
        entry = CompletionQueueEntry.at(0);
    }

    public static CompletionProcessor create(final int numEntries) {
        return new CompletionProcessor(numEntries);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        RawMemory.dispose(completionBuffer);
        closed = true;
    }

    public CompletionProcessor process(final long baseAddress, final Consumer<CompletionQueueEntry> consumer) {
        long ready = 0;

        try {
            ready = Uring.peekCQ(baseAddress, completionBuffer, size);
            for (long i = 0, address = completionBuffer; i < ready; i++, address += ENTRY_SIZE) {
                entry.reposition(RawMemory.getLong(address));
                consumer.accept(entry);
            }
        } finally {
            if (ready > 0) {
                Uring.advanceCQ(baseAddress, ready);
            }
        }
        return this;
    }
}
