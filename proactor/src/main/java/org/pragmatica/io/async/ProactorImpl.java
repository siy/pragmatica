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

package org.pragmatica.io.async;

import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.*;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.file.stat.StatFlag;
import org.pragmatica.io.async.file.stat.StatMask;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.UringApi;
import org.pragmatica.io.async.uring.UringSetupFlags;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.util.DaemonThreadFactory;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.allocator.ChunkedAllocator;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.lang.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector.withReadBuffers;
import static org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector.withWriteBuffers;

/**
 * Asynchronous Input/Output Proactor Implementation.
 */
class ProactorImpl implements Proactor {
    private static final int AT_FDCWD = -100; // Special value used to indicate the openat/statx functions should use the current working directory.
    private static final Timeout HEARTBEAT_INTERVAL = Timeout.timeout(1).millis();

    private final UringApi uringApi;
    private final ChunkedAllocator sharedAllocator;
    private final ExecutorService executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private ProactorImpl(UringApi uringApi, ChunkedAllocator sharedAllocator) {
        this.uringApi = uringApi;

        uringApi.submit(uringApi.factory().forDelay(this::heartbeat, HEARTBEAT_INTERVAL));

        this.sharedAllocator = sharedAllocator.register(uringApi);
        this.executor = Executors.newSingleThreadExecutor(DaemonThreadFactory.threadFactory("Proactor Worker %d"));
        this.executor.submit(this::processCompletions);
    }

    private void heartbeat(Result<Duration> result, Proactor proactor) {
        proactor.delay(this::heartbeat, HEARTBEAT_INTERVAL);
    }

    static Proactor proactor(int queueSize, Set<UringSetupFlags> openFlags, ChunkedAllocator sharedAllocator) {
        var api = UringApi.uringApi(queueSize, openFlags)
                          .fold(ProactorImpl::fail, Functions::id);

        return new ProactorImpl(api, sharedAllocator);
    }

    private static <R> R fail(Result.Cause cause) {
        throw new IllegalStateException("Unable to initialize IO_URING interface: " + cause.message());
    }

    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            try {
                uringApi.shutdown();
                shutdownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        }
    }

    private void processCompletions() {
        while (!shutdown.get()) {
            uringApi.processCompletions(this);
//            uringApi.waitForCompletions(1);
        }
        shutdownLatch.countDown();
    }

    @Override
    public void nop(BiConsumer<Result<Unit>, Proactor> completion) {
        uringApi.submit(uringApi.factory().forNop(completion));
    }

    @Override
    public void delay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout) {
        uringApi.submit(uringApi.factory().forDelay(completion, timeout));
    }

    @Override
    public void close(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forClose(completion, fd, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forRead(completion, fd, buffer, offset, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                      OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forWrite(completion, fd, buffer, offset, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void splice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forSplice(completion, descriptor, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void open(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> flags,
                     Set<FilePermission> mode, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forOpen(completion, path, flags, mode, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void socket(Consumer<Result<FileDescriptor>> completion, AddressFamily addressFamily,
                       SocketType socketType, Set<SocketFlag> openFlags, Set<SocketOption> options) {
        completion.accept(UringApi.socket(addressFamily, socketType, openFlags, options));
    }

    @Override
    public <T extends InetAddress> void listen(Consumer<Result<ListenContext<T>>> completion,
                                               SocketAddress<T> socketAddress, SocketType socketType,
                                               Set<SocketFlag> openFlags, SizeT queueDepth, Set<SocketOption> options) {
        completion.accept(UringApi.listen(socketAddress, socketType, openFlags, options, queueDepth));
    }

    @Override
    public <T extends InetAddress> void accept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                               FileDescriptor socket, Set<SocketFlag> flags, T addressType) {
        uringApi.submit(uringApi.factory().forAccept(completion, socket, flags, addressType instanceof InetAddress.Inet6Address));
    }

    @Override
    public <T extends InetAddress> void connect(BiConsumer<Result<FileDescriptor>, Proactor> completion, FileDescriptor socket,
                                                SocketAddress<T> address, Option<Timeout> timeout) {
        var clientAddress = OffHeapSocketAddress.unsafeSocketAddress(address);

        if (clientAddress == null) {
            completion.accept(SystemError.EPFNOSUPPORT.result(), this);
            return;
        }

        uringApi.submit(uringApi.factory().forConnect(completion, socket, clientAddress, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion,
                     Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        //Reset EMPTY_PATH and force use the path.
        uringApi.submit(uringApi.factory().forStat(completion,
                                                   AT_FDCWD,
                                        Bitmask.combine(flags) & ~StatFlag.EMPTY_PATH.mask(),
                                                   Bitmask.combine(mask),
                                                   OffHeapCString.cstring(path.toString())));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion, FileDescriptor fd, Set<StatFlag> flags,
                     Set<StatMask> mask, Option<Timeout> timeout) {
        //Set EMPTY_PATH and force use of file descriptor.
        uringApi.submit(uringApi.factory().forStat(completion,
                                                   fd.descriptor(),
                                        Bitmask.combine(flags) | StatFlag.EMPTY_PATH.mask(),
                                                   Bitmask.combine(mask),
                                                   OffHeapCString.cstring("")));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void readVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                           Option<Timeout> timeout, OffHeapSlice... buffers) {
        uringApi.submit(uringApi.factory().forReadVector(completion, fileDescriptor, offset, timeout, withReadBuffers(buffers)));

        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void writeVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                            Option<Timeout> timeout, OffHeapSlice... buffers) {
        uringApi.submit(uringApi.factory().forWriteVector(completion, fileDescriptor, offset, timeout, withWriteBuffers(buffers)));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void fsync(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                      boolean syncMetadata, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forFSync(completion, fileDescriptor, syncMetadata, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void falloc(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                       Set<FileAllocFlags> allocFlags, long offset, long len, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forFAlloc(completion, fileDescriptor, allocFlags, offset, len, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void readFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                          OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forReadFixed(completion, fd, buffer, offset, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void writeFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                           OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forWriteFixed(completion, fd, buffer, offset, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void send(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forSend(completion, fd, buffer, msgFlags, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public void recv(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        uringApi.submit(uringApi.factory().forRecv(completion, fd, buffer, msgFlags, timeout));
        timeout.onPresent(this::appendTimeout);
    }

    @Override
    public Result<FixedBuffer> allocateFixedBuffer(int size) {
        return sharedAllocator.allocate(size);
    }

    private void appendTimeout(Timeout timeout) {
        uringApi.submit(uringApi.factory().forTimeout(timeout));
    }
}
