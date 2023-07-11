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

package org.pragmatica.io.async;

import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.OpenFlags;
import org.pragmatica.io.async.file.SpliceDescriptor;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.file.stat.StatFlag;
import org.pragmatica.io.async.file.stat.StatMask;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.net.InetAddress.Inet6Address;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.UringApi;
import org.pragmatica.io.async.uring.UringSetupFlags;
import org.pragmatica.io.async.uring.exchange.ExchangeEntry;
import org.pragmatica.io.async.uring.exchange.ExchangeEntryFactory;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.lang.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.BiConsumer;
import org.pragmatica.lang.Result.Cause;

import static org.pragmatica.io.async.uring.UringApi.uringApi;

/**
 * Asynchronous Input/Output Proactor Implementation.
 * <pre>
 * WARNING!
 * The implementation is fairly low level and designed for single thread use.
 * </pre>
 */
class ProactorImpl implements Proactor {
    private static final int AT_FDCWD = -100; // Special value used to indicate the openat/statx functions should use the current working directory.

    private final UringApi uringApi;
    private final ObjectHeap<CompletionHandler> pendingCompletions;
    private final Deque<ExchangeEntry<?>> queue = new ArrayDeque<>();
    private final ExchangeEntryFactory factory = new ExchangeEntryFactory();

    private ProactorImpl(UringApi uringApi) {
        this.uringApi = uringApi;
        pendingCompletions = ObjectHeap.objectHeap(uringApi.numEntries());
    }

    public static Proactor proactor(int queueSize) {
        return proactor(queueSize, UringSetupFlags.defaultFlags());
    }

    public static Proactor proactor(int queueSize, Set<UringSetupFlags> openFlags) {
        return new ProactorImpl(uringApi(queueSize, openFlags).fold(ProactorImpl::fail, Functions::id));
    }

    private static <R> R fail(Cause cause) {
        throw new IllegalStateException("Unable to initialize IO_URING interface: " + cause.message());
    }

    @Override
    public void shutdown() {
        uringApi.close();
        factory.clear();
    }

    @Override
    public int processIO() {
        if (!queue.isEmpty()) {
            uringApi.processSubmissions(queue);
        }

        if (pendingCompletions.count() > 0) {
            uringApi.processCompletions(pendingCompletions, this);
        }

        return pendingCompletions.count();
    }

    @Override
    public void nop(BiConsumer<Result<Unit>, Proactor> completion) {
        queue.add(factory.forNop(completion)
                         .register(pendingCompletions));
    }

    @Override
    public void delay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout) {
        queue.add(factory.forDelay(completion, timeout)
                         .register(pendingCompletions));
    }

    @Override
    public void close(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout) {
        queue.add(factory.forClose(completion, fd, timeout)
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapBuffer buffer,
                     OffsetT offset, Option<Timeout> timeout) {
        queue.add(factory.forRead(completion, fd, buffer, offset, timeout)
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapBuffer buffer,
                      OffsetT offset, Option<Timeout> timeout) {
        if (buffer.used() == 0) {
            completion.accept(SystemError.ENODATA.result(), this);
            return;
        }

        queue.add(factory.forWrite(completion, fd, buffer, offset, timeout)
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void splice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        queue.add(factory.forSplice(completion, descriptor, timeout)
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void open(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> flags,
                     Set<FilePermission> mode, Option<Timeout> timeout) {
        queue.add(factory.forOpen(completion, path, flags, mode, timeout)
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void socket(BiConsumer<Result<FileDescriptor>, Proactor> completion, AddressFamily addressFamily,
                       SocketType socketType, Set<SocketFlag> openFlags, Set<SocketOption> options) {
        queue.add(factory.forSocket(completion, addressFamily, socketType, openFlags, options)
                         .register(pendingCompletions));
    }

    @Override
    public <T extends InetAddress> void listen(BiConsumer<Result<ListenContext<T>>, Proactor> completion,
                                               SocketAddress<T> socketAddress, SocketType socketType,
                                               Set<SocketFlag> openFlags, SizeT queueDepth, Set<SocketOption> options) {
        queue.add(factory.forListen(completion, socketAddress, socketType, openFlags, queueDepth, options)
                         .register(pendingCompletions));
    }

    @Override
    public <T extends InetAddress> void accept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                               FileDescriptor socket, Set<SocketFlag> flags, T addressType) {
        queue.add(factory.forAccept(completion, socket, flags, addressType instanceof Inet6Address)
                         .register(pendingCompletions));
    }

    @Override
    public <T extends InetAddress> void connect(BiConsumer<Result<FileDescriptor>, Proactor> completion, FileDescriptor socket,
                                                SocketAddress<T> address, Option<Timeout> timeout) {
        var clientAddress = OffHeapSocketAddress.unsafeSocketAddress(address);

        if (clientAddress == null) {
            completion.accept(SystemError.EPFNOSUPPORT.result(), this);
            return;
        }

        queue.add(factory.forConnect(completion, socket, clientAddress, timeout)
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion,
                     Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        //Reset EMPTY_PATH and force use the path.
        queue.add(factory.forStat(completion,
                                  AT_FDCWD,
                                  Bitmask.combine(flags) & ~StatFlag.EMPTY_PATH.mask(),
                                  Bitmask.combine(mask),
                                  OffHeapCString.cstring(path.toString()))
                         .register(pendingCompletions));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion, FileDescriptor fd, Set<StatFlag> flags,
                     Set<StatMask> mask, Option<Timeout> timeout) {
        //Set EMPTY_PATH and force use of file descriptor.
        queue.add(factory.forStat(completion,
                                  fd.descriptor(),
                                  Bitmask.combine(flags) | StatFlag.EMPTY_PATH.mask(),
                                  Bitmask.combine(mask),
                                  OffHeapCString.cstring(""))
                         .register(pendingCompletions));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                     Option<Timeout> timeout, OffHeapBuffer... buffers) {
        queue.add(factory.forReadVector(completion, fileDescriptor, offset, timeout, OffHeapIoVector.withBuffers(buffers))
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                      Option<Timeout> timeout, OffHeapBuffer... buffers) {
        queue.add(factory.forWriteVector(completion, fileDescriptor, offset, timeout, OffHeapIoVector.withBuffers(buffers))
                         .register(pendingCompletions));

        timeout.onPresent(this::appendTimeout);
    }

    private void appendTimeout(Timeout timeout) {
        queue.add(factory.forTimeout(timeout)
                         .register(pendingCompletions));
    }
}
