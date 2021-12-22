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

package org.pfj.io.async.uring;

import org.junit.jupiter.api.Test;
import org.pfj.io.async.uring.struct.raw.CompletionQueueEntry;
import org.pfj.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.async.util.raw.RawMemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UringNativeTest {
    @Test
    void nopCanBeSubmittedAndConfirmed() {
        final long ringBase = RawMemory.allocate(256);

        assertNotEquals(0, ringBase);

        final long completionBase = RawMemory.allocate(16 * 1024); // 1024 * 2 (CQ size is twice of SQ size) * 8 (bytes per ptr)

        final int rc = UringNative.init(128, ringBase, 0);

        try {
            assertEquals(0, rc);

            final long sq = UringNative.nextSQEntry(ringBase);

            assertNotEquals(0, sq);

            final SubmitQueueEntry entry = SubmitQueueEntry.at(sq);

            entry.clear()
                 .opcode(AsyncOperation.IORING_OP_NOP.opcode())
                 .userData(0x0CAFEBABEL)
                 .fd(-1);

            final long completionCount = UringNative.submitAndWait(ringBase, 1);

            assertEquals(1, completionCount);

            final int readyCompletions = UringNative.peekCQ(ringBase, completionBase, 1024);

            assertEquals(1, readyCompletions);

            final CompletionQueueEntry cq = CompletionQueueEntry.at(RawMemory.getLong(completionBase));

            assertEquals(0x0CAFEBABEL, cq.userData());

            UringNative.advanceCQ(ringBase, 1);
        } finally {
            UringNative.close(ringBase);
            RawMemory.dispose(ringBase);
            RawMemory.dispose(completionBase);
        }
    }

    @Test
    void nopCanBeSubmittedAndConfirmedWithCompletionProcessor() {
        final long ringBase = RawMemory.allocate(256);

        assertNotEquals(0, ringBase);

        final int rc = UringNative.init(128, ringBase, 0);

        try {
            assertEquals(0, rc);

            final long sq = UringNative.nextSQEntry(ringBase);

            assertNotEquals(0, sq);

            final SubmitQueueEntry entry = SubmitQueueEntry.at(sq);

            entry.clear()
                 .opcode(AsyncOperation.IORING_OP_NOP.opcode())
                 .userData(0x0CAFEBABEL)
                 .fd(-1);

            final long completionCount = UringNative.submitAndWait(ringBase, 1);
            assertEquals(1, completionCount);

            try (final CompletionProcessor processor = CompletionProcessor.create(1024)) {
                processor.process(ringBase, (cq) -> assertEquals(0x0CAFEBABEL, cq.userData()));
            }
        } finally {
            UringNative.close(ringBase);
            RawMemory.dispose(ringBase);
        }
    }
}