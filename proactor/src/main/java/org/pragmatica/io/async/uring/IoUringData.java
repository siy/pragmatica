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

import org.pragmatica.io.async.uring.struct.offheap.AbstractOffHeapStructure;

import java.util.Set;

public class IoUringData extends AbstractOffHeapStructure<IoUringData> {
    private final IoUring ioUring;
    private final int numEntries;
    private final int workQueueFD;
    private final int flags;

    private IoUringData(int requestedEntries, Set<UringSetupFlags> openFlags, int workQueueFD) {
        super(IoUring.RAW_SIZE);
        clear();

        this.numEntries = calculateNumEntries(requestedEntries);
        this.ioUring = IoUring.at(address());
        this.flags = Bitmask.combine(openFlags);
        this.workQueueFD = workQueueFD;
    }

    public static IoUringData create(int requestedEntries, Set<UringSetupFlags> openFlags, int workQueueFD) {
        return new IoUringData(requestedEntries, openFlags, workQueueFD);
    }

    private static int calculateNumEntries(int size) {
        if (size <= UringApi.MIN_QUEUE_SIZE) {
            return UringApi.MIN_QUEUE_SIZE;
        }

        //Round up to the nearest power of two
        return 1 << (32 - Integer.numberOfLeadingZeros(size - 1));
    }

    public int numEntries() {
        return numEntries;
    }

    int init() {
        ioUring.params().flags(flags);
        ioUring.params().workQueueFD(workQueueFD);

        var rc = UringApi.init(numEntries, ioUring.address());

        if (rc == 0) {
            ioUring.reposition(address());
        }

        return rc;
    }

    public int fd() {
        return ioUring.fd();
    }

    public IoUringCQ completionQueue() {
        return ioUring.completionQueue();
    }

    public int submitAndWait(int waitNr) {
        return ioUring.submitAndWait(waitNr);
    }

    public IoUringSQ submissionQueue() {
        return ioUring.submissionQueue();
    }

    @Override
    public void close() {
        UringApi.close(address());
        super.close();
    }
}
