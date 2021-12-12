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

package org.pfj.io.uring;

import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.net.*;
import org.pfj.io.uring.exchange.ExchangeEntry;
import org.pfj.io.uring.struct.raw.CompletionQueueEntry;
import org.pfj.io.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.uring.utils.ObjectHeap;
import org.pfj.lang.Result;
import org.pfj.lang.Tuple;
import org.pfj.lang.Tuple.Tuple3;
import org.pfj.io.Bitmask;
import org.pfj.io.CompletionHandler;
import org.pfj.io.NativeFailureType;
import org.pfj.io.async.Submitter;
import org.pfj.io.raw.RawMemory;

import java.util.Deque;
import java.util.Set;

import static org.pfj.lang.Result.success;
import static org.pfj.lang.Tuple.tuple;
import static org.pfj.io.NativeFailureType.ENOTSOCK;
import static org.pfj.io.NativeFailureType.EPFNOSUPPORT;
import static org.pfj.io.NativeFailureType.result;
import static org.pfj.io.uring.struct.offheap.OffHeapSocketAddress.addressIn;
import static org.pfj.io.uring.struct.offheap.OffHeapSocketAddress.addressIn6;

public class UringHolder implements AutoCloseable {
//    public static final int DEFAULT_QUEUE_SIZE = 32;
//    public static final int DEFAULT_QUEUE_SIZE = 128;
//    public static final int DEFAULT_QUEUE_SIZE = 1024;
    public static final int DEFAULT_QUEUE_SIZE = 8192;

    private static final int ENTRY_SIZE = 8;    // each entry is a 64-bit pointer

    private final long ringBase;
    private final int submissionEntries;
    private final long submissionBuffer;

    private final int completionEntries;
    private final long completionBuffer;

    private final CompletionQueueEntry cqEntry;
    private final SubmitQueueEntry sqEntry;

    private boolean closed = false;

    private UringHolder(final int numEntries, final long ringBase) {
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

        Uring.close(ringBase);
        RawMemory.dispose(submissionBuffer);
        RawMemory.dispose(completionBuffer);
        RawMemory.dispose(ringBase);
        closed = true;
    }

    public void processCompletions(final ObjectHeap<CompletionHandler> pendingCompletions, final Submitter submitter) {
        final long ready = Uring.peekCQ(ringBase, completionBuffer, completionEntries);

        for (long i = 0, address = completionBuffer; i < ready; i++, address += ENTRY_SIZE) {
            cqEntry.reposition(RawMemory.getLong(address));

            pendingCompletions.releaseUnsafe((int) cqEntry.userData())
                              .accept(cqEntry.res(), cqEntry.flags(), submitter);
        }

        if (ready > 0) {
            Uring.advanceCQ(ringBase, ready);
        }
    }

    public void processSubmissions(final Deque<ExchangeEntry> queue) {
        final int available = Uring.peekSQEntries(ringBase,
                                                  submissionBuffer,
                                                  Math.min(queue.size(), submissionEntries));

        for (long i = 0, address = submissionBuffer; i < available; i++, address += ENTRY_SIZE) {
            sqEntry.reposition(RawMemory.getLong(address));
            queue.removeFirst().apply(sqEntry.clear());
        }

        Uring.submitAndWait(ringBase, 0);
    }

    public static Result<UringHolder> create(final int requestedEntries, final Set<UringSetupFlags> openFlags) {
        final long ringBase = RawMemory.allocate(Uring.SIZE);
        final int numEntries = calculateNumEntries(requestedEntries);
        final int rc = Uring.init(numEntries, ringBase, Bitmask.combine(openFlags));

        if (rc != 0) {
            RawMemory.dispose(ringBase);
            return NativeFailureType.fromCode(rc).result();
        }

        return success(new UringHolder(numEntries, ringBase));
    }

    private static int calculateNumEntries(final int size) {
        if (size <= 0) {
            return DEFAULT_QUEUE_SIZE;
        }
        //Round up to nearest power of two
        return 1 << (32 - Integer.numberOfLeadingZeros(size - 1));
    }

    public int numEntries() {
        return submissionEntries;
    }

    public static Result<FileDescriptor> socket(final AddressFamily addressFamily,
                                                final SocketType socketType,
                                                final Set<SocketFlag> openFlags,
                                                final Set<SocketOption> options) {
        return result(Uring.socket(addressFamily.familyId(),
                                   socketType.code() | Bitmask.combine(openFlags),
                                   Bitmask.combine(options)),
                      (addressFamily == AddressFamily.INET6) ? FileDescriptor::socket6
                                                             : FileDescriptor::socket);
    }

    public static Result<ServerContext<?>> server(final SocketAddress<?> socketAddress,
                                                  final SocketType socketType,
                                                  final Set<SocketFlag> openFlags,
                                                  final Set<SocketOption> options,
                                                  final SizeT queueDepth) {
        return socket(socketAddress.family(), socketType, openFlags, options)
                .flatMap(fileDescriptor -> configureForListen(fileDescriptor, socketAddress, (int) queueDepth.value()))
                .map(tuple -> tuple.map(ServerContext::connector));
    }

    private static Result<Tuple3<FileDescriptor, SocketAddress<?>, Integer>> configureForListen(final FileDescriptor fileDescriptor,
                                                                                                final SocketAddress<?> socketAddress,
                                                                                                final int queueDepth) {

        if (!fileDescriptor.isSocket()) {
            return ENOTSOCK.result();
        }

        final var rc = switch (socketAddress.family()) {
            case INET -> configureForInet(fileDescriptor, socketAddress, queueDepth);
            case INET6 -> configureForInet6(fileDescriptor, socketAddress, queueDepth);
            default -> EPFNOSUPPORT.code();
        };

        return result(rc, __ -> tuple(fileDescriptor, socketAddress, queueDepth));
    }

    private static int configureForInet6(final FileDescriptor fileDescriptor, final SocketAddress<?> socketAddress, final int queueDepth) {
        if (!fileDescriptor.isSocket6() || socketAddress.family() != AddressFamily.INET6) {
            return -EPFNOSUPPORT.code();
        }

        if (socketAddress instanceof SocketAddressIn6 socketAddressIn6) {
            final var addressIn6 = addressIn6(socketAddressIn6);

            return Uring.prepareForListen(fileDescriptor.descriptor(),
                                          addressIn6.sockAddrPtr(),
                                          addressIn6.sockAddrSize(),
                                          queueDepth);
        }

        return -EPFNOSUPPORT.code();
    }

    private static int configureForInet(final FileDescriptor fileDescriptor, final SocketAddress<?> socketAddress, final int queueDepth) {
        if (fileDescriptor.isSocket6() || socketAddress.family() == AddressFamily.INET6) {
            return -EPFNOSUPPORT.code();
        }

        if (socketAddress instanceof SocketAddressIn socketAddressIn) {
            final var addressIn = addressIn(socketAddressIn);

            return Uring.prepareForListen(fileDescriptor.descriptor(),
                                          addressIn.sockAddrPtr(),
                                          addressIn.sockAddrSize(),
                                          queueDepth);

        }

        return -EPFNOSUPPORT.code();
    }
}
