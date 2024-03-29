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
import org.pragmatica.io.async.uring.UringApi;
import org.pragmatica.io.async.uring.UringSetupFlags;
import org.pragmatica.io.async.util.DaemonThreadFactory;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.Units;
import org.pragmatica.io.async.util.allocator.ChunkedAllocator;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.pragmatica.lang.Option.empty;

/**
 * Low level externally accessible API for submission of I/O operations. The API designed as a <a
 * href="https://en.wikipedia.org/wiki/Proactor_pattern">Proactor</a> pattern.
 */
//TODO: finish docs
public interface Proactor {
    /**
     * Submit NOP operation.
     * <p>
     * This operation actually does nothing except performing round trip to OS kernel and back.
     *
     * @param completion Callback which is invoked once operation is finished.
     */
    void nop(BiConsumer<Result<Unit>, Proactor> completion);

    default void nop(Consumer<Result<Unit>> completion) {
        nop((result, __) -> completion.accept(result));
    }

    /**
     * Submit DELAY (TIMEOUT) operation.
     * <p>
     * This operation completes after specified timeout. More or less precise value of the actual delay passed as a parameter to the callback.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param timeout    Requested delay.
     */
    void delay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout);

    default void delay(Consumer<Result<Duration>> completion, Timeout timeout) {
        delay((result, __) -> completion.accept(result), timeout);
    }

    /**
     * Submit SPLICE operation.
     * <p>
     * Copies data from one file descriptor to another. Upon completion number of copied bytes is passed to the callback.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param descriptor Splice operation details container
     * @param timeout    Optional operation timeout.
     */
    void splice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout);

    default void splice(Consumer<Result<SizeT>> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        splice((result, __) -> completion.accept(result), descriptor, timeout);
    }

    /**
     * Submit READ operation.
     * <p>
     * Read from specified file descriptor. The number of bytes to read is defined by the provided buffer {@link OffHeapSlice#size()}. Upon successful
     * completion {@code buffer} has its {@link OffHeapSlice#used(int)} value set to number of bytes actually read. The number of bytes read also
     * passed as a parameter to callback upon completion.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param fd         File descriptor to read from.
     * @param buffer     Data buffer.
     * @param offset     Offset to read from if file descriptor points to file.
     * @param timeout    Optional operation timeout.
     */
    void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer, OffsetT offset, Option<Timeout> timeout);

    default void read(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer, OffsetT offset, Option<Timeout> timeout) {
        read((result, __) -> completion.accept(result), fd, buffer, offset, timeout);
    }

    default void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer, Option<Timeout> timeout) {
        read(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void read(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer, Option<Timeout> timeout) {
        read(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void read(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer) {
        read(completion, fd, buffer, OffsetT.ZERO, empty());
    }

    default void read(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer) {
        read((result, __) -> completion.accept(result), fd, buffer);
    }

    /**
     * Submit WRITE operation.
     * <p>
     * Writes data into specified file descriptor at specified offset. The number of bytes to write is defined by the provided buffer
     * {@link OffHeapSlice#used()}. Number of bytes actually written is passed as a parameter to provided callback.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param fd         File descriptor to write to.
     * @param buffer     Data buffer.
     * @param offset     Offset in a file to start writing if file descriptor points to file.
     * @param timeout    Optional operation timeout.
     */
    void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer, OffsetT offset, Option<Timeout> timeout);

    default void write(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer, OffsetT offset, Option<Timeout> timeout) {
        write((result, __) -> completion.accept(result), fd, buffer, offset, timeout);
    }

    default void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer, Option<Timeout> timeout) {
        write(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void write(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer, Option<Timeout> timeout) {
        write(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void write(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer) {
        write(completion, fd, buffer, OffsetT.ZERO, empty());
    }

    default void write(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer) {
        write((result, __) -> completion.accept(result), fd, buffer);
    }

    /**
     * Submit CLOSE operation.
     * <p>
     * Closes specified file descriptor (either file or socket). Upon completion callback is invoked with {@link Unit} instance as a parameter.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param fd         File descriptor to close.
     * @param timeout    Optional operation timeout.
     */
    void close(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout);

    default void close(Consumer<Result<Unit>> completion, FileDescriptor fd, Option<Timeout> timeout) {
        close((result, __) -> completion.accept(result), fd, timeout);
    }

    default void close(Consumer<Result<Unit>> completion, FileDescriptor fd) {
        close((result, __) -> completion.accept(result), fd, empty());
    }

    /**
     * Submit OPEN operation.
     * <p>
     * Open file at specified location. Upon completion callback is invoked with file descriptor of opened file as a parameter.
     * <p>
     * Note that this method only partially covers functionality of the underlying {@code openat(2)} call. Instead, simpler {@code open(2)} semantics
     * is implemented.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param path       File path.
     * @param flags      File open flags.
     * @param mode       File open mode. Must be present only if {@code flags} contains {@link OpenFlags#CREATE} or {@link OpenFlags#TMPFILE}.
     * @param timeout    Optional operation timeout.
     */
    void open(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> flags,
              Set<FilePermission> mode, Option<Timeout> timeout);

    default void open(Consumer<Result<FileDescriptor>> completion, Path path, Set<OpenFlags> flags,
                      Set<FilePermission> mode, Option<Timeout> timeout) {
        open((result, __) -> completion.accept(result), path, flags, mode, timeout);
    }

    /**
     * Create socket for making client-side connections/requests. Upon completion callback is invoked with opened socket file descriptor as a
     * parameter.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param af         Socket address family (see {@link AddressFamily})
     * @param type       Socket type. Usually it's {@link SocketType#STREAM} for TCP and {@link SocketType#DGRAM} for UDP.
     * @param flags      Socket open flags. See {@link SocketFlag} for more details.
     * @param options    Additional socket options. See {@link SocketOption} for more details.
     */
    void socket(Consumer<Result<FileDescriptor>> completion, AddressFamily af, SocketType type,
                Set<SocketFlag> flags, Set<SocketOption> options);

    /**
     * Create listener bound to specified address/port and ready to accept incoming connection. Upon completion provided callback is invoked with the
     * filled listen context instance.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param address    Socket address
     * @param type       Socket type
     * @param flags      Socket open flags
     * @param len        Length of the listening queue
     * @param options    Socket options. See {@link SocketOption} for more details
     *
     * @see ListenContext
     */
    <T extends InetAddress> void listen(Consumer<Result<ListenContext<T>>> completion,
                                        SocketAddress<T> address, SocketType type,
                                        Set<SocketFlag> flags, SizeT len, Set<SocketOption> options);

    /**
     * Submit ACCEPT operation.
     * <p>
     * Accept incoming connection for server socket. Upon completion filled client connection descriptor is passed to callback as a parameter.
     * <p>
     * Accepted connection receives its own socket which then can be used to communicate (read/write) with particular client.
     *
     * @param completion  Callback which is invoked once operation is finished.
     * @param socket      Listening socket to accept connections on.
     * @param flags       Accept flags (see {@link SocketFlag} for more details)
     * @param addressType tag for address type (TCPv4 or TCPv6). Actual value is irrelevant, matters only type. Constants
     *                    {@link InetAddress.Inet4Address#INADDR_ANY} and {@link InetAddress.Inet6Address#INADDR_ANY} could be used for this purpose.
     *
     * @see ConnectionContext
     */
    <T extends InetAddress> void accept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                        FileDescriptor socket, Set<SocketFlag> flags, T addressType);

    default <T extends InetAddress> void accept(Consumer<Result<ConnectionContext<T>>> completion,
                                                FileDescriptor socket, Set<SocketFlag> flags, T addressType) {
        accept((result, __) -> completion.accept(result), socket, flags, addressType);
    }

    default void acceptV4(BiConsumer<Result<ConnectionContext<InetAddress.Inet4Address>>, Proactor> completion,
                          FileDescriptor socket,
                          Set<SocketFlag> flags) {
        accept(completion, socket, flags, InetAddress.Inet4Address.INADDR_ANY);
    }

    default void acceptV4(Consumer<Result<ConnectionContext<InetAddress.Inet4Address>>> completion, FileDescriptor socket, Set<SocketFlag> flags) {
        accept(completion, socket, flags, InetAddress.Inet4Address.INADDR_ANY);
    }

    default void acceptV6(BiConsumer<Result<ConnectionContext<InetAddress.Inet6Address>>, Proactor> completion,
                          FileDescriptor socket,
                          Set<SocketFlag> flags) {
        accept(completion, socket, flags, InetAddress.Inet6Address.INADDR_ANY);
    }

    default void acceptV6(Consumer<Result<ConnectionContext<InetAddress.Inet6Address>>> completion, FileDescriptor socket, Set<SocketFlag> flags) {
        accept(completion, socket, flags, InetAddress.Inet6Address.INADDR_ANY);
    }

    /**
     * Submit CONNECT operation.
     * <p>
     * Connect to external server at provided address (host/port). Upon completion callback is invoked with the file descriptor passed as a parameter
     * for convenience.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param socket     Socket to connect
     * @param address    Address to connect
     * @param timeout    Optional operation timeout.
     */
    <T extends InetAddress> void connect(BiConsumer<Result<FileDescriptor>, Proactor> completion, FileDescriptor socket,
                                         SocketAddress<T> address, Option<Timeout> timeout);

    default <T extends InetAddress> void connect(Consumer<Result<FileDescriptor>> completion, FileDescriptor socket,
                                                 SocketAddress<T> address, Option<Timeout> timeout) {
        connect((result, __) -> completion.accept(result), socket, address, timeout);
    }

    /**
     * Get file status information for file specified by path. Upon completion callback is invoked with requested file status details as a parameter.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param path       File path
     * @param flags      Flags which affect how information is retrieved, refer to {@link StatFlag} for more details
     * @param mask       Specification of which information should be retrieved.
     * @param timeout    Optional operation timeout
     *
     * @see FileStat
     */
    void stat(BiConsumer<Result<FileStat>, Proactor> completion, Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout);

    default void stat(Consumer<Result<FileStat>> completion, Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        stat((result, __) -> completion.accept(result), path, flags, mask, timeout);
    }

    /**
     * Get file status information for file specified by file descriptor. Upon completion callback is invoked with requested file status details as a
     * parameter.
     *
     * @param completion Callback which is invoked once operation is finished.
     * @param fd         File descriptor
     * @param flags      Flags which affect how information is retrieved, refer to {@link StatFlag} for more details
     * @param mask       Specification of which information should be retrieved.
     * @param timeout    Optional operation timeout
     *
     * @see FileStat
     */
    void stat(BiConsumer<Result<FileStat>, Proactor> completion, FileDescriptor fd, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout);

    default void stat(Consumer<Result<FileStat>> completion, FileDescriptor fd, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        stat((result, __) -> completion.accept(result), fd, flags, mask, timeout);
    }

    /**
     * Read into buffers passed as a parameters.
     * <p>
     * Note that for proper operation this method requires that every passed buffer should have set {@link OffHeapSlice#used()} value to actual number
     * of bytes to be read into this buffer.
     * <p>
     * Upon completion callback is invoked with total number of bytes read.
     *
     * @param completion     Callback which is invoked once operation is finished.
     * @param fileDescriptor File descriptor to read from
     * @param offset         Initial offset in the input file
     * @param timeout        Optional operation timeout
     * @param buffers        Set of buffers where read information will be put. Each buffer should have it's {@link OffHeapSlice#used()} property set
     *                       to actual number of bytes which application expects to see in this buffer.
     */
    void readVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                    Option<Timeout> timeout, OffHeapSlice... buffers);

    default void readVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor, OffsetT offset,
                            Option<Timeout> timeout, OffHeapSlice... buffers) {
        readVector((result, __) -> completion.accept(result), fileDescriptor, offset, timeout, buffers);
    }

    default void readVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                            Option<Timeout> timeout, OffHeapSlice... buffers) {
        readVector(completion, fileDescriptor, OffsetT.ZERO, timeout, buffers);
    }

    default void readVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor,
                            Option<Timeout> timeout, OffHeapSlice... buffers) {
        readVector(completion, fileDescriptor, OffsetT.ZERO, timeout, buffers);
    }

    default void readVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                            OffHeapSlice... buffers) {
        readVector(completion, fileDescriptor, offset, empty(), buffers);
    }

    default void readVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor, OffsetT offset,
                            OffHeapSlice... buffers) {
        readVector(completion, fileDescriptor, offset, empty(), buffers);
    }

    default void readVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                            OffHeapSlice... buffers) {
        readVector(completion, fileDescriptor, OffsetT.ZERO, empty(), buffers);
    }

    default void readVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor, OffHeapSlice... buffers) {
        readVector(completion, fileDescriptor, OffsetT.ZERO, empty(), buffers);
    }

    /**
     * Write from buffers passed as a parameters.
     * <p>
     * Note that only {@link OffHeapSlice#used()} portion of the each buffer is written.
     * <p>
     * Upon completion callback is invoked with total number of bytes written.
     *
     * @param completion     Callback which is invoked once operation is finished.
     * @param fileDescriptor File descriptor to read from
     * @param offset         Initial offset in file
     * @param timeout        Optional operation timeout
     * @param buffers        Set of buffers to write from
     */
    void writeVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                     Option<Timeout> timeout, OffHeapSlice... buffers);

    default void writeVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor, OffsetT offset,
                             Option<Timeout> timeout, OffHeapSlice... buffers) {
        writeVector((result, __) -> completion.accept(result), fileDescriptor, offset, timeout, buffers);
    }

    default void writeVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                             Option<Timeout> timeout, OffHeapSlice... buffers) {
        writeVector(completion, fileDescriptor, OffsetT.ZERO, timeout, buffers);
    }

    default void writeVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor,
                             Option<Timeout> timeout, OffHeapSlice... buffers) {
        writeVector(completion, fileDescriptor, OffsetT.ZERO, timeout, buffers);
    }

    default void writeVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor, OffsetT offset,
                             OffHeapSlice... buffers) {
        writeVector(completion, fileDescriptor, offset, empty(), buffers);
    }

    default void writeVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor, OffsetT offset,
                             OffHeapSlice... buffers) {
        writeVector(completion, fileDescriptor, offset, empty(), buffers);
    }

    default void writeVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                             OffHeapSlice... buffers) {
        writeVector(completion, fileDescriptor, OffsetT.ZERO, empty(), buffers);
    }

    default void writeVector(Consumer<Result<SizeT>> completion, FileDescriptor fileDescriptor, OffHeapSlice... buffers) {
        writeVector(completion, fileDescriptor, OffsetT.ZERO, empty(), buffers);
    }

    /**
     * Perform a file synchronization between memory and file system.
     * <p>
     * Note that this operation is subject of some limitations, in particular there are no guarantees that it will flush buffers modified by
     * previously submitted (but not yet finished) write operation.
     *
     * @param completion     Callback which is invoked once operation is finished.
     * @param fileDescriptor File descriptor to read from
     * @param syncMetadata   Flag which controls flushing of file metadata: {@code true} enables syncing metadata
     * @param timeout        Optional operation timeout
     */
    void fileSync(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                  boolean syncMetadata, Option<Timeout> timeout);

    default void fileSync(Consumer<Result<Unit>> completion, FileDescriptor fileDescriptor,
                          boolean syncMetadata, Option<Timeout> timeout) {
        fileSync((result, __) -> completion.accept(result), fileDescriptor, syncMetadata, timeout);
    }

    /**
     * Perform changes in file allocation - add/remove/replace part of the file.
     *
     * @param completion     Callback which is invoked once operation is finished.
     * @param fileDescriptor File descriptor to read from
     * @param allocFlags     Flags which define type of the operation and behavior.
     * @param offset         Offset in file
     * @param len            Length of the affected part of the file.
     * @param timeout        Optional operation timeout
     */
    void fileAlloc(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fileDescriptor,
                   Set<FileAllocFlags> allocFlags, OffsetT offset, long len, Option<Timeout> timeout);

    default void fileAlloc(Consumer<Result<Unit>> completion, FileDescriptor fileDescriptor,
                           Set<FileAllocFlags> allocFlags, OffsetT offset, long len, Option<Timeout> timeout) {
        fileAlloc((result, __) -> completion.accept(result), fileDescriptor, allocFlags, offset, len, timeout);
    }

    /**
     * Allocate fixed buffer which will be shared between kernel and user space and can be used with
     * {@link #readFixed(BiConsumer, FileDescriptor, FixedBuffer, OffsetT, Option)} and
     * {@link #writeFixed(BiConsumer, FileDescriptor, FixedBuffer, OffsetT, Option)} methods.
     * <p>
     * Fixed buffers are allocated from common memory arena shared across all instances of {@link Proactor}. The arena size is configured at start and
     * can't be changed at run time.
     * <p>
     * Note that allocation is relatively slow process and frequent allocation/release of buffers might quickly result to fragmentation, so it is
     * highly recommended avoiding frequent allocation/release of the fixed buffers.
     * <p>
     * Allocated buffer can be released using {@link FixedBuffer#dispose()}. Buffer content must not be accessed once this method is invoked.
     *
     * @param size Buffer size in bytes. Note that allocation is done in chunks of size
     *             {@link org.pragmatica.io.async.util.allocator.ChunkedAllocator#CHUNK_SIZE}.
     *
     * @return allocation result.
     */
    Result<FixedBuffer> allocateFixedBuffer(int size);

    void readFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer, OffsetT offset, Option<Timeout> timeout);

    default void readFixed(Consumer<Result<SizeT>> completion, FileDescriptor fd, FixedBuffer buffer, OffsetT offset, Option<Timeout> timeout) {
        readFixed((result, __) -> completion.accept(result), fd, buffer, offset, timeout);
    }

    default void readFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer, Option<Timeout> timeout) {
        readFixed(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void readFixed(Consumer<Result<SizeT>> completion, FileDescriptor fd, FixedBuffer buffer, Option<Timeout> timeout) {
        readFixed(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void readFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer) {
        readFixed(completion, fd, buffer, OffsetT.ZERO, empty());
    }

    default void readFixed(Consumer<Result<SizeT>> completion, FileDescriptor fd, FixedBuffer buffer) {
        readFixed((result, __) -> completion.accept(result), fd, buffer);
    }


    void writeFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer, OffsetT offset, Option<Timeout> timeout);

    default void writeFixed(Consumer<Result<SizeT>> completion, FileDescriptor fd, FixedBuffer buffer, OffsetT offset, Option<Timeout> timeout) {
        writeFixed((result, __) -> completion.accept(result), fd, buffer, offset, timeout);
    }

    default void writeFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer, Option<Timeout> timeout) {
        writeFixed(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void writeFixed(Consumer<Result<SizeT>> completion, FileDescriptor fd, FixedBuffer buffer, Option<Timeout> timeout) {
        writeFixed(completion, fd, buffer, OffsetT.ZERO, timeout);
    }

    default void writeFixed(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, FixedBuffer buffer) {
        writeFixed(completion, fd, buffer, OffsetT.ZERO, empty());
    }

    default void writeFixed(Consumer<Result<SizeT>> completion, FileDescriptor fd, FixedBuffer buffer) {
        writeFixed((result, __) -> completion.accept(result), fd, buffer);
    }


    void send(BiConsumer<Result<SizeT>, Proactor> completion,
              FileDescriptor fd,
              OffHeapSlice buffer,
              Set<MessageFlags> msgFlags,
              Option<Timeout> timeout);

    default void send(Consumer<Result<SizeT>> completion,
                      FileDescriptor fd,
                      OffHeapSlice buffer,
                      Set<MessageFlags> msgFlags,
                      Option<Timeout> timeout) {
        send((result, __) -> completion.accept(result), fd, buffer, msgFlags, timeout);
    }

    default void send(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer, Set<MessageFlags> msgFlags) {
        send(completion, fd, buffer, msgFlags, empty());
    }

    default void send(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer, Set<MessageFlags> msgFlags) {
        send((result, __) -> completion.accept(result), fd, buffer, msgFlags, empty());
    }

    void recv(BiConsumer<Result<SizeT>, Proactor> completion,
              FileDescriptor fd,
              OffHeapSlice buffer,
              Set<MessageFlags> msgFlags,
              Option<Timeout> timeout);

    default void recv(Consumer<Result<SizeT>> completion,
                      FileDescriptor fd,
                      OffHeapSlice buffer,
                      Set<MessageFlags> msgFlags,
                      Option<Timeout> timeout) {
        recv((result, __) -> completion.accept(result), fd, buffer, msgFlags, timeout);
    }

    default void recv(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapSlice buffer, Set<MessageFlags> msgFlags) {
        recv(completion, fd, buffer, msgFlags, empty());
    }

    default void recv(Consumer<Result<SizeT>> completion, FileDescriptor fd, OffHeapSlice buffer, Set<MessageFlags> msgFlags) {
        recv((result, __) -> completion.accept(result), fd, buffer, msgFlags, empty());
    }

    //recvmsg, sendmsg, read_fixed, write_fixed


    static Proactor proactor() {
        return ProactorHolder.INSTANCE.get();
    }

    /**
     * Shutdown current Proactor instance.
     */
    void shutdown();

    /**
     * Shutdown entire Proactor pool.
     */
    static void shutdownAll() {
        ProactorHolder.INSTANCE.shutdown();
    }

    enum ProactorHolder {
        INSTANCE;

        private final AtomicInteger counter = new AtomicInteger(0);
        private final List<ProactorImpl> proactors;

        private final ChunkedAllocator allocator = ChunkedAllocator.allocator(Units._1MiB);
        private static final int DEFAULT_QUEUE_SIZE = 128;

        ProactorHolder() {
            var numCores = Runtime.getRuntime().availableProcessors();
            var factory = DaemonThreadFactory.threadFactory("Proactor Worker %d");
//            var numCores = 1;

            proactors = IntStream.range(0, numCores)
                                 .mapToObj(__ -> ProactorImpl.proactor(DEFAULT_QUEUE_SIZE,
                                                                       UringSetupFlags.defaultFlags(),
                                                                       allocator,
                                                                       factory))
                                 .collect(Collectors.toList());
        }

        Proactor get() {
            return proactors.get(counter.incrementAndGet() % proactors.size());
        }

        public void shutdown() {
            proactors.forEach(Proactor::shutdown);
            allocator.close();
        }
    }
}
