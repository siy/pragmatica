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

package org.pragmatica.io.async.uring;

import org.jctools.queues.MpscArrayQueue;
import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.exchange.ExchangeEntry;
import org.pragmatica.io.async.uring.exchange.ExchangeEntryPool;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.CQEntry;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.struct.shape.CompletionQueueEntryOffsets;
import org.pragmatica.io.async.uring.struct.shape.SubmitQueueEntryOffsets;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.Queue;
import java.util.Set;

/**
 * Low-level IO URING API
 */
public class UringApi {
    public static final int MIN_QUEUE_SIZE = 128;
    private final OffHeapSlice ringBuffer;
    private final OffHeapSlice completionEntriesBuffer;
    private final OffHeapSlice submissionEntriesBuffer;
    private final CQEntry cqEntry;
    private final SQEntry sqEntry;
    private final int entriesCount;
    private final ExchangeEntryPool pool;
    private final Queue<ExchangeEntry<?>> queue = new MpscArrayQueue<>(MIN_QUEUE_SIZE * MIN_QUEUE_SIZE);
    private boolean closed = false;

    private UringApi(int numEntries, ExchangeEntryPool pool) {
        this.pool = pool;
        this.entriesCount = (numEntries <= MIN_QUEUE_SIZE) ?
                            MIN_QUEUE_SIZE : 1 << (32 - Integer.numberOfLeadingZeros(numEntries - 1));

        this.ringBuffer = OffHeapSlice.fixedSize(UringNative.SIZE);
        this.submissionEntriesBuffer = OffHeapSlice.fixedSize(entriesCount * SubmitQueueEntryOffsets.SIZE);
        this.completionEntriesBuffer = OffHeapSlice.fixedSize(entriesCount * 2 * CompletionQueueEntryOffsets.SIZE);
        this.cqEntry = CQEntry.at(0);
        this.sqEntry = SQEntry.at(0);
    }

    public static Result<UringApi> uringApi(int requestedEntries, Set<UringSetupFlags> openFlags, ExchangeEntryPool pool) {
        var uringApi = new UringApi(requestedEntries, pool);
        var rc = uringApi.init(openFlags);

        if (rc != 0) {
            uringApi.shutdown();
            pool.clear();
            return SystemError.fromCode(rc).result();
        }

        return Result.success(uringApi);
    }

    private int init(Set<UringSetupFlags> openFlags) {
        return UringNative.init(entriesCount, ringBuffer.address(), Bitmask.combine(openFlags));
    }

    public int register(RegisterOperation op, long arg1, long arg2) {
        return UringNative.register(ringBuffer.address(), op.ordinal(), arg1, arg2);
    }

    public Result<OffHeapSlice[]> registerBuffers(OffHeapSlice... buffers) {
        var vector = OffHeapIoVector.withReadBuffers(buffers);
        try {
            int rc = register(RegisterOperation.IORING_REGISTER_BUFFERS, vector.address(), vector.length());

            if (rc < 0) {
                return SystemError.result(rc);
            }

            return Result.success(buffers);
        } finally {
            vector.close();
        }
    }

    public Result<Unit> unregisterBuffers() {
        int rc = register(RegisterOperation.IORING_REGISTER_BUFFERS, 0L, 0L);

        return rc < 0
               ? SystemError.result(rc)
               : Unit.unitResult();
    }

    public synchronized void shutdown() {
        if (closed) {
            return;
        }

        UringNative.exit(ringBuffer.address());
        submissionEntriesBuffer.close();
        completionEntriesBuffer.close();
        ringBuffer.close();
        pool.clear();
        closed = true;
    }

    public int processCompletions(Proactor proactor) {
        long ready = UringNative.copyCQES(ringBuffer.address(), completionEntriesBuffer.address(), completionEntriesBuffer.size() / CompletionQueueEntryOffsets.SIZE);

        for (long i = 0, address = completionEntriesBuffer.address(); i < ready; i++, address += CompletionQueueEntryOffsets.SIZE) {
            cqEntry.reposition(address);
            long key = cqEntry.userData();
            int res = cqEntry.res();
            int flags = cqEntry.flags();
            pool.completeRequest(key, res, flags, proactor);
        }
        return (int) ready;
    }

    public int processSubmissions() {
        var address = submissionEntriesBuffer.address();
        int filled = 0;

        while (true) {
            var entry = queue.poll();

            if (entry == null) {
                break;
            }

            if (filled > (entriesCount - 2)) { // entry may have timeout, so we need to ensure at least 2 empty slots
                break;
            }

            sqEntry.reposition(address);
            entry.fill(sqEntry);

            address += SubmitQueueEntryOffsets.SIZE;
            filled++;

            if (entry.hasTimeout()) {
                sqEntry.reposition(address);
                entry.fillTimeout(sqEntry);

                address += SubmitQueueEntryOffsets.SIZE;
                filled++;
            }
        }

        UringNative.submit(ringBuffer.address(), submissionEntriesBuffer.address(), filled, SubmissionFlags.IMMEDIATE.mask());
        return filled;
    }

    public void submit(ExchangeEntry<?> entry) {
        queue.offer(entry);
    }

    public static Result<FileDescriptor> socket(AddressFamily af, SocketType type, Set<SocketFlag> flags, Set<SocketOption> options) {
        return SystemError.result(UringNative.socket(af.familyId(), type.code() | Bitmask.combine(flags), Bitmask.combine(options)),
                                  (af == AddressFamily.INET6) ? FileDescriptor::socket6 : FileDescriptor::socket);
    }

    public static <T extends InetAddress> Result<ListenContext<T>> listen(SocketAddress<T> address, SocketType type, Set<SocketFlag> flags,
                                                                          Set<SocketOption> options, SizeT queueLen) {
        var len = (int) queueLen.value();

        return socket(address.family(), type, flags, options)
            .flatMap(fd -> configureForListen(fd, address, len))
            .map(fd -> ListenContext.listenContext(fd, address, len));
    }

    private static <T extends InetAddress> Result<FileDescriptor> configureForListen(FileDescriptor fd, SocketAddress<T> address, int queueLen) {
        if (!fd.isSocket()) {
            return SystemError.ENOTSOCK.result();
        }

        var offHeapAddress = OffHeapSocketAddress.unsafeSocketAddress(address);
        try {
            var rc = UringNative.listen(fd.descriptor(), offHeapAddress.sockAddrPtr(), offHeapAddress.sockAddrSize(), queueLen);
            return SystemError.result(rc, __ -> fd);
        } finally {
            offHeapAddress.close();
        }
    }
}
