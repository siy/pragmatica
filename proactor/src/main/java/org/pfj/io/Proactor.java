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

package org.pfj.io;

import org.pfj.io.async.Submitter;
import org.pfj.io.async.common.OffsetT;
import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.file.FilePermission;
import org.pfj.io.async.file.OpenFlags;
import org.pfj.io.async.file.SpliceDescriptor;
import org.pfj.io.async.file.stat.FileStat;
import org.pfj.io.async.file.stat.StatFlag;
import org.pfj.io.async.file.stat.StatMask;
import org.pfj.io.async.net.*;
import org.pfj.io.async.util.OffHeapBuffer;
import org.pfj.io.scheduler.Timeout;
import org.pfj.io.uring.UringHolder;
import org.pfj.io.uring.UringSetupFlags;
import org.pfj.io.uring.exchange.ExchangeEntry;
import org.pfj.io.uring.exchange.ExchangeEntryFactory;
import org.pfj.io.uring.utils.ObjectHeap;
import org.pfj.io.uring.struct.offheap.OffHeapCString;
import org.pfj.io.uring.struct.offheap.OffHeapIoVector;
import org.pfj.io.uring.struct.offheap.OffHeapSocketAddress;
import org.pfj.lang.Option;
import org.pfj.lang.Result;
import org.pfj.lang.Unit;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Input/Output Proactor.
 * <p>
 * This class provides implementation of {@link Submitter} interface using <a href="https://en.wikipedia.org/wiki/Proactor_pattern">Proactor</a> pattern.
 * <p>
 * <pre>
 * WARNING!
 * This class is part of low level internal classes. It's written with specific assumptions in mind and most likely will be hard
 * or inconvenient to use in other environment. In particular class is designed to be used by single thread at a time and does not perform
 * synchronization at all.
 * </pre>
 */
public class Proactor implements Submitter {
    private static final int AT_FDCWD = -100; // Special value used to indicate the openat/statx functions should use the current working directory.

    private final UringHolder uringHolder;
    private final ObjectHeap<CompletionHandler> pendingCompletions;
    private final Deque<ExchangeEntry> queue = new ArrayDeque<>();
    private final ExchangeEntryFactory factory = new ExchangeEntryFactory();

    private Proactor(final UringHolder uringHolder) {
        this.uringHolder = uringHolder;
        pendingCompletions = ObjectHeap.objectHeap(uringHolder.numEntries());
    }

    public static Proactor proactor() {
        return proactor(UringHolder.DEFAULT_QUEUE_SIZE, UringSetupFlags.defaultFlags());
    }

    public static Proactor proactor(final int queueSize) {
        return proactor(queueSize, UringSetupFlags.defaultFlags());
    }

    static Proactor proactor(final int queueSize, final Set<UringSetupFlags> openFlags) {
        return new Proactor(UringHolder.create(queueSize, openFlags)
                                       .fold(f -> {
                                                 throw new IllegalStateException("Unable to initialize IO_URING interface: " + f.message());
                                             },
                                             h -> h));
    }

    @Override
    public void close() {
        uringHolder.close();
        factory.clear();
    }

    /**
     * This method should be periodically called to perform submissions and handle completions to/from IO_URING.
     */
    public Proactor processIO() {
        if (!queue.isEmpty()) {
            uringHolder.processSubmissions(queue);
        }

        if (pendingCompletions.count() > 0) {
            uringHolder.processCompletions(pendingCompletions, this);
        }
        return this;
    }

    @Override
    public void nop(final BiConsumer<Result<Unit>, Submitter> completion) {

        queue.add(factory.forNop(completion)
                         .register(pendingCompletions));
    }

    @Override
    public void delay(final BiConsumer<Result<Duration>, Submitter> completion,
                      final Timeout timeout) {

        queue.add(factory.forDelay(completion, timeout)
                         .register(pendingCompletions));
    }

    @Override
    public void closeFileDescriptor(final BiConsumer<Result<Unit>, Submitter> completion,
                                    final FileDescriptor fd,
                                    final Option<Timeout> timeout) {

        queue.add(factory.forClose(completion, fd, timeout)
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void read(final BiConsumer<Result<SizeT>, Submitter> completion,
                     final FileDescriptor fd,
                     final OffHeapBuffer buffer,
                     final OffsetT offset,
                     final Option<Timeout> timeout) {

        queue.add(factory.forRead(completion, fd, buffer, offset, timeout)
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void write(final BiConsumer<Result<SizeT>, Submitter> completion,
                      final FileDescriptor fd,
                      final OffHeapBuffer buffer,
                      final OffsetT offset,
                      final Option<Timeout> timeout) {

        if (buffer.used() == 0) {
            completion.accept(NativeFailureType.ENODATA.result(), this);
            return;
        }

        queue.add(factory.forWrite(completion, fd, buffer, offset, timeout)
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void splice(final BiConsumer<Result<SizeT>, Submitter> completion,
                       final SpliceDescriptor descriptor,
                       final Option<Timeout> timeout) {

        queue.add(factory.forSplice(completion, descriptor, timeout)
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void open(final BiConsumer<Result<FileDescriptor>, Submitter> completion,
                     final Path path,
                     final Set<OpenFlags> flags,
                     final Set<FilePermission> mode,
                     final Option<Timeout> timeout) {

        queue.add(factory.forOpen(completion, path, flags, mode, timeout)
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void socket(final BiConsumer<Result<FileDescriptor>, Submitter> completion,
                       final AddressFamily addressFamily,
                       final SocketType socketType,
                       final Set<SocketFlag> openFlags,
                       final Set<SocketOption> options) {
        queue.add(factory.forSocket(completion, addressFamily, socketType, openFlags, options)
                         .register(pendingCompletions));
    }

    @Override
    public void server(final BiConsumer<Result<ServerContext<?>>, Submitter> completion,
                       final SocketAddress<?> socketAddress,
                       final SocketType socketType,
                       final Set<SocketFlag> openFlags,
                       final SizeT queueDepth,
                       final Set<SocketOption> options) {

        queue.add(factory.forServer(completion, socketAddress, socketType, openFlags, queueDepth, options)
                         .register(pendingCompletions));
    }

    @Override
    public void accept(final BiConsumer<Result<ClientConnection<?>>, Submitter> completion,
                       final FileDescriptor socket,
                       final Set<SocketFlag> flags) {

        queue.add(factory.forAccept(completion, socket, flags)
                         .register(pendingCompletions));
    }

    @Override
    public void connect(final BiConsumer<Result<FileDescriptor>, Submitter> completion,
                        final FileDescriptor socket,
                        final SocketAddress<?> address,
                        final Option<Timeout> timeout) {
        final var clientAddress = OffHeapSocketAddress.unsafeSocketAddress(address);

        if (clientAddress == null) {
            completion.accept(NativeFailureType.EPFNOSUPPORT.result(), this);
            return;
        }

        queue.add(factory.forConnect(completion, socket, clientAddress, timeout)
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void stat(final BiConsumer<Result<FileStat>, Submitter> completion,
                     final Path path,
                     final Set<StatFlag> flags,
                     final Set<StatMask> mask,
                     final Option<Timeout> timeout) {

        //Reset EMPTY_PATH and force use the path.
        queue.add(factory.forStat(completion,
                                  AT_FDCWD,
                                  Bitmask.combine(flags) & ~StatFlag.EMPTY_PATH.mask(),
                                  Bitmask.combine(mask),
                                  OffHeapCString.cstring(path.toString()))
                         .register(pendingCompletions));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void stat(final BiConsumer<Result<FileStat>, Submitter> completion,
                     final FileDescriptor fd,
                     final Set<StatFlag> flags,
                     final Set<StatMask> mask,
                     final Option<Timeout> timeout) {

        //Set EMPTY_PATH and force use of file descriptor.
        queue.add(factory.forStat(completion,
                                  fd.descriptor(),
                                  Bitmask.combine(flags) | StatFlag.EMPTY_PATH.mask(),
                                  Bitmask.combine(mask),
                                  OffHeapCString.cstring(""))
                         .register(pendingCompletions));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void readVector(final BiConsumer<Result<SizeT>, Submitter> completion,
                           final FileDescriptor fileDescriptor,
                           final OffsetT offset,
                           final Option<Timeout> timeout,
                           final OffHeapBuffer... buffers) {

        queue.add(factory.forReadVector(completion, fileDescriptor, offset, timeout, OffHeapIoVector.withBuffers(buffers))
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void writeVector(final BiConsumer<Result<SizeT>, Submitter> completion,
                            final FileDescriptor fileDescriptor,
                            final OffsetT offset,
                            final Option<Timeout> timeout,
                            final OffHeapBuffer... buffers) {

        queue.add(factory.forWriteVector(completion, fileDescriptor, offset, timeout, OffHeapIoVector.withBuffers(buffers))
                         .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    private void appendTimeout(final Timeout timeout) {
        queue.add(factory.forTimeout(timeout)
                         .register(pendingCompletions));
    }
}
