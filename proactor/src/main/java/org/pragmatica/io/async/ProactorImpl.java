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
import org.pragmatica.io.async.uring.exchange.AsyncOperation;
import org.pragmatica.io.async.uring.exchange.ExchangeEntryPool;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.allocator.ChunkedAllocator;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.pragmatica.io.async.uring.exchange.AsyncOperation.*;
import static org.pragmatica.io.async.uring.exchange.ExchangeEntryPool.arrayPool;
import static org.pragmatica.io.async.uring.exchange.ExchangeEntryPool.hybridPool;
import static org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector.withReadBuffers;
import static org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector.withWriteBuffers;
import static org.pragmatica.io.async.uring.struct.raw.SQEntry.IORING_FSYNC_DATASYNC;

/**
 * Asynchronous Input/Output Proactor Implementation.
 */
class ProactorImpl implements Proactor {
    private static final Logger LOG = LoggerFactory.getLogger(Proactor.class);

    // Special value used to indicate the openat/statx functions should use the current working directory.
    private static final FileDescriptor AT_FDCWD = FileDescriptor.file(-100);
    private static final Timeout HEARTBEAT_INTERVAL = Timeout.timeout(1000).millis();

    private final UringApi uringApi;
    private final ExchangeEntryPool pool;
    private final ChunkedAllocator sharedAllocator;
    private final ExecutorService executor;
    private boolean shutdown = false;
    private final CountDownLatch shutdownLatch = new CountDownLatch(2);

    private ProactorImpl(UringApi uringApi, ChunkedAllocator sharedAllocator, ExchangeEntryPool pool, ThreadFactory factory) {
        this.uringApi = uringApi;
        this.pool = pool;

        this.sharedAllocator = sharedAllocator.register(uringApi);
        this.executor = Executors.newSingleThreadExecutor(factory);
        this.executor.submit(this::processIO);
    }

    private void heartbeat(Result<Duration> result, Proactor proactor) {
        proactor.delay(this::heartbeat, HEARTBEAT_INTERVAL);
    }

    static ProactorImpl proactor(int queueSize, Set<UringSetupFlags> openFlags, ChunkedAllocator sharedAllocator, ThreadFactory factory) {
        var pool = hybridPool();
        var api = UringApi.uringApi(queueSize, openFlags, pool)
                          .fold(ProactorImpl::fail, Functions::id);

        return new ProactorImpl(api, sharedAllocator, pool, factory);
    }

    private static <R> R fail(Cause cause) {
        throw new IllegalStateException("Unable to initialize IO_URING interface: " + cause.message());
    }

    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }

        shutdown = true;
        try {
            uringApi.shutdown();
            shutdownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            LOG.debug("Proactor shutdown completed");
        }
    }

    private void processIO() {
        while (!shutdown) {
            try {
                int count = uringApi.processSubmissions();
                count += uringApi.processCompletions(this);

                if (count == 0) {
                    Thread.yield();
//                    LockSupport.parkNanos(10);
                }

            } catch (Exception e) {
                LOG.debug("processCompletions caught (and ignored) exception: ", e);
            }
        }
        shutdownLatch.countDown();
    }

    @Override
    public void nop(BiConsumer<Result<Unit>, Proactor> completion) {
        uringApi.submit(pool.acquire(NOP)
                            .completion(completion));
    }

    @Override
    public void delay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout) {
        uringApi.submit(pool.acquire(TIMEOUT)
                            .completion(completion)
                            .setDelayTime(timeout));
    }

    @Override
    public void close(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(CLOSE)
                            .completion(completion)
                            .descriptor(fd)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(READ)
                            .completion(completion)
                            .descriptor(fd)
                            .buffer(buffer)
                            .offset(offset)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                      OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(WRITE)
                            .completion(completion)
                            .descriptor(fd)
                            .buffer(buffer)
                            .offset(offset)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void splice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(SPLICE)
                            .completion(completion)
                            .spliceDescriptor(descriptor)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void open(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> flags,
                     Set<FilePermission> mode, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(OPENAT)
                            .completion(completion)
                            .rawPath(OffHeapCString.cstring(path.toString()))
                            .openFlags(Bitmask.combine(flags))
                            .openMode(Bitmask.combine(mode))
                            .setOperationTimeout(timeout));
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T extends InetAddress> void accept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                               FileDescriptor socket, Set<SocketFlag> flags, T address) {
        uringApi.submit(pool.acquire((AsyncOperation<ConnectionContext<T>>) (AsyncOperation) ACCEPT)
                            .completion(completion)
                            .descriptor(socket)
                            .acceptFlags(Bitmask.combine(flags))
                            .protocolVersion(address.version()));
    }

    @Override
    public <T extends InetAddress> void connect(BiConsumer<Result<FileDescriptor>, Proactor> completion, FileDescriptor socket,
                                                SocketAddress<T> address, Option<Timeout> timeout) {
        var destinationAddress = OffHeapSocketAddress.unsafeSocketAddress(address);

        if (destinationAddress == null) {
            completion.accept(SystemError.EPFNOSUPPORT.result(), this);
            return;
        }

        uringApi.submit(pool.acquire(CONNECT)
                            .completion(completion)
                            .destinationAddress(destinationAddress)
                            .descriptor(socket)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion,
                     Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        //Reset EMPTY_PATH and force use the path.
        uringApi.submit(pool.acquire(STATX)
                            .completion(completion)
                            .descriptor(AT_FDCWD)
                            .statFlags(Bitmask.combine(flags) & ~StatFlag.EMPTY_PATH.mask())
                            .statMask(Bitmask.combine(mask))
                            .rawPath(OffHeapCString.cstring(path.toString())));
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion, FileDescriptor fileDescriptor, Set<StatFlag> flags,
                     Set<StatMask> mask, Option<Timeout> timeout) {
        //Set EMPTY_PATH and force use of file descriptor.
        uringApi.submit(pool.acquire(STATX)
                            .completion(completion)
                            .descriptor(fileDescriptor)
                            .statFlags(Bitmask.combine(flags) | StatFlag.EMPTY_PATH.mask())
                            .statMask(Bitmask.combine(mask))
                            .rawPath(OffHeapCString.cstring("")));

    }

    @Override
    public void readVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                           Option<Timeout> timeout, OffHeapSlice... buffers) {
        uringApi.submit(pool.acquire(READV)
                            .completion(completion)
                            .descriptor(fileDescriptor)
                            .offset(offset)
                            .ioVector(withReadBuffers(buffers))
                            .setOperationTimeout(timeout));
    }

    @Override
    public void writeVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                            Option<Timeout> timeout, OffHeapSlice... buffers) {
        uringApi.submit(pool.acquire(WRITEV)
                            .completion(completion)
                            .descriptor(fileDescriptor)
                            .offset(offset)
                            .ioVector(withWriteBuffers(buffers))
                            .setOperationTimeout(timeout));
    }

    @Override
    public void fileSync(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                         boolean syncMetadata, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(FSYNC)
                            .completion(completion)
                            .descriptor(fileDescriptor)
                            .syncFlags(syncMetadata ? 0 : IORING_FSYNC_DATASYNC)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void fileAlloc(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                          Set<FileAllocFlags> allocFlags, OffsetT offset, long len, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(FALLOCATE)
                            .completion(completion).descriptor(fileDescriptor)
                            .allocFlags(Bitmask.combine(allocFlags))
                            .offset(offset)
                            .len(len)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void readFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                          OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(READ_FIXED)
                            .completion(completion)
                            .descriptor(fd)
                            .fixedBuffer(buffer)
                            .offset(offset)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void writeFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                           OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(WRITE_FIXED)
                            .completion(completion)
                            .descriptor(fd)
                            .fixedBuffer(buffer)
                            .offset(offset)
                            .setOperationTimeout(timeout));
    }

    @Override
    public void send(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(SEND)
                            .completion(completion)
                            .buffer(buffer)
                            .descriptor(fd)
                            .msgFlags(Bitmask.combine(msgFlags))
                            .setOperationTimeout(timeout));
    }

    @Override
    public void recv(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        uringApi.submit(pool.acquire(RECV)
                            .completion(completion)
                            .buffer(buffer)
                            .descriptor(fd)
                            .msgFlags(Bitmask.combine(msgFlags))
                            .setOperationTimeout(timeout));
    }

    @Override
    public Result<FixedBuffer> allocateFixedBuffer(int size) {
        return sharedAllocator.allocate(size);
    }
}
