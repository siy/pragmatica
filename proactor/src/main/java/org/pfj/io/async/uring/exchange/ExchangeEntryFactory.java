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

package org.pfj.io.async.uring.exchange;

import org.pfj.io.async.Proactor;
import org.pfj.io.async.Timeout;
import org.pfj.io.async.common.OffsetT;
import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.file.FilePermission;
import org.pfj.io.async.file.OpenFlags;
import org.pfj.io.async.file.SpliceDescriptor;
import org.pfj.io.async.file.stat.FileStat;
import org.pfj.io.async.net.*;
import org.pfj.io.async.uring.Bitmask;
import org.pfj.io.async.uring.struct.ExternalRawStructure;
import org.pfj.io.async.uring.struct.offheap.OffHeapCString;
import org.pfj.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pfj.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pfj.io.async.uring.struct.raw.SubmitQueueEntryFlags;
import org.pfj.io.async.uring.utils.PlainObjectPool;
import org.pfj.io.async.util.OffHeapBuffer;
import org.pfj.lang.Option;
import org.pfj.lang.Result;
import org.pfj.lang.Unit;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.function.BiConsumer;

public class ExchangeEntryFactory {
    private final PlainObjectPool<NopExchangeEntry> nopPool = new PlainObjectPool<>(NopExchangeEntry::new);
    private final PlainObjectPool<DelayExchangeEntry> delayPool = new PlainObjectPool<>(DelayExchangeEntry::new);
    private final PlainObjectPool<CloseExchangeEntry> closePool = new PlainObjectPool<>(CloseExchangeEntry::new);
    private final PlainObjectPool<TimeoutExchangeEntry> timeoutPool = new PlainObjectPool<>(TimeoutExchangeEntry::new);
    private final PlainObjectPool<ReadExchangeEntry> readPool = new PlainObjectPool<>(ReadExchangeEntry::new);
    private final PlainObjectPool<WriteExchangeEntry> writePool = new PlainObjectPool<>(WriteExchangeEntry::new);
    private final PlainObjectPool<SpliceExchangeEntry> splicePool = new PlainObjectPool<>(SpliceExchangeEntry::new);
    private final PlainObjectPool<OpenExchangeEntry> openPool = new PlainObjectPool<>(OpenExchangeEntry::new);
    private final PlainObjectPool<SocketExchangeEntry> socketPool = new PlainObjectPool<>(SocketExchangeEntry::new);
    private final PlainObjectPool<ServerExchangeEntry> serverPool = new PlainObjectPool<>(ServerExchangeEntry::new);
    private final PlainObjectPool<AcceptExchangeEntry> acceptPool = new PlainObjectPool<>(AcceptExchangeEntry::new);
    private final PlainObjectPool<ConnectExchangeEntry> connectPool = new PlainObjectPool<>(ConnectExchangeEntry::new);
    private final PlainObjectPool<StatExchangeEntry> statPool = new PlainObjectPool<>(StatExchangeEntry::new);
    private final PlainObjectPool<ReadVectorExchangeEntry> readVectorPool = new PlainObjectPool<>(ReadVectorExchangeEntry::new);
    private final PlainObjectPool<WriteVectorExchangeEntry> writeVectorPool = new PlainObjectPool<>(WriteVectorExchangeEntry::new);

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
        return timeout.equals(Option.empty()) ? 0 : SubmitQueueEntryFlags.IOSQE_IO_LINK;
    }

    public ReadExchangeEntry forRead(BiConsumer<Result<SizeT>, Proactor> completion,
                                     FileDescriptor fd,
                                     OffHeapBuffer buffer,
                                     OffsetT offset,
                                     Option<Timeout> timeout) {
        return readPool.alloc()
                       .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public WriteExchangeEntry forWrite(BiConsumer<Result<SizeT>, Proactor> completion,
                                       FileDescriptor fd,
                                       OffHeapBuffer buffer,
                                       OffsetT offset,
                                       Option<Timeout> timeout) {
        return writePool.alloc()
                        .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public SpliceExchangeEntry forSplice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        return splicePool.alloc()
                         .prepare(completion, descriptor, calculateFlags(timeout));
    }

    public OpenExchangeEntry forOpen(BiConsumer<Result<FileDescriptor>, Proactor> completion,
                                     Path path,
                                     Set<OpenFlags> openFlags,
                                     Set<FilePermission> mode,
                                     Option<Timeout> timeout) {
        return openPool.alloc()
                       .prepare(completion, path, Bitmask.combine(openFlags), Bitmask.combine(mode), calculateFlags(timeout));
    }

    public SocketExchangeEntry forSocket(BiConsumer<Result<FileDescriptor>, Proactor> completion,
                                         AddressFamily addressFamily,
                                         SocketType socketType,
                                         Set<SocketFlag> openFlags,
                                         Set<SocketOption> options) {
        return socketPool.alloc()
                         .prepare(completion, addressFamily, socketType, openFlags, options);
    }

    public ServerExchangeEntry forServer(BiConsumer<Result<ServerContext<?>>, Proactor> completion,
                                         SocketAddress<?> socketAddress,
                                         SocketType socketType,
                                         Set<SocketFlag> openFlags,
                                         SizeT queueDepth,
                                         Set<SocketOption> options) {
        return serverPool.alloc()
                         .prepare(completion, socketAddress, socketType, openFlags, queueDepth, options);
    }

    public AcceptExchangeEntry forAccept(BiConsumer<Result<ConnectionContext<?>>, Proactor> completion, FileDescriptor socket, Set<SocketFlag> flags) {
        return acceptPool.alloc()
                         .prepare(completion, socket.descriptor(), Bitmask.combine(flags));
    }

    public ConnectExchangeEntry forConnect(BiConsumer<Result<FileDescriptor>, Proactor> completion,
                                           FileDescriptor socket,
                                           OffHeapSocketAddress<SocketAddress<?>, ExternalRawStructure<?>> clientAddress,
                                           Option<Timeout> timeout) {
        return connectPool.alloc()
                          .prepare(completion, socket, clientAddress, calculateFlags(timeout));
    }

    public StatExchangeEntry forStat(BiConsumer<Result<FileStat>, Proactor> completion,
                                     int descriptor,
                                     int statFlags,
                                     int statMask,
                                     OffHeapCString rawPath) {
        return statPool.alloc()
                       .prepare(completion, descriptor, statFlags, statMask, rawPath);
    }

    public ReadVectorExchangeEntry forReadVector(BiConsumer<Result<SizeT>, Proactor> completion,
                                                 FileDescriptor fileDescriptor,
                                                 OffsetT offset,
                                                 Option<Timeout> timeout,
                                                 OffHeapIoVector ioVector) {
        return readVectorPool.alloc()
                             .prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), ioVector);
    }

    public WriteVectorExchangeEntry forWriteVector(BiConsumer<Result<SizeT>, Proactor> completion,
                                                   FileDescriptor fileDescriptor,
                                                   OffsetT offset,
                                                   Option<Timeout> timeout,
                                                   OffHeapIoVector ioVector) {
        return writeVectorPool.alloc()
                              .prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), ioVector);
    }

    public void clear() {
        nopPool.clear();
        delayPool.clear();
        closePool.clear();
        timeoutPool.clear();
        readPool.clear();
        writePool.clear();
        splicePool.clear();
        openPool.clear();
        socketPool.clear();
        serverPool.clear();
        acceptPool.clear();
        connectPool.clear();
        statPool.clear();
        readVectorPool.clear();
        writeVectorPool.clear();
    }
}