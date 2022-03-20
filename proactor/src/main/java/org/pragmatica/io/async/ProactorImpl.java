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

package org.pragmatica.io.async;

import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.*;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.file.stat.StatFlag;
import org.pragmatica.io.async.file.stat.StatMask;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.UringApi;
import org.pragmatica.io.async.uring.UringSetupFlags;
import org.pragmatica.io.async.uring.exchange.ExchangeEntryFactory;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.allocator.ChunkedAllocator;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.lang.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector.withReadBuffers;
import static org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector.withWriteBuffers;

/**
 * Asynchronous Input/Output Proactor Implementation.
 * <pre>
 * WARNING!
 * The implementation intentionally (for performance reasons) does not use any means of synchronization of the access, i.e. it is not thread safe.
 * </pre>
 */
class ProactorImpl implements Proactor {
    private static final int AT_FDCWD = -100; // Special value used to indicate the openat/statx functions should use the current working directory.

    private final UringApi uringApi;
    private final ObjectHeap<CompletionHandler> exchangeRegistry;
    private final ChunkedAllocator sharedAllocator;
    private final ExchangeEntryFactory factory;

    private ProactorImpl(UringApi uringApi, ChunkedAllocator sharedAllocator) {
        this.uringApi = uringApi;
        this.exchangeRegistry = ObjectHeap.objectHeap(uringApi.numEntries());
        this.factory = new ExchangeEntryFactory(exchangeRegistry);
        this.sharedAllocator = sharedAllocator.register(uringApi);
    }

    public static Proactor proactor(int queueSize, Set<UringSetupFlags> openFlags, ChunkedAllocator sharedAllocator, Option<Proactor> rootProactor) {
        var api = rootProactor.map(ProactorImpl.class::cast)
                              .fold(() -> UringApi.uringApi(queueSize, openFlags),
                                    root -> UringApi.uringApi(queueSize, openFlags, root.uringApi.fd()))
                              .fold(ProactorImpl::fail, Functions::id);

        return new ProactorImpl(api, sharedAllocator);
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
    public void processSubmissions() {
        uringApi.processSubmissions();
    }

    @Override
    public int processCompletions() {
        return uringApi.processCompletions(exchangeRegistry, this);
    }

    @Override
    public void nop(BiConsumer<Result<Unit>, Proactor> completion) {
        uringApi.submit(factory.forNop(completion));
    }

    @Override
    public void delay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout) {
        uringApi.submit(factory.forDelay(completion, timeout));
    }

    @Override
    public void close(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout) {
        uringApi.submit(factory.forClose(completion, fd, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(factory.forRead(completion, fd, buffer, offset, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                      OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(factory.forWrite(completion, fd, buffer, offset, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void splice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        uringApi.submit(factory.forSplice(completion, descriptor, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void open(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> flags,
                     Set<FilePermission> mode, Option<Timeout> timeout) {
        uringApi.submit(factory.forOpen(completion, path, flags, mode, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void socket(BiConsumer<Result<FileDescriptor>, Proactor> completion, AddressFamily addressFamily,
                       SocketType socketType, Set<SocketFlag> openFlags, Set<SocketOption> options) {
        uringApi.submit(factory.forSocket(completion, addressFamily, socketType, openFlags, options));
    }

    @Override
    public <T extends InetAddress> void listen(BiConsumer<Result<ListenContext<T>>, Proactor> completion,
                                               SocketAddress<T> socketAddress, SocketType socketType,
                                               Set<SocketFlag> openFlags, SizeT queueDepth, Set<SocketOption> options) {
        uringApi.submit(factory.forListen(completion, socketAddress, socketType, openFlags, queueDepth, options));
    }

    @Override
    public <T extends InetAddress> void accept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                               FileDescriptor socket, Set<SocketFlag> flags, T addressType) {
        uringApi.submit(factory.forAccept(completion, socket, flags, addressType instanceof InetAddress.Inet6Address));
    }

    @Override
    public <T extends InetAddress> void connect(BiConsumer<Result<FileDescriptor>, Proactor> completion, FileDescriptor socket,
                                                SocketAddress<T> address, Option<Timeout> timeout) {
        var clientAddress = OffHeapSocketAddress.unsafeSocketAddress(address);

        if (clientAddress == null) {
            completion.accept(SystemError.EPFNOSUPPORT.result(), this);
            return;
        }

        uringApi.submit(factory.forConnect(completion, socket, clientAddress, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion,
                     Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        //Reset EMPTY_PATH and force use the path.
        uringApi.submit(factory.forStat(completion,
                                        AT_FDCWD,
                                        Bitmask.combine(flags) & ~StatFlag.EMPTY_PATH.mask(),
                                        Bitmask.combine(mask),
                                        OffHeapCString.cstring(path.toString())));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion, FileDescriptor fd, Set<StatFlag> flags,
                     Set<StatMask> mask, Option<Timeout> timeout) {
        //Set EMPTY_PATH and force use of file descriptor.
        uringApi.submit(factory.forStat(completion,
                                        fd.descriptor(),
                                        Bitmask.combine(flags) | StatFlag.EMPTY_PATH.mask(),
                                        Bitmask.combine(mask),
                                        OffHeapCString.cstring("")));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void readVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                           Option<Timeout> timeout, OffHeapSlice... buffers) {
        uringApi.submit(factory.forReadVector(completion, fileDescriptor, offset, timeout, withReadBuffers(buffers))
                               .register(exchangeRegistry));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void writeVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                            Option<Timeout> timeout, OffHeapSlice... buffers) {
        uringApi.submit(factory.forWriteVector(completion, fileDescriptor, offset, timeout, withWriteBuffers(buffers)));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void fsync(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                      boolean syncMetadata, Option<Timeout> timeout) {
        uringApi.submit(factory.forFSync(completion, fileDescriptor, syncMetadata, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void falloc(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                       Set<FileAllocFlags> allocFlags, long offset, long len, Option<Timeout> timeout) {
        uringApi.submit(factory.forFAlloc(completion, fileDescriptor, allocFlags, offset, len, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void readFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                          OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(factory.forReadFixed(completion, fd, buffer, offset, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void writeFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                           OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(factory.forWriteFixed(completion, fd, buffer, offset, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void send(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        uringApi.submit(factory.forSend(completion, fd, buffer, msgFlags, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void recv(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        uringApi.submit(factory.forRecv(completion, fd, buffer, msgFlags, timeout));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public Result<FixedBuffer> allocateFixedBuffer(int size) {
        return sharedAllocator.allocate(size);
    }

    private void appendTimeout(Timeout timeout) {
        uringApi.submit(factory.forTimeout(timeout));
    }
}
