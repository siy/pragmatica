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

package org.pragmatica.io.async.uring.struct.raw;

import org.pragmatica.io.async.uring.exchange.Opcode;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.SubmitQueueEntryOffsets;

/**
 * Submission Queue Entry
 */
public class SQEntry extends AbstractExternalRawStructure<SQEntry> {
    public static final int IORING_FSYNC_DATASYNC = 1;      /* sqe->fsync_flags */
    public static final int IORING_TIMEOUT_ABS = 1;         /* sqe->timeout_flags */
    public static final int SPLICE_F_FD_IN_FIXED = 1 << 31; /* sqe->splice_flags, extends splice(2) flags */

    private SQEntry(final long address) {
        super(address, SubmitQueueEntryOffsets.SIZE);
    }

    public static SQEntry at(final long address) {
        return new SQEntry(address);
    }

    public SQEntry opcode(Opcode opcode) {
        return putByte(SubmitQueueEntryOffsets.opcode, opcode.opcode());
    }

    public SQEntry flags(final byte data) {
        return putByte(SubmitQueueEntryOffsets.flags, data);
    }

    public SQEntry ioprio(final short data) {
        return putShort(SubmitQueueEntryOffsets.ioprio, data);
    }

    public SQEntry pollEvents(final short data) {
        return putShort(SubmitQueueEntryOffsets.poll_events, data);
    }

    public SQEntry bufIndex(final short data) {
        return putShort(SubmitQueueEntryOffsets.buf_index, data);
    }

    public SQEntry bufGroup(final short data) {
        return putShort(SubmitQueueEntryOffsets.buf_group, data);
    }

    public SQEntry personality(final short data) {
        return putShort(SubmitQueueEntryOffsets.personality, data);
    }

    public SQEntry fd(final int data) {
        return putInt(SubmitQueueEntryOffsets.fd, data);
    }

    public SQEntry len(final int data) {
        return putInt(SubmitQueueEntryOffsets.len, data);
    }

    public SQEntry rwFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.rw_flags, data);
    }

    public SQEntry syncFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.fsync_flags, data);
    }

    public SQEntry syncRangeFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.sync_range_flags, data);
    }

    public SQEntry msgFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.msg_flags, data);
    }

    public SQEntry timeoutFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.timeout_flags, data);
    }

    public SQEntry acceptFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.accept_flags, data);
    }

    public SQEntry cancelFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.cancel_flags, data);
    }

    public SQEntry openFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.open_flags, data);
    }

    public SQEntry statxFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.statx_flags, data);
    }

    public SQEntry fadviseAdvice(final int data) {
        return putInt(SubmitQueueEntryOffsets.fadvise_advice, data);
    }

    public SQEntry spliceFlags(final int data) {
        return putInt(SubmitQueueEntryOffsets.splice_flags, data);
    }

    public SQEntry spliceFdIn(final int data) {
        return putInt(SubmitQueueEntryOffsets.splice_fd_in, data);
    }

    public SQEntry off(final long data) {
        return putLong(SubmitQueueEntryOffsets.off, data);
    }

    public SQEntry addr2(final long data) {
        return putLong(SubmitQueueEntryOffsets.addr2, data);
    }

    public SQEntry addr(final long data) {
        return putLong(SubmitQueueEntryOffsets.addr, data);
    }

    public SQEntry spliceOffIn(final long data) {
        return putLong(SubmitQueueEntryOffsets.splice_off_in, data);
    }

    public SQEntry userData(final long data) {
        return putLong(SubmitQueueEntryOffsets.user_data, data);
    }

    public SQEntry bufPad(final long data) {
        return putLong(SubmitQueueEntryOffsets.buf_pad, data);
    }

    public SQEntry headPad(final long data) {
        return putLong(SubmitQueueEntryOffsets.head_pad, data);
    }

    public SQEntry lenPad(final long data) {
        return putLong(SubmitQueueEntryOffsets.len_pad, data);
    }
}