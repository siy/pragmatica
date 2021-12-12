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

package org.pfj.io.uring.struct.raw;

import org.pfj.io.uring.struct.AbstractExternalRawStructure;
import org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets;

import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.accept_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.addr;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.addr2;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.buf_group;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.buf_index;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.cancel_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.fadvise_advice;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.fd;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.fsync_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.ioprio;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.len;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.msg_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.off;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.opcode;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.open_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.personality;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.poll_events;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.rw_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.splice_fd_in;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.splice_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.splice_off_in;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.statx_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.sync_range_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.timeout_flags;
import static org.pfj.io.uring.struct.shape.SubmitQueueEntryOffsets.user_data;

public class SubmitQueueEntry extends AbstractExternalRawStructure<SubmitQueueEntry> {
    public static final int IORING_FSYNC_DATASYNC = 1;      /* sqe->fsync_flags */
    public static final int IORING_TIMEOUT_ABS = 1;         /* sqe->timeout_flags */
    public static final int SPLICE_F_FD_IN_FIXED = 1 << 31; /* sqe->splice_flags, extends splice(2) flags */

    //  SubmissionFlags
    public static final int IOSQE_FIXED_FILE = 1;       /* issue after inflight IO */
    public static final int IOSQE_IO_DRAIN = 2;
    public static final int IOSQE_IO_LINK = 4;          /* links next sqe */
    public static final int IOSQE_IO_HARDLINK = 8;      /* like LINK, but stronger */
    public static final int IOSQE_ASYNC = 16;           /* always go async */
    public static final int IOSQE_BUFFER_SELECT = 32;   /* select buffer from sqe->buf_group */

    private SubmitQueueEntry(final long address) {
        super(address, SubmitQueueEntryOffsets.SIZE);
    }

    public static SubmitQueueEntry at(final long address) {
        return new SubmitQueueEntry(address);
    }

    public SubmitQueueEntry opcode(final byte data) {
        return putByte(opcode, data);
    }

    public SubmitQueueEntry flags(final byte data) {
        return putByte(flags, data);
    }

    public SubmitQueueEntry ioprio(final short data) {
        return putShort(ioprio, data);
    }

    public SubmitQueueEntry pollEvents(final short data) {
        return putShort(poll_events, data);
    }

    public SubmitQueueEntry bufIndex(final short data) {
        return putShort(buf_index, data);
    }

    public SubmitQueueEntry bufGroup(final short data) {
        return putShort(buf_group, data);
    }

    public SubmitQueueEntry personality(final short data) {
        return putShort(personality, data);
    }

    public SubmitQueueEntry fd(final int data) {
        return putInt(fd, data);
    }

    public SubmitQueueEntry len(final int data) {
        return putInt(len, data);
    }

    public SubmitQueueEntry rwFlags(final int data) {
        return putInt(rw_flags, data);
    }

    public SubmitQueueEntry fsyncFlags(final int data) {
        return putInt(fsync_flags, data);
    }

    public SubmitQueueEntry syncRangeFlags(final int data) {
        return putInt(sync_range_flags, data);
    }

    public SubmitQueueEntry msgFlags(final int data) {
        return putInt(msg_flags, data);
    }

    public SubmitQueueEntry timeoutFlags(final int data) {
        return putInt(timeout_flags, data);
    }

    public SubmitQueueEntry acceptFlags(final int data) {
        return putInt(accept_flags, data);
    }

    public SubmitQueueEntry cancelFlags(final int data) {
        return putInt(cancel_flags, data);
    }

    public SubmitQueueEntry openFlags(final int data) {
        return putInt(open_flags, data);
    }

    public SubmitQueueEntry statxFlags(final int data) {
        return putInt(statx_flags, data);
    }

    public SubmitQueueEntry fadviseAdvice(final int data) {
        return putInt(fadvise_advice, data);
    }

    public SubmitQueueEntry spliceFlags(final int data) {
        return putInt(splice_flags, data);
    }

    public SubmitQueueEntry spliceFdIn(final int data) {
        return putInt(splice_fd_in, data);
    }

    public SubmitQueueEntry off(final long data) {
        return putLong(off, data);
    }

    public SubmitQueueEntry addr2(final long data) {
        return putLong(addr2, data);
    }

    public SubmitQueueEntry addr(final long data) {
        return putLong(addr, data);
    }

    public SubmitQueueEntry spliceOffIn(final long data) {
        return putLong(splice_off_in, data);
    }

    public SubmitQueueEntry userData(final long data) {
        return putLong(user_data, data);
    }
}