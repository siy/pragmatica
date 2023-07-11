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

package org.pragmatica.io.async.uring;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.raw.CQEntry;
import org.pragmatica.io.async.uring.struct.shape.IoUringCQOffsets;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.util.raw.RawMemory;

import static org.pragmatica.io.async.uring.struct.shape.IoUringCQOffsets.*;

/**
 * Representation of the internals of the {@code io_uring_cq} structure.
 */
public class IoUringCQ extends AbstractExternalRawStructure<IoUringCQ> {
    private final CQEntry cqEntry = CQEntry.at(0);

    private long kheadAddr;
    private long ktailAddr;
    private long mask;
    private long cqesAddress;

    private IoUringCQ(long address) {
        super(address, IoUringCQOffsets.SIZE);
    }

    public static IoUringCQ at(long address) {
        return new IoUringCQ(address);
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

    public int processCompletions(ObjectHeap<CompletionHandler> pendingCompletions, Proactor proactor) {
        var head = RawMemory.getLong(kheadAddr);
        var ready = RawMemory.getLongVolatile(ktailAddr) - head;

        if (ready > 0) {
            var last = head + ready;

            for (; head != last; head++) {
                cqEntry.reposition(cqesAddress + ((head & mask) << 4));
                pendingCompletions.elementUnsafe((int) cqEntry.userData())
                                  .accept(cqEntry.res(), cqEntry.flags(), proactor);
            }

            RawMemory.putLongVolatile(kheadAddr, last);
        }

        return (int) ready;
    }
}
