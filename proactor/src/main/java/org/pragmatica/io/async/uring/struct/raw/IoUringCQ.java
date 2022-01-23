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

package org.pragmatica.io.async.uring.struct.raw;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.UringEnterFlags;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.IoUringCQOffsets;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.util.raw.RawMemory;

import static org.pragmatica.io.async.uring.struct.shape.IoUringCQOffsets.*;

public class IoUringCQ extends AbstractExternalRawStructure<IoUringCQ> {
    private final CompletionQueueEntry cqEntry = CompletionQueueEntry.at(0);
    private final IoUring ioUring;

    private long kheadAddr;
    private long ktailAddr;
    private long mask;
    private long cqesAddress;

    private IoUringCQ(long address, IoUring ioUring) {
        super(address, IoUringCQOffsets.SIZE);
        this.ioUring = ioUring;
        adjustAddresses();
    }

    public static IoUringCQ at(long address, IoUring ioUring) {
        return new IoUringCQ(address, ioUring);
    }

    private void adjustAddresses() {
        kheadAddr = getLong(khead);
        ktailAddr = getLong(ktail);
        mask = RawMemory.getLong(getLong(kring_mask)) & 0x0000_0000_FFFF_FFFFL;
        cqesAddress = getLong(cqes);
    }

    @Override
    public void reposition(long address) {
        super.reposition(address);
        adjustAddresses();
    }

    public int ready() {
        return (int) (RawMemory.getLongVolatile(ktailAddr) - RawMemory.getLong(kheadAddr));
    }

    public int processCompletions(ObjectHeap<CompletionHandler> pendingCompletions, Proactor proactor) {
        var ready = ready();

        if (ready == 0) {
            if (ioUring.submissionQueue().needsFlush()) {
                ioUring.enter(0, 0, UringEnterFlags.IORING_ENTER_GETEVENTS.mask());
                ready = ready();
            }
        }

        if (ready > 0) {
            var head = RawMemory.getLong(kheadAddr);
            var last = head + ready;

            for(; head != last; head++) {
                cqEntry.reposition(cqesAddress + ((head & mask) << 4));
                pendingCompletions.releaseUnsafe((int) cqEntry.userData())
                                  .accept(cqEntry.res(), cqEntry.flags(), proactor);
            }

            advanceCQ(ready);
        }

        return ready;
    }

    public void advanceCQ(int count) {
        RawMemory.putLongVolatile(kheadAddr, RawMemory.getLong(kheadAddr) + count);
    }
}
