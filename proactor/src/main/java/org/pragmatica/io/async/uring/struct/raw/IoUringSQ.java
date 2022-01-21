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

import org.pragmatica.io.async.uring.UringEnterFlags;
import org.pragmatica.io.async.uring.UringSetupFlags;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.IoUringSQOffsets;
import org.pragmatica.io.async.util.raw.RawMemory;

import static org.pragmatica.io.async.uring.struct.shape.IoUringCQOffsets.khead;
import static org.pragmatica.io.async.uring.struct.shape.IoUringSQOffsets.*;

public class IoUringSQ extends AbstractExternalRawStructure<IoUringSQ> {
    public static final long IORING_SQ_NEED_WAKEUP = (1L << 0); /* needs io_uring_enter wakeup */
    public static final long IORING_SQ_CQ_OVERFLOW = (1L << 1); /* CQ ring is overflown */

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
        adjustAddresses();
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
        mask = RawMemory.getLong(getLong(kring_mask)) & 0x0000_FFFF_FFFF_FFFFL;
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

    int flush() {
        var ktail = RawMemory.getLong(ktailAddress);
        var toSubmit = getInt(sqe_tail) - getInt(sqe_head);

        if (toSubmit != 0) {
            do {
                var ptr = arrayAddress + ((ktail & mask) << 3);
                var head = getInt(sqe_head);
                RawMemory.putLong(ptr, head & mask);
                ktail++;
                putInt(sqe_head, head + 1);
            } while (--toSubmit > 0);

            RawMemory.putLongVolatile(ktailAddress, ktail);
        }
        return (int) (ktail - RawMemory.getLong(kheadAddr));
    }

    boolean needsEnter(int[] flags) {
        if ((ioUring.flags() & UringSetupFlags.SQPOLL.mask()) == 0) {
            return true;
        }

        if ((RawMemory.getLong(kflagsAddress) & IORING_SQ_NEED_WAKEUP) != 0) {
            flags[0] |= UringEnterFlags.IORING_ENTER_SQ_WAKEUP.mask();
            return true;
        }

        return false;
    }

    /*
static inline bool sq_ring_needs_enter(struct io_uring *ring, unsigned *flags)
{
	if (!(ring->flags & IORING_SETUP_SQPOLL))
		return true;

	if (uring_unlikely(IO_URING_READ_ONCE(*ring->sq.kflags) &
			   IORING_SQ_NEED_WAKEUP)) {
		*flags |= IORING_ENTER_SQ_WAKEUP;
		return true;
	}

	return false;
}


int __io_uring_flush_sq(struct io_uring *ring)
{
	struct io_uring_sq *sq = &ring->sq;
	const unsigned mask = *sq->kring_mask;
	unsigned ktail = *sq->ktail;
	unsigned to_submit = sq->sqe_tail - sq->sqe_head;

	if (!to_submit)
		goto out;

	 // Fill in sqes that we have queued up, adding them to the kernel ring
	do {
        sq->array[ktail & mask] = sq->sqe_head & mask;
        ktail++;
        sq->sqe_head++;
    } while (--to_submit);

     // Ensure that the kernel sees the SQE updates before it sees the tail update.
    io_uring_smp_store_release(sq->ktail, ktail);
    out:

         // This _may_ look problematic, as we're not supposed to be reading
         // SQ->head without acquire semantics. When we're in SQPOLL mode, the
         // kernel submitter could be updating this right now. For non-SQPOLL,
         // task itself does it, and there's no potential race. But even for
         // SQPOLL, the load is going to be potentially out-of-date the very
         // instant it's done, regardless or whether or not it's done
         // atomically. Worst case, we're going to be over-estimating what
         // we can submit. The point is, we need to be able to deal with this
         // situation regardless of any perceived atomicity.

        return ktail - *sq->khead;
}

    struct io_uring_sq {
        unsigned *khead;
        unsigned *ktail;
        unsigned *kring_mask;
        unsigned *kring_entries;
        unsigned *kflags;
        unsigned *kdropped;
        unsigned *array;
        struct io_uring_sqe *sqes;

        unsigned sqe_head;
        unsigned sqe_tail;

        size_t ring_sz;
        void *ring_ptr;

        unsigned pad[4];
    };

    struct io_uring_sqe *io_uring_get_sqe(struct io_uring *ring)
    {
        struct io_uring_sq *sq = &ring->sq;
        unsigned int head = io_uring_smp_load_acquire(sq->khead);
        unsigned int next = sq->sqe_tail + 1;
        struct io_uring_sqe *sqe = NULL;

        if (next - head <= *sq->kring_entries) {
            sqe = &sq->sqes[sq->sqe_tail & *sq->kring_mask];
            sq->sqe_tail = next;
        }
        return sqe;
    }
         */
}
