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

package org.pragmatica.io.async.uring;

import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.uring.struct.raw.CompletionQueueEntry;
import org.pragmatica.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pragmatica.io.async.util.raw.RawMemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UringNativeTest {
    private static final int QUEUE_SIZE = 128;

    interface LongBiConsumer {
        void accept(long ringBase, long completionBase);
    }

    private static void runWithUring(LongBiConsumer action) {
        var ringBase = RawMemory.allocate(UringNative.SIZE);
        var completionBase = RawMemory.allocate(QUEUE_SIZE * 2 * 8); // queue size * 2 (CQ size == 2 x SQ size) * 8 (bytes per ptr)
        assertNotEquals(0, ringBase);
        assertNotEquals(0, completionBase);

        var rc = UringNative.init(QUEUE_SIZE, ringBase, 0);

        try {
            assertEquals(0, rc);

            action.accept(ringBase, completionBase);

        } finally {
            UringNative.close(ringBase);
            RawMemory.dispose(ringBase);
            RawMemory.dispose(completionBase);
        }
    }

    @Test
    void nopCanBeSubmittedAndConfirmed() {
        runWithUring((ringBase, completionBase) -> {
            var sq = UringNative.nextSQEntry(ringBase);
            assertNotEquals(0, sq);

            var entry = SubmitQueueEntry.at(sq);

            entry.clear()
                 .opcode(AsyncOperation.IORING_OP_NOP.opcode())
                 .userData(0x0CAFEBABEL)
                 .fd(-1);

            var completionCount = UringNative.submitAndWait(ringBase, 1);
            assertEquals(1, completionCount);

            var readyCompletions = UringNative.peekCQ(ringBase, completionBase, QUEUE_SIZE);
            assertEquals(1, readyCompletions);

            var cq = CompletionQueueEntry.at(RawMemory.getLong(completionBase));
            assertEquals(0x0CAFEBABEL, cq.userData());

            UringNative.advanceCQ(ringBase, 1);
        });
    }

    @Test
    void nopCanBeSubmittedAndConfirmedWithCompletionProcessor() {
        runWithUring((ringBase, completionBase) -> {
            var sq = UringNative.nextSQEntry(ringBase);
            assertNotEquals(0, sq);

            var entry = SubmitQueueEntry.at(sq);

            entry.clear()
                 .opcode(AsyncOperation.IORING_OP_NOP.opcode())
                 .userData(0x0CAFEBABEL)
                 .fd(-1);

            var completionCount = UringNative.submitAndWait(ringBase, 1);
            assertEquals(1, completionCount);

            try (var processor = CompletionProcessor.create(QUEUE_SIZE)) {
                processor.process(ringBase, (cq) -> assertEquals(0x0CAFEBABEL, cq.userData()));
            }
        });
    }
}
