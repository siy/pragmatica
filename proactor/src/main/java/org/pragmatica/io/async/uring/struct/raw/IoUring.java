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
import org.pragmatica.io.async.uring.UringNative;
import org.pragmatica.io.async.uring.UringSetupFlags;
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

    public int ringFd() {
        return getInt(ring_fd);
    }

    public int features() {
        return getInt(features);
    }

    public long enter(long toSubmit, long minComplete, int flags) {
        return UringNative.enter(address(), toSubmit, minComplete, flags);
    }

    private int submit(int submitted, int waitNr) {
        var flags = new int[] {0};

        if (submissionQueue.needsEnter(flags) || waitNr != 0) {
            if (waitNr != 0 || (flags() & UringSetupFlags.IOPOLL.mask()) != 0) {
                flags[0] |= UringEnterFlags.IORING_ENTER_GETEVENTS.mask();
            }

            return (int) enter(submitted, waitNr, flags[0]);
        }

        return submitted;
    }

    public int submitAndWait(int waitNr) {
        return submit(submissionQueue.flush(), waitNr);
    }

    /*
static int __io_uring_submit(struct io_uring *ring, unsigned submitted,
			     unsigned wait_nr)
{
	unsigned flags;
	int ret;

	flags = 0;
	if (sq_ring_needs_enter(ring, &flags) || wait_nr) {
		if (wait_nr || (ring->flags & IORING_SETUP_IOPOLL))
			flags |= IORING_ENTER_GETEVENTS;

		ret = ____sys_io_uring_enter(ring->ring_fd, submitted, wait_nr,
					     flags, NULL);
	} else
		ret = submitted;

	return ret;
}

     */
}
