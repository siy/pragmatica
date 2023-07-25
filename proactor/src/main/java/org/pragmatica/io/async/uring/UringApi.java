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

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.exchange.ExchangeEntry;
import org.pragmatica.io.async.uring.exchange.ExchangeEntryFactory;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.CQEntry;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.raw.RawMemory;
import org.pragmatica.lang.Functions.FN1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedTransferQueue;

import static org.pragmatica.io.async.uring.exchange.ExchangeEntryFactory.exchangeEntryFactory;

/**
 * Low-level IO URING API
 */
public class UringApi {
    private static final Logger LOG = LoggerFactory.getLogger(UringApi.class);

    public static final int MIN_QUEUE_SIZE = 128;
    private static final int ENTRY_SIZE = 8;    // each entry is a 64-bit pointer

    private final OffHeapSlice ringBuffer;
    private final OffHeapSlice completionBuffer;
    private final OffHeapSlice submissionBuffer;
    private final CQEntry cqEntry;
    private final SQEntry sqEntry;
    private final int entriesCount;
    private final ExchangeEntryFactory factory;
    private final Queue<ExchangeEntry<?>> queue = new LinkedTransferQueue<>();

    private boolean closed = false;

    private UringApi(int numEntries) {
        this.entriesCount = (numEntries <= MIN_QUEUE_SIZE) ?
                            MIN_QUEUE_SIZE : 1 << (32 - Integer.numberOfLeadingZeros(numEntries - 1));

        this.ringBuffer = OffHeapSlice.fixedSize(UringNative.SIZE);
        this.submissionBuffer = OffHeapSlice.fixedSize(entriesCount * ENTRY_SIZE);
        this.completionBuffer = OffHeapSlice.fixedSize(entriesCount * 2 * ENTRY_SIZE);
        this.cqEntry = CQEntry.at(0);
        this.sqEntry = SQEntry.at(0);
        this.factory = exchangeEntryFactory();
    }

    public static Result<UringApi> uringApi(int requestedEntries, Set<UringSetupFlags> openFlags) {
        var uringApi = new UringApi(requestedEntries);
        var rc = uringApi.init(openFlags);

        if (rc != 0) {
            uringApi.shutdown();
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
        try (var vector = OffHeapIoVector.withReadBuffers(buffers)) {
            int rc = register(RegisterOperation.IORING_REGISTER_BUFFERS, vector.address(), vector.length());

            if (rc < 0) {
                return SystemError.result(rc);
            }

            return Result.success(buffers);
        }
    }

    public Result<Unit> unregisterBuffers() {
        int rc = register(RegisterOperation.IORING_REGISTER_BUFFERS, 0L, 0L);

        return rc < 0 ? SystemError.result(rc) : Unit.unitResult();
    }

    public synchronized void shutdown() {
        if (closed) {
            return;
        }

        UringNative.exit(ringBuffer.address());
        submissionBuffer.close();
        completionBuffer.close();
        ringBuffer.close();
        factory.clear();
        closed = true;
    }

    public void waitForCompletions(int count) {
        UringNative.submitAndWait(ringBuffer.address(), count);
    }

    public void processCompletions(Proactor proactor) {
        long ready = UringNative.peekBatchAndAdvanceCQE(ringBuffer.address(), completionBuffer.address(), completionBuffer.size() / ENTRY_SIZE);

        for (long i = 0, address = completionBuffer.address(); i < ready; i++, address += ENTRY_SIZE) {
            cqEntry.reposition(RawMemory.getLong(address));

            var element = factory.registry().lookup((int) cqEntry.userData());

            if (!element.isUsable()) {
                LOG.warn("Received completion for already finished element: {}", cqEntry.userData());
                continue;
            }

//            if (element instanceof AbstractExchangeEntry<?, ?> xe) {
//                if (xe.completion == null) {
//                    LOG.warn("No element found for user data: {}", cqEntry.userData());
//                    continue;
//                }
//            }

            element.accept(cqEntry.res(), cqEntry.flags(), proactor);
        }
    }

    public void processSubmissions() {
        int available = UringNative.peekBatchSQE(ringBuffer.address(),
                                                 submissionBuffer.address(),
                                                 Math.min(queue.size(), entriesCount));

        for (long i = 0, address = submissionBuffer.address(); i < available; i++, address += ENTRY_SIZE) {
            sqEntry.reposition(RawMemory.getLong(address));
            queue.remove().apply(sqEntry.clear());
        }
        //UringNative.submitAndWait(ringBuffer.address(), 0);
    }

    //public void submit(ExchangeEntry<?> entry) {
    public void submit(FN1<ExchangeEntry<?>, ExchangeEntryFactory> fn) {
        queue.add(fn.apply(factory));
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

        try (var offHeapAddress = OffHeapSocketAddress.unsafeSocketAddress(address)) {
            var rc = UringNative.listen(fd.descriptor(), offHeapAddress.sockAddrPtr(), offHeapAddress.sockAddrSize(), queueLen);

            return SystemError.result(rc, __ -> fd);
        }
    }
}
