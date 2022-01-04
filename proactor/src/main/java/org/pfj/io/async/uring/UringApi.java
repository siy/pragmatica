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

package org.pfj.io.async.uring;

import org.pfj.io.async.Proactor;
import org.pfj.io.async.SystemError;
import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.net.*;
import org.pfj.io.async.uring.exchange.ExchangeEntry;
import org.pfj.io.async.uring.struct.raw.CompletionQueueEntry;
import org.pfj.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.async.uring.utils.ObjectHeap;
import org.pfj.io.async.util.raw.RawMemory;
import org.pfj.lang.Result;

import java.util.Deque;
import java.util.Set;

import static org.pfj.io.async.SystemError.*;
import static org.pfj.io.async.uring.struct.offheap.OffHeapSocketAddress.addressIn;
import static org.pfj.io.async.uring.struct.offheap.OffHeapSocketAddress.addressIn6;
import static org.pfj.lang.Result.success;

/**
 * Low-level IO URING API
 */
public class UringApi implements AutoCloseable {
    public static final int MIN_QUEUE_SIZE = 128;
    private static final long ENTRY_SIZE = 8L;    // each entry is a 64-bit pointer

    private final long ringBase;
    private final int submissionEntries;
    private final long submissionBuffer;

    private final int completionEntries;
    private final long completionBuffer;

    private final CompletionQueueEntry cqEntry;
    private final SubmitQueueEntry sqEntry;

    private boolean closed = false;

    private UringApi(int numEntries, long ringBase) {
        submissionEntries = numEntries;
        completionEntries = numEntries * 2;
        this.ringBase = ringBase;
        submissionBuffer = RawMemory.allocate(submissionEntries * ENTRY_SIZE);
        completionBuffer = RawMemory.allocate(completionEntries * ENTRY_SIZE);
        cqEntry = CompletionQueueEntry.at(0);
        sqEntry = SubmitQueueEntry.at(0);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        UringNative.close(ringBase);
        RawMemory.dispose(submissionBuffer);
        RawMemory.dispose(completionBuffer);
        RawMemory.dispose(ringBase);
        closed = true;
    }

    public void processCompletions(ObjectHeap<CompletionHandler> pendingCompletions, Proactor proactor) {
        long ready = UringNative.peekCQ(ringBase, completionBuffer, completionEntries);

        for (long i = 0, address = completionBuffer; i < ready; i++, address += ENTRY_SIZE) {
            cqEntry.reposition(RawMemory.getLong(address));

            pendingCompletions.releaseUnsafe((int) cqEntry.userData())
                              .accept(cqEntry.res(), cqEntry.flags(), proactor);
        }

        if (ready > 0) {
            UringNative.advanceCQ(ringBase, ready);
        }
    }

    public void processSubmissions(Deque<ExchangeEntry<?>> queue) {
        int available = UringNative.peekSQEntries(ringBase,
                                                  submissionBuffer,
                                                  Math.min(queue.size(), submissionEntries));

        for (long i = 0, address = submissionBuffer; i < available; i++, address += ENTRY_SIZE) {
            sqEntry.reposition(RawMemory.getLong(address));
            queue.removeFirst().apply(sqEntry.clear());
        }

        UringNative.submitAndWait(ringBase, 0);
    }

    public static Result<UringApi> uringApi(int requestedEntries, Set<UringSetupFlags> openFlags) {
        long ringBase = RawMemory.allocate(UringNative.SIZE);
        int numEntries = calculateNumEntries(requestedEntries);
        int rc = UringNative.init(numEntries, ringBase, Bitmask.combine(openFlags));

        if (rc != 0) {
            RawMemory.dispose(ringBase);
            return SystemError.fromCode(rc).result();
        }

        return success(new UringApi(numEntries, ringBase));
    }

    private static int calculateNumEntries(int size) {
        if (size <= MIN_QUEUE_SIZE) {
            return MIN_QUEUE_SIZE;
        }

        //Round up to the nearest power of two
        return 1 << (32 - Integer.numberOfLeadingZeros(size - 1));
    }

    public int numEntries() {
        return submissionEntries;
    }

    public static Result<FileDescriptor> socket(AddressFamily af, SocketType type, Set<SocketFlag> flags, Set<SocketOption> options) {
        return result(UringNative.socket(af.familyId(), type.code() | Bitmask.combine(flags), Bitmask.combine(options)),
                      (af == AddressFamily.INET6) ? FileDescriptor::socket6 : FileDescriptor::socket);
    }

    public static Result<ServerContext<?>> server(SocketAddress<?> address, SocketType type, Set<SocketFlag> flags,
                                                  Set<SocketOption> options, SizeT queueLen) {

        var qlen = (int) queueLen.value();

        return socket(address.family(), type, flags, options)
            .flatMap(fd -> configureForListen(fd, address, qlen))
            .map(fd -> ServerContext.serverContext(fd, address, qlen));
    }

    private static Result<FileDescriptor> configureForListen(FileDescriptor fd, SocketAddress<?> address, int qlen) {
        if (!fd.isSocket()) {
            return ENOTSOCK.result();
        }

        var rc = switch (address.family()) {
            case INET -> configureForInet(fd, address, qlen);
            case INET6 -> configureForInet6(fd, address, qlen);
            default -> EPFNOSUPPORT.code();
        };

        return result(rc, __ -> fd);
    }

    private static int configureForInet6(FileDescriptor fd, SocketAddress<?> address, int qlen) {
        if (!fd.isSocket6() || address.family() != AddressFamily.INET6) {
            return -EPFNOSUPPORT.code();
        }

        if (address instanceof SocketAddressIn6 socketAddressIn6) {
            var addressIn6 = addressIn6(socketAddressIn6);

            return UringNative.prepareForListen(fd.descriptor(), addressIn6.sockAddrPtr(), addressIn6.sockAddrSize(), qlen);
        } else {
            return -EPFNOSUPPORT.code();
        }
    }

    private static int configureForInet(FileDescriptor fd, SocketAddress<?> address, int qlen) {
        if (fd.isSocket6() || address.family() == AddressFamily.INET6) {
            return -EPFNOSUPPORT.code();
        }

        if (address instanceof SocketAddressIn socketAddressIn) {
            var addressIn = addressIn(socketAddressIn);

            return UringNative.prepareForListen(fd.descriptor(), addressIn.sockAddrPtr(), addressIn.sockAddrSize(), qlen);

        } else {
            return -EPFNOSUPPORT.code();
        }
    }
}