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

package org.pfj.io.async;

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
import org.pfj.lang.Option;
import org.pfj.lang.Result;
import org.pfj.lang.Unit;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Low level externally accessible API for submission of I/O operations.
 * The API designed as a <a href="https://en.wikipedia.org/wiki/Proactor_pattern">Proactor</a> pattern.
 */
public interface Proactor {
    int DEFAULT_QUEUE_SIZE = 128;

    /**
     * Create an instance with default queue length.
     */
    static Proactor proactor() {
        return proactor(DEFAULT_QUEUE_SIZE);
    }

    /**
     * Create an instance with default queue length.
     */
    static Proactor proactor(int queueSize) {
        return ProactorImpl.proactor(queueSize);
    }

    /**
     * Perform internal tasks
     */
    Proactor processIO();

    /**
     * Close current Proactor instance.
     */
    void close();

    /**
     * Submit NOP operation.
     * <p>
     * This operation actually does nothing except performing round trip to OS kernel and back.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     */
    void nop(final BiConsumer<Result<Unit>, Proactor> completion);

    /**
     * Submit DELAY (TIMEOUT) operation.
     * <p>
     * This operation completes after specified timeout. More or less precise value of the actual delay passed as a parameter to the callback.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param timeout
     *         Requested delay.
     */
    void delay(final BiConsumer<Result<Duration>, Proactor> completion, final Timeout timeout);

    /**
     * Submit SPLICE operation.
     * <p>
     * Copies data from one file descriptor to another. Upon completion number of copied bytes is passed to the callback.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param descriptor
     *         Splice operation details container
     * @param timeout
     *         Optional operation timeout.
     */

    void splice(final BiConsumer<Result<SizeT>, Proactor> completion,
                final SpliceDescriptor descriptor,
                final Option<Timeout> timeout);

    /**
     * Submit READ operation.
     * <p>
     * Read from specified file descriptor. The number of bytes to read is defined by the provided buffer {@link OffHeapBuffer#size()}.
     * Upon successful completion {@code buffer} has its {@link OffHeapBuffer#used(int)} value set to number of bytes actually read.
     * The number of bytes read also passed as a parameter to callback upon completion.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param fdIn
     *         File descriptor to read from.
     * @param buffer
     *         Data buffer.
     * @param offset
     *         Offset to read from if file descriptor points to file.
     * @param timeout
     *         Optional operation timeout.
     */
    void read(final BiConsumer<Result<SizeT>, Proactor> completion,
              final FileDescriptor fdIn,
              final OffHeapBuffer buffer,
              final OffsetT offset,
              final Option<Timeout> timeout);

    /**
     * Submit WRITE operation.
     * <p>
     * Writes data into specified file descriptor at specified offset. The number of bytes to write is defined by the provided buffer
     * {@link OffHeapBuffer#used()}. Number of bytes actually written is passed as a parameter to provided callback.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param fdOut
     *         File descriptor to write to.
     * @param buffer
     *         Data buffer.
     * @param offset
     *         Offset in a file to start writing if file descriptor points to file.
     * @param timeout
     *         Optional operation timeout.
     */
    void write(final BiConsumer<Result<SizeT>, Proactor> promise,
               final FileDescriptor fdOut,
               final OffHeapBuffer buffer,
               final OffsetT offset,
               final Option<Timeout> timeout);

    /**
     * Submit CLOSE operation.
     * <p>
     * Closes specified file descriptor (either file or socket). Upon completion callback is invoked with {@link Unit} instance as a parameter.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param fd
     *         File descriptor to close.
     * @param timeout
     *         Optional operation timeout.
     */
    void closeFileDescriptor(final BiConsumer<Result<Unit>, Proactor> completion,
                             final FileDescriptor fd,
                             final Option<Timeout> timeout);

    /**
     * Submit OPEN operation.
     * <p>
     * Open file at specified location. Upon completion callback is invoked with file descriptor of opened file as a parameter.
     * <p>
     * Note that this method only partially covers functionality of the underlying {@code openat(2)} call.
     * Instead simpler {@code open(2)} semantics is implemented.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param path
     *         File path.
     * @param flags
     *         File open flags.
     * @param mode
     *         File open mode. Must be present only if {@code flags} contains {@link OpenFlags#CREATE} or {@link OpenFlags#TMPFILE}.
     * @param timeout
     *         Optional operation timeout.
     */
    void open(final BiConsumer<Result<FileDescriptor>, Proactor> completion,
              final Path path,
              final Set<OpenFlags> flags,
              final Set<FilePermission> mode,
              final Option<Timeout> timeout);

    /**
     * Create socket for making client-side connections/requests. Upon completion callback is invoked with opened socket file descriptor
     * as a parameter.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param addressFamily
     *         Socket address family (see {@link AddressFamily})
     * @param socketType
     *         Socket type. Usually it's {@link SocketType#STREAM} for TCP and {@link SocketType#DGRAM} for UDP.
     * @param openFlags
     *         Socket open flags. See {@link SocketFlag} for more details.
     * @param options
     *         Additional socket options. See {@link SocketOption} for more details.
     */
    void socket(final BiConsumer<Result<FileDescriptor>, Proactor> completion,
                final AddressFamily addressFamily,
                final SocketType socketType,
                final Set<SocketFlag> openFlags,
                final Set<SocketOption> options);

    /**
     * Create server connector bound to specified address/port and is ready to accept incoming connection.
     * Upon completion provided callback is invoked with the filled server context instance.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param socketAddress
     *         Socket address
     * @param socketType
     *         Socket type
     * @param openFlags
     *         Socket open flags
     * @param queueDepth
     *         Depth of the listening queue
     * @param options
     *         Socket options. See {@link SocketOption} for more details
     * @see ServerContext
     */
    void server(final BiConsumer<Result<ServerContext<?>>, Proactor> completion,
                final SocketAddress<?> socketAddress,
                final SocketType socketType,
                final Set<SocketFlag> openFlags,
                final SizeT queueDepth,
                final Set<SocketOption> options);

    /**
     * Submit ACCEPT operation.
     * <p>
     * Accept incoming connection for server socket. Upon completion filled client connection descriptor is passed to callback as a
     * parameter.
     * <p>
     * Accepted connection receives its own socket which then can be used to communicate (read/write) with particular client.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param socket
     *         Server socket to accept connections on.
     * @param flags
     *         Accept flags (see {@link SocketFlag} for more details)
     * @see ClientConnection
     */
    void accept(final BiConsumer<Result<ClientConnection<?>>, Proactor> completion,
                final FileDescriptor socket,
                final Set<SocketFlag> flags);

    /**
     * Submit CONNECT operation.
     * <p>
     * Connect to external server at provided address (host/port). Upon completion callback is invoked with the file descriptor
     * passed as a parameter for convenience.
     * <p>
     * Returned {@link Promise} for convenience holds the same file descriptor as passed in {@code socket} parameter.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param socket
     *         Socket to connect
     * @param address
     *         Address to connect
     * @param timeout
     *         Optional operation timeout.
     */
    void connect(final BiConsumer<Result<FileDescriptor>, Proactor> completion,
                 final FileDescriptor socket,
                 final SocketAddress<?> address,
                 final Option<Timeout> timeout);

    /**
     * Get file status information for file specified by path.
     * Upon completion callback is invoked with requested file status details as a parameter.
     *
     * @param completion
     *         Callback which is invoked once operation is finished.
     * @param path
     *         File path
     * @param flags
     *         Flags which affect how information is retrieved, refer to {@link StatFlag} for more details
     * @param mask
     *         Specification of which information should be retrieved.
     * @param timeout
     *         Optional operation timeout
     *
     * @see FileStat
     */
    void stat(final BiConsumer<Result<FileStat>, Proactor> completion,
              final Path path,
              final Set<StatFlag> flags,
              final Set<StatMask> mask,
              final Option<Timeout> timeout);

    /**
     * Get file status information for file specified by file descriptor.
     * Upon completion callback is invoked with requested file status details as a parameter.
     *
     * @param fd
     *         File descriptor
     * @param flags
     *         Flags which affect how information is retrieved, refer to {@link StatFlag} for more details
     * @param mask
     *         Specification of which information should be retrieved.
     * @param timeout
     *         Optional operation timeout
     *
     * @see FileStat
     */
    void stat(final BiConsumer<Result<FileStat>, Proactor> completion,
              final FileDescriptor fd,
              final Set<StatFlag> flags,
              final Set<StatMask> mask,
              final Option<Timeout> timeout);

    /**
     * Read into buffers passed as a parameters.
     * <p>
     * Note that for proper operation this method requires that every passed buffer should have set {@link OffHeapBuffer#used()} value to actual number of bytes to be read into
     * this buffer.
     * <p>
     * Upon completion callback is invoked with total number of bytes read.
     *
     * @param fileDescriptor
     *         File descriptor to read from
     * @param offset
     *         Initial offset in the input file
     * @param timeout
     *         Optional operation timeout
     * @param buffers
     *         Set of buffers where read information will be put. Each buffer should have it's {@link OffHeapBuffer#used()} property set to actual number of bytes which application
     *         expects to see in this buffer.
     */
    void readVector(final BiConsumer<Result<SizeT>, Proactor> completion,
                    final FileDescriptor fileDescriptor,
                    final OffsetT offset,
                    final Option<Timeout> timeout,
                    final OffHeapBuffer... buffers);

    /**
     * Write from buffers passed as a parameters.
     * <p>
     * Note that only {@link OffHeapBuffer#used()} portion of the each buffer is written.
     * <p>
     * Upon completion callback is invoked with total number of bytes written.
     *
     * @param fileDescriptor
     *         File descriptor to read from
     * @param offset
     *         Initial offset in file
     * @param timeout
     *         Optional operation timeout
     * @param buffers
     *         Set of buffers to write from
     */
    void writeVector(final BiConsumer<Result<SizeT>, Proactor> completion,
                     final FileDescriptor fileDescriptor,
                     final OffsetT offset,
                     final Option<Timeout> timeout,
                     final OffHeapBuffer... buffers);

    //TODO: implement batching?
    //TODO: recv, send - implement later, when handling for specific cases will be necessary
}
