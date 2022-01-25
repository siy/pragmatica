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
import org.pragmatica.io.async.uring.struct.shape.IoUringOffsets;

import static org.pragmatica.io.async.uring.struct.shape.IoUringOffsets.*;

public class IoUring extends AbstractExternalRawStructure<IoUring> {
    private final IoUringSQ submissionQueue;
    private final IoUringCQ completionQueue;

    private IoUring(long address) {
        super(address, IoUringOffsets.SIZE);

        submissionQueue = IoUringSQ.at(sqAddress(), this);
        completionQueue = IoUringCQ.at(cqAddress(), this);
    }

    public static IoUring at(long address) {
        return new IoUring(address);
    }

    @Override
    protected void address(long address) {
        super.address(address);
        submissionQueue.reposition(sqAddress());
        completionQueue.reposition(cqAddress());
    }

    private long sqAddress() {
        return address() + sq.offset();
    }

    private long cqAddress() {
        return address() + cq.offset();
    }

    public IoUringSQ submissionQueue() {
        return submissionQueue;
    }

    public IoUringCQ completionQueue() {
        return completionQueue;
    }

    public int flags() {
        return getInt(flags);
    }

    public long enter(long toSubmit, long minComplete, int flags) {
        return UringApi.enter(address(), toSubmit, minComplete, flags);
    }

    public int submitAndWait(int waitNr) {
        int submitted = submissionQueue.flush();
        var flags1 = new int[] {0};

        if (submissionQueue.needsEnter(flags1) || waitNr != 0) {
            if (waitNr != 0 || (flags() & UringSetupFlags.IO_POLL.mask()) != 0) {
                flags1[0] |= UringEnterFlags.GET_EVENTS.mask();
            }

            return (int) enter(submitted, waitNr, flags1[0]);
        }

        return submitted;
    }
}
