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

package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.*;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.SQEntryFlags;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.struct.raw.SQEntry.IORING_FSYNC_DATASYNC;
import static org.pragmatica.io.async.uring.utils.PlainObjectPool.objectPool;

/**
 * Factory for the different types of exchange entries.
 * <p>
 * This implementation uses pools of the instances for each type of exchange entry. This approach eliminates pressure on GC when huge amount of
 * requests are issued.
 */
public class ExchangeEntryFactory {
    @SuppressWarnings({"rawtypes"})
    private final PlainObjectPool<AcceptExchangeEntry> acceptPool;
    private final PlainObjectPool<CloseExchangeEntry> closePool;
    private final PlainObjectPool<ConnectExchangeEntry> connectPool;
    private final PlainObjectPool<DelayExchangeEntry> delayPool;
    private final PlainObjectPool<FAllocExchangeEntry> fallocPool;
    private final PlainObjectPool<FSyncExchangeEntry> fsyncPool;
    @SuppressWarnings({"rawtypes"})
    private final PlainObjectPool<ListenExchangeEntry> listenPool;
    private final PlainObjectPool<NopExchangeEntry> nopPool;
    private final PlainObjectPool<OpenExchangeEntry> openPool;
    private final PlainObjectPool<ReadExchangeEntry> readPool;
    private final PlainObjectPool<ReadFixedExchangeEntry> readFixedPool;
    private final PlainObjectPool<ReadVectorExchangeEntry> readVectorPool;
    private final PlainObjectPool<RecvExchangeEntry> recvPool;
    private final PlainObjectPool<SendExchangeEntry> sendPool;
    private final PlainObjectPool<SocketExchangeEntry> socketPool;
    private final PlainObjectPool<SpliceExchangeEntry> splicePool;
    private final PlainObjectPool<StatExchangeEntry> statPool;
    private final PlainObjectPool<TimeoutExchangeEntry> timeoutPool;
    private final PlainObjectPool<WriteExchangeEntry> writePool;
    private final PlainObjectPool<WriteFixedExchangeEntry> writeFixedPool;
    private final PlainObjectPool<WriteVectorExchangeEntry> writeVectorPool;
    private final List<PlainObjectPool<?>> pools;

    public ExchangeEntryFactory(ObjectHeap<CompletionHandler> exchangeRegistry) {
        acceptPool = objectPool(AcceptExchangeEntry::new, exchangeRegistry);
        closePool = objectPool(CloseExchangeEntry::new, exchangeRegistry);
        connectPool = objectPool(ConnectExchangeEntry::new, exchangeRegistry);
        delayPool = objectPool(DelayExchangeEntry::new, exchangeRegistry);
        fallocPool = objectPool(FAllocExchangeEntry::new, exchangeRegistry);
        fsyncPool = objectPool(FSyncExchangeEntry::new, exchangeRegistry);
        listenPool = objectPool(ListenExchangeEntry::new, exchangeRegistry);
        nopPool = objectPool(NopExchangeEntry::new, exchangeRegistry);
        openPool = objectPool(OpenExchangeEntry::new, exchangeRegistry);
        readPool = objectPool(ReadExchangeEntry::new, exchangeRegistry);
        readFixedPool = objectPool(ReadFixedExchangeEntry::new, exchangeRegistry);
        readVectorPool = objectPool(ReadVectorExchangeEntry::new, exchangeRegistry);
        recvPool = objectPool(RecvExchangeEntry::new, exchangeRegistry);
        sendPool = objectPool(SendExchangeEntry::new, exchangeRegistry);
        socketPool = objectPool(SocketExchangeEntry::new, exchangeRegistry);
        splicePool = objectPool(SpliceExchangeEntry::new, exchangeRegistry);
        statPool = objectPool(StatExchangeEntry::new, exchangeRegistry);
        timeoutPool = objectPool(TimeoutExchangeEntry::new, exchangeRegistry);
        writePool = objectPool(WriteExchangeEntry::new, exchangeRegistry);
        writeFixedPool = objectPool(WriteFixedExchangeEntry::new, exchangeRegistry);
        writeVectorPool = objectPool(WriteVectorExchangeEntry::new, exchangeRegistry);

        pools = List.of(acceptPool, closePool, connectPool, delayPool, fallocPool, fsyncPool, listenPool,
                        nopPool, openPool, readPool, readFixedPool, readVectorPool, socketPool, splicePool,
                        statPool, timeoutPool, writePool, writeFixedPool, writeVectorPool);
    }

    public NopExchangeEntry forNop(BiConsumer<Result<Unit>, Proactor> completion) {
        return nopPool.alloc()
                      .prepare(completion);
    }

    public TimeoutExchangeEntry forTimeout(Timeout timeout) {
        return timeoutPool.alloc()
                          .prepare(timeout);
    }

    public DelayExchangeEntry forDelay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout) {
        return delayPool.alloc()
                        .prepare(completion, timeout);
    }

    public CloseExchangeEntry forClose(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout) {
        return closePool.alloc()
                        .prepare(completion, fd.descriptor(), calculateFlags(timeout));
    }

    private byte calculateFlags(Option<Timeout> timeout) {
        return timeout.equals(Option.empty()) ? 0 : (byte) SQEntryFlags.IO_LINK.mask();
    }

    public ReadExchangeEntry forRead(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                                     OffsetT offset, Option<Timeout> timeout) {
        return readPool.alloc()
                       .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public WriteExchangeEntry forWrite(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                                       OffsetT offset, Option<Timeout> timeout) {
        return writePool.alloc()
                        .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public SpliceExchangeEntry forSplice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        return splicePool.alloc()
                         .prepare(completion, descriptor, calculateFlags(timeout));
    }

    public OpenExchangeEntry forOpen(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> openFlags,
                                     Set<FilePermission> mode, Option<Timeout> timeout) {
        return openPool.alloc()
                       .prepare(completion, path, Bitmask.combine(openFlags), Bitmask.combine(mode), calculateFlags(timeout));
    }

    public SocketExchangeEntry forSocket(BiConsumer<Result<FileDescriptor>, Proactor> completion, AddressFamily addressFamily,
                                         SocketType socketType, Set<SocketFlag> openFlags, Set<SocketOption> options) {
        return socketPool.alloc()
                         .prepare(completion, addressFamily, socketType, openFlags, options);
    }

    @SuppressWarnings("unchecked")
    public <T extends InetAddress> ListenExchangeEntry<T> forListen(BiConsumer<Result<ListenContext<T>>, Proactor> completion,
                                                                    SocketAddress<T> socketAddress, SocketType socketType,
                                                                    Set<SocketFlag> openFlags, SizeT queueDepth, Set<SocketOption> options) {
        return listenPool.alloc()
                         .prepare(completion, socketAddress, socketType, openFlags, queueDepth, options);
    }

    @SuppressWarnings("unchecked")
    public <T extends InetAddress> AcceptExchangeEntry<T> forAccept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                                                    FileDescriptor socket, Set<SocketFlag> flags, boolean v6) {
        return acceptPool.alloc()
                         .prepare(completion, socket.descriptor(), Bitmask.combine(flags), v6);
    }

    public ConnectExchangeEntry forConnect(BiConsumer<Result<FileDescriptor>, Proactor> completion,
                                           FileDescriptor socket, OffHeapSocketAddress clientAddress,
                                           Option<Timeout> timeout) {
        return connectPool.alloc()
                          .prepare(completion, socket, clientAddress, calculateFlags(timeout));
    }

    public StatExchangeEntry forStat(BiConsumer<Result<FileStat>, Proactor> completion,
                                     int descriptor, int statFlags, int statMask, OffHeapCString rawPath) {
        return statPool.alloc()
                       .prepare(completion, descriptor, statFlags, statMask, rawPath);
    }

    public ReadVectorExchangeEntry forReadVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                                                 OffsetT offset, Option<Timeout> timeout, OffHeapIoVector ioVector) {
        return readVectorPool.alloc()
                             .prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), ioVector);
    }

    public WriteVectorExchangeEntry forWriteVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                                                   OffsetT offset, Option<Timeout> timeout, OffHeapIoVector ioVector) {
        return writeVectorPool.alloc()
                              .prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), ioVector);
    }

    public FSyncExchangeEntry forFSync(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                                       boolean syncMetadata, Option<Timeout> timeout) {
        return fsyncPool.alloc()
                        .prepare(completion, fileDescriptor.descriptor(), syncMetadata ? 0 : IORING_FSYNC_DATASYNC, calculateFlags(timeout));
    }

    public FAllocExchangeEntry forFAlloc(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                                         Set<FileAllocFlags> allocFlags, long offset, long len, Option<Timeout> timeout) {
        return fallocPool.alloc()
                         .prepare(completion, fileDescriptor.descriptor(), Bitmask.combine(allocFlags), offset, len, calculateFlags(timeout));
    }

    public ReadFixedExchangeEntry forReadFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                                               OffsetT offset, Option<Timeout> timeout) {
        return readFixedPool.alloc()
                            .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public WriteFixedExchangeEntry forWriteFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer,
                                                 OffsetT offset, Option<Timeout> timeout) {
        return writeFixedPool.alloc()
                             .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public RecvExchangeEntry forRecv(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        return recvPool.alloc()
                       .prepare(completion, fd.descriptor(), buffer, Bitmask.combine(msgFlags), calculateFlags(timeout));
    }

    public SendExchangeEntry forSend(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer,
                                     Set<MessageFlags> msgFlags, Option<Timeout> timeout) {
        return sendPool.alloc()
                       .prepare(completion, fd.descriptor(), buffer, Bitmask.combine(msgFlags), calculateFlags(timeout));
    }

    public void clear() {
        pools.forEach(PlainObjectPool::clear);
    }
}
