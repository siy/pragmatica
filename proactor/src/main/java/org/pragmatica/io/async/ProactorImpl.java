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
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.OpenFlags;
import org.pragmatica.io.async.file.SpliceDescriptor;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.file.stat.StatFlag;
import org.pragmatica.io.async.file.stat.StatMask;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.UringApi;
import org.pragmatica.io.async.uring.UringSetupFlags;
import org.pragmatica.io.async.uring.exchange.*;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.SubmitQueueEntryFlags;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.lang.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector.withBuffers;

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
    private final ExchangeEntryFactory factory = new ExchangeEntryFactory();

    private ProactorImpl(UringApi uringApi) {
        this.uringApi = uringApi;
        pendingCompletions = ObjectHeap.objectHeap(uringApi.numEntries());
    }

    public static Proactor proactor(int queueSize) {
        return proactor(queueSize, UringSetupFlags.defaultFlags());
    }

    public static Proactor proactor(int queueSize, Set<UringSetupFlags> openFlags) {
        return new ProactorImpl(UringApi.uringApi(queueSize, openFlags).fold(ProactorImpl::fail, Functions::id));
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
        uringApi.processSubmissions();

        if (pendingCompletions.count() > 0) {
            return uringApi.processCompletions(pendingCompletions, this);
        }

        return 0;
    }

    private final NopExchangeEntry nop = new NopExchangeEntry(null);
    private final DelayExchangeEntry delay = new DelayExchangeEntry(null);
    private final CloseExchangeEntry close = new CloseExchangeEntry(null);
    private final TimeoutExchangeEntry timeout = new TimeoutExchangeEntry(null);
    private final ReadExchangeEntry read = new ReadExchangeEntry(null);
    private final WriteExchangeEntry write = new WriteExchangeEntry(null);
    private final SpliceExchangeEntry splice = new SpliceExchangeEntry(null);
    private final OpenExchangeEntry open = new OpenExchangeEntry(null);
    private final SocketExchangeEntry socket = new SocketExchangeEntry(null);
    private final StatExchangeEntry stat = new StatExchangeEntry(null);
    private final ReadVectorExchangeEntry readVector = new ReadVectorExchangeEntry(null);
    private final WriteVectorExchangeEntry writeVector = new WriteVectorExchangeEntry(null);
    private final ConnectExchangeEntry connect = new ConnectExchangeEntry(null);
    @SuppressWarnings("rawtypes")
    private final ListenExchangeEntry listen = new ListenExchangeEntry<>(null);
    @SuppressWarnings("rawtypes")
    private final AcceptExchangeEntry accept = new AcceptExchangeEntry<>(null);


    @Override
    public void nop(BiConsumer<Result<Unit>, Proactor> completion) {
        uringApi.submit(nop.prepare(completion).register(pendingCompletions));
    }

    @Override
    public void delay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout) {
        uringApi.submit(delay.prepare(completion, timeout).register(pendingCompletions));
    }

    @Override
    public void close(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout) {
        uringApi.submit(close.prepare(completion, fd.descriptor(), calculateFlags(timeout))
                             .register(pendingCompletions));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapBuffer buffer,
                     OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(read.prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout))
                            .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapBuffer buffer,
                      OffsetT offset, Option<Timeout> timeout) {
        uringApi.submit(write.prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout))
                             .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void splice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        uringApi.submit(splice.prepare(completion, descriptor, calculateFlags(timeout))
                              .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void open(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> flags,
                     Set<FilePermission> mode, Option<Timeout> timeout) {
        uringApi.submit(open.prepare(completion, path, Bitmask.combine(flags), Bitmask.combine(mode), calculateFlags(timeout))
                            .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void socket(BiConsumer<Result<FileDescriptor>, Proactor> completion, AddressFamily addressFamily,
                       SocketType socketType, Set<SocketFlag> openFlags, Set<SocketOption> options) {
        uringApi.submit(socket.prepare(completion, addressFamily, socketType, openFlags, options)
                              .register(pendingCompletions));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends InetAddress> void listen(BiConsumer<Result<ListenContext<T>>, Proactor> completion,
                                               SocketAddress<T> socketAddress, SocketType socketType,
                                               Set<SocketFlag> openFlags, SizeT queueDepth, Set<SocketOption> options) {
        uringApi.submit(listen.prepare(completion, socketAddress, socketType, openFlags, queueDepth, options)
                              .register(pendingCompletions));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends InetAddress> void accept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                               FileDescriptor socket, Set<SocketFlag> flags, T addressType) {
        uringApi.submit(accept.prepare(completion, socket.descriptor(), Bitmask.combine(flags), addressType instanceof InetAddress.Inet6Address)
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

        uringApi.submit(connect.prepare(completion, socket, clientAddress, calculateFlags(timeout))
                               .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion,
                     Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        //Reset EMPTY_PATH and force use the path.
        uringApi.submit(stat.prepare(completion,
                                     AT_FDCWD,
                                     Bitmask.combine(flags) & ~StatFlag.EMPTY_PATH.mask(),
                                     Bitmask.combine(mask),
                                     OffHeapCString.cstring(path.toString()))
                            .register(pendingCompletions));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void stat(BiConsumer<Result<FileStat>, Proactor> completion, FileDescriptor fd, Set<StatFlag> flags,
                     Set<StatMask> mask, Option<Timeout> timeout) {
        //Set EMPTY_PATH and force use of file descriptor.
        uringApi.submit(stat.prepare(completion,
                                     fd.descriptor(),
                                     Bitmask.combine(flags) | StatFlag.EMPTY_PATH.mask(),
                                     Bitmask.combine(mask),
                                     OffHeapCString.cstring(""))
                            .register(pendingCompletions));
        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                     Option<Timeout> timeout, OffHeapBuffer... buffers) {
        uringApi.submit(readVector.prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), withBuffers(buffers))
                                  .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    @Override
    public void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                      Option<Timeout> timeout, OffHeapBuffer... buffers) {
        uringApi.submit(writeVector.prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), withBuffers(buffers))
                                   .register(pendingCompletions));

        timeout.whenPresent(this::appendTimeout);
    }

    private void appendTimeout(Timeout value) {
        uringApi.submit(timeout.prepare(value).register(pendingCompletions));
    }

    private byte calculateFlags(Option<Timeout> timeout) {
        return timeout.equals(Option.empty()) ? 0 : SubmitQueueEntryFlags.IOSQE_IO_LINK;
    }
}
