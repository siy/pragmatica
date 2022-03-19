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

import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.IoUringSQOffsets;
import org.pragmatica.io.async.util.raw.RawMemory;

import static org.pragmatica.io.async.uring.struct.shape.IoUringCQOffsets.khead;
import static org.pragmatica.io.async.uring.struct.shape.IoUringSQOffsets.*;

/**
 * Representation of the internals of the {@code io_uring_sq} structure.
 */
public class IoUringSQ extends AbstractExternalRawStructure<IoUringSQ> {
    private static final long IORING_SQ_NEED_WAKEUP = (1L); /* needs io_uring_enter wakeup */
    private static final long IORING_SQ_CQ_OVERFLOW = (1L << 1); /* CQ ring is overflown */

    private final IoUring ioUring;
    private long kheadAddr;
    private long entriesCount;
    private long mask;
    private long sqesAddress;
    private long kflagsAddress;
    private long ktailAddress;
    private long arrayAddress;

    private IoUringSQ(long address, IoUring ioUring) {
        super(address, IoUringSQOffsets.SIZE);
        this.ioUring = ioUring;
    }

    public static IoUringSQ at(long address, IoUring ioUring) {
        return new IoUringSQ(address, ioUring);
    }

    @Override
    public void reposition(long address) {
        super.reposition(address);

        if (address > 0) {
            adjustAddresses();
        }
    }

    private void adjustAddresses() {
        kheadAddr = getLong(khead);
        entriesCount = RawMemory.getLong(getLong(kring_entries)) & 0x0000_0000_FFFF_FFFFL;
        mask = RawMemory.getLong(getLong(kring_mask)) & 0x0000_0000_FFFF_FFFFL;
        sqesAddress = getLong(sqes);
        kflagsAddress = getLong(kflags);
        ktailAddress = getLong(ktail);
        arrayAddress = getLong(array);
    }

    public boolean needsFlush() {
        return (RawMemory.getLong(kflagsAddress) & IORING_SQ_CQ_OVERFLOW) != 0;
    }

    public long nextSQE() {
        var head = RawMemory.getLongVolatile(kheadAddr);
        var tail = getInt(sqe_tail);
        var next = tail + 1;

        if (next - head > entriesCount) {
            return 0L;
        }

        var sqe = sqesAddress + ((tail & mask) << 6);
        putInt(sqe_tail, next);

        return sqe;
    }

    public boolean needsEnter(int[] flags) {
        if ((ioUring.flags() & UringSetupFlags.SQ_POLL.mask()) == 0) {
            return true;
        }

        if ((RawMemory.getLong(kflagsAddress) & IORING_SQ_NEED_WAKEUP) != 0) {
            flags[0] |= UringEnterFlags.SQ_WAKEUP.mask();
            return true;
        }

        return false;
    }

    public int flush() {
        var ktail = RawMemory.getLong(ktailAddress);
        var toSubmit = getInt(sqe_tail) - getInt(sqe_head);

        if (toSubmit != 0) {
            do {
                var head = getInt(sqe_head);
                RawMemory.putInt(arrayAddress + ((ktail & mask) << 2), (int) (head & mask));
                ktail++;
                putInt(sqe_head, head + 1);
            } while (--toSubmit > 0);

            RawMemory.putLongVolatile(ktailAddress, ktail);
        }
        return (int) (ktail - RawMemory.getLong(kheadAddr));
    }
}
