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
import org.pragmatica.io.async.net.ConnectionContext;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.MessageFlags;
import org.pragmatica.io.async.net.SocketFlag;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.SQEntryFlags;
import org.pragmatica.io.async.uring.utils.ExchangeEntryPool;
import org.pragmatica.io.async.uring.utils.ExchangeEntryRegistry;
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
import static org.pragmatica.io.async.uring.utils.ExchangeEntryPool.exchangeEntryPool;
import static org.pragmatica.io.async.uring.utils.ExchangeEntryRegistry.exchangeEntryRegistry;

/**
 * Factory for the different types of exchange entries.
 * <p>
 * This implementation uses pools of the instances for each type of exchange entry. This approach eliminates pressure on GC when huge amount of
 * requests are issued.
 */
public class ExchangeEntryFactory {
    @SuppressWarnings({"rawtypes"})
    private final ExchangeEntryPool acceptPool;
    private final ExchangeEntryPool<CloseExchangeEntry> closePool;
    private final ExchangeEntryPool<ConnectExchangeEntry> connectPool;
    private final ExchangeEntryPool<DelayExchangeEntry> delayPool;
    private final ExchangeEntryPool<FAllocExchangeEntry> fallocPool;
    private final ExchangeEntryPool<FSyncExchangeEntry> fsyncPool;
    private final ExchangeEntryPool<NopExchangeEntry> nopPool;
    private final ExchangeEntryPool<OpenExchangeEntry> openPool;
    private final ExchangeEntryPool<ReadExchangeEntry> readPool;
    private final ExchangeEntryPool<ReadFixedExchangeEntry> readFixedPool;
    private final ExchangeEntryPool<ReadVectorExchangeEntry> readVectorPool;
    private final ExchangeEntryPool<RecvExchangeEntry> recvPool;
    private final ExchangeEntryPool<SendExchangeEntry> sendPool;
    private final ExchangeEntryPool<SpliceExchangeEntry> splicePool;
    private final ExchangeEntryPool<StatExchangeEntry> statPool;
    private final ExchangeEntryPool<TimeoutExchangeEntry> timeoutPool;
    private final ExchangeEntryPool<WriteExchangeEntry> writePool;
    private final ExchangeEntryPool<WriteFixedExchangeEntry> writeFixedPool;
    private final ExchangeEntryPool<WriteVectorExchangeEntry> writeVectorPool;
    @SuppressWarnings({"rawtypes"})
    private final List<ExchangeEntryPool> pools;
    private final ExchangeEntryRegistry registry = exchangeEntryRegistry();

    private ExchangeEntryFactory() {
        acceptPool = exchangeEntryPool(AcceptExchangeEntry::new, registry);
        closePool = exchangeEntryPool(CloseExchangeEntry::new, registry);
        connectPool = exchangeEntryPool(ConnectExchangeEntry::new, registry);
        delayPool = exchangeEntryPool(DelayExchangeEntry::new, registry);
        fallocPool = exchangeEntryPool(FAllocExchangeEntry::new, registry);
        fsyncPool = exchangeEntryPool(FSyncExchangeEntry::new, registry);
        nopPool = exchangeEntryPool(NopExchangeEntry::new, registry);
        openPool = exchangeEntryPool(OpenExchangeEntry::new, registry);
        readPool = exchangeEntryPool(ReadExchangeEntry::new, registry);
        readFixedPool = exchangeEntryPool(ReadFixedExchangeEntry::new, registry);
        readVectorPool = exchangeEntryPool(ReadVectorExchangeEntry::new, registry);
        recvPool = exchangeEntryPool(RecvExchangeEntry::new, registry);
        sendPool = exchangeEntryPool(SendExchangeEntry::new, registry);
        splicePool = exchangeEntryPool(SpliceExchangeEntry::new, registry);
        statPool = exchangeEntryPool(StatExchangeEntry::new, registry);
        timeoutPool = exchangeEntryPool(TimeoutExchangeEntry::new, registry);
        writePool = exchangeEntryPool(WriteExchangeEntry::new, registry);
        writeFixedPool = exchangeEntryPool(WriteFixedExchangeEntry::new, registry);
        writeVectorPool = exchangeEntryPool(WriteVectorExchangeEntry::new, registry);

        pools = List.of(acceptPool, closePool, connectPool, delayPool, fallocPool, fsyncPool, nopPool, openPool,
                        readPool, readFixedPool, readVectorPool, splicePool, statPool, timeoutPool, writePool,
                        writeFixedPool, writeVectorPool);
    }

    public static ExchangeEntryFactory exchangeEntryFactory() {
        return new ExchangeEntryFactory();
    }

    public ExchangeEntryRegistry registry() {
        return registry;
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

    @SuppressWarnings("unchecked")
    public <T extends InetAddress> AcceptExchangeEntry<T> forAccept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                                                    FileDescriptor socket, Set<SocketFlag> flags, boolean v6) {
        return ((AcceptExchangeEntry<T>) acceptPool.alloc())
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
        pools.forEach(ExchangeEntryPool::clear);
    }
}
