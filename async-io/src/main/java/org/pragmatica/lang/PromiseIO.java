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

package org.pragmatica.lang;

import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.*;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.file.stat.StatFlag;
import org.pragmatica.io.async.file.stat.StatMask;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.InetAddress.Inet6Address;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.allocator.FixedBuffer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.pragmatica.lang.Option.empty;

/**
 * "Promisified" Proactor I/O API.
 */
//TODO: finish docs
public interface PromiseIO {
    /**
     * Basic NOP (no-operation). Although the operation does nothing, internally it goes full round-trip to kernel and back.
     *
     * @return a {@link Promise} instance, which is resolved once operations is finished.
     */
    static Promise<Unit> nop() {
        return Promise.promise((promise, proactor) -> proactor.nop(promise::resolve));
    }

    /**
     * Simple delay. The returned {@link Promise} is resolved when specified timeout expires. The {@link Duration} value contains actual delay.
     *
     * @return a {@link Promise} instance, which is resolved once operations is finished.
     */
    static Promise<Duration> delay(Timeout timeout) {
        return Promise.promise((promise, proactor) -> proactor.delay(promise::resolve, timeout));
    }

    /**
     * Perform a copy operation between two file descriptors (see {@link SpliceDescriptor} for more details).
     *
     * @param descriptor Splice operation descriptor.
     * @param timeout    Operation timeout.
     *
     * @return a {@link Promise} instance, which is resolved once operations is finished.
     */
    static Promise<SizeT> splice(SpliceDescriptor descriptor, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.splice(promise::resolve, descriptor, timeout));
    }

    /**
     * Same as {@link #splice(SpliceDescriptor, Option)}, but no operation timeout is used.
     *
     * @param descriptor Splice operation descriptor.
     *
     * @return a {@link Promise} instance, which is resolved once operations is finished.
     */
    static Promise<SizeT> splice(SpliceDescriptor descriptor) {
        return splice(descriptor, empty());
    }

    /**
     * Read data from specified file descriptor into provided buffer. The number of bytes to read is defined by buffer size. Upon successful
     * completion, {@link OffHeapSlice#used()} value is set to actual number of bytes read. Number of read bytes also used to resolve returned
     * promise.
     *
     * @param fd      File descriptor or socket
     * @param buffer  Buffer to store read data
     * @param offset  Offset in the source file if file descriptor points to file. Use {@link OffsetT#ZERO} if file descriptor belongs to socket or
     *                pipe
     * @param timeout Operation timeout
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully or resolved with error
     *     description if operation failed.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapSlice buffer, OffsetT offset, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.read(promise::resolve, fd, buffer, offset, timeout));
    }

    /**
     * Same as {@link #read(FileDescriptor, OffHeapSlice, OffsetT, Option)}, but no timeout specified.
     *
     * @param fd     File descriptor or socket
     * @param buffer Buffer to store read data
     * @param offset Offset in the source file if file descriptor points to file. Use {@link OffsetT#ZERO} if file descriptor belongs to socket or
     *               pipe
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully or resolved with error
     *     description if operation failed.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapSlice buffer, OffsetT offset) {
        return read(fd, buffer, offset, empty());
    }

    /**
     * Same as {@link #read(FileDescriptor, OffHeapSlice, OffsetT, Option)}, but no offset needs to be provided. Convenient for use with sockets or
     * pipes.
     *
     * @param fd      File descriptor or socket
     * @param buffer  Buffer to store read data
     * @param timeout Operation timeout
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully or resolved with error
     *     description if operation failed.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapSlice buffer, Option<Timeout> timeout) {
        return read(fd, buffer, OffsetT.ZERO, timeout);
    }

    /**
     * Same as {@link #read(FileDescriptor, OffHeapSlice, Option)}, but no timeout is specified.
     *
     * @param fd     File descriptor or socket
     * @param buffer Buffer to store read data
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully or resolved with error
     *     description if operation failed.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapSlice buffer) {
        return read(fd, buffer, OffsetT.ZERO, empty());
    }

    /**
     * Write data to specified file descriptor from provided buffer. The number of bytes to write is defined by buffer {@link OffHeapSlice#used()}
     * value. Upon successful completion, number of bytes written also used to resolve returned promise.
     *
     * @param fd      File descriptor or socket
     * @param buffer  Buffer to write
     * @param offset  Offset in the destination file if file descriptor points to file. Use {@link OffsetT#ZERO} if file descriptor belongs to socket
     *                or pipe
     * @param timeout Operation timeout
     *
     * @return a {@link Promise} instance, which is resolved with number of written bytes once operations is finished successfully or resolved with
     *     error description if operation failed.
     */
    static Promise<SizeT> write(FileDescriptor fd, OffHeapSlice buffer, OffsetT offset, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.write(promise::resolve, fd, buffer, offset, timeout));
    }

    /**
     * Same as {@link #write(FileDescriptor, OffHeapSlice, OffsetT, Option)}, but no timeout is specified.
     *
     * @param fd     File descriptor or socket
     * @param buffer Buffer to write
     * @param offset Offset in the destination file if file descriptor points to file. Use {@link OffsetT#ZERO} if file descriptor belongs to socket
     *               or pipe
     *
     * @return a {@link Promise} instance, which is resolved with number of written bytes once operations is finished successfully or resolved with
     *     error description if operation failed.
     */
    static Promise<SizeT> write(FileDescriptor fd, OffHeapSlice buffer, OffsetT offset) {
        return write(fd, buffer, offset, empty());
    }

    /**
     * Same as {@link #write(FileDescriptor, OffHeapSlice, OffsetT, Option)}, but no offset needs to be provided. Convenient for use with sockets or
     * pipes.
     *
     * @param fd      File descriptor or socket
     * @param buffer  Buffer to write
     * @param timeout Operation timeout
     *
     * @return a {@link Promise} instance, which is resolved with number of written bytes once operations is finished successfully or resolved with
     *     error description if operation failed.
     */
    static Promise<SizeT> write(FileDescriptor fd, OffHeapSlice buffer, Option<Timeout> timeout) {
        return write(fd, buffer, OffsetT.ZERO, timeout);
    }

    /**
     * Same as {@link #write(FileDescriptor, OffHeapSlice, Option)}, but no timeout is specified.
     *
     * @param fd     File descriptor or socket
     * @param buffer Buffer to write
     *
     * @return a {@link Promise} instance, which is resolved with number of written bytes once operations is finished successfully or resolved with
     *     error description if operation failed.
     */
    static Promise<SizeT> write(FileDescriptor fd, OffHeapSlice buffer) {
        return write(fd, buffer, OffsetT.ZERO, empty());
    }

    /**
     * Close provided file descriptor.
     *
     * @param fd      File descriptor or socket.
     * @param timeout Operation timeout.
     *
     * @return a {@link Promise} instance, which is resolved when operation is completed.
     */
    static Promise<Unit> close(FileDescriptor fd, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.close(promise::resolve, fd, timeout));
    }

    /**
     * Same as {@link #close(FileDescriptor, Option)}, but no timeout is specified.
     *
     * @param fd File descriptor or socket.
     *
     * @return a {@link Promise} instance, which is resolved when operation is completed.
     */
    static Promise<Unit> close(FileDescriptor fd) {
        return close(fd, empty());
    }

    /**
     * Open file at specified path using provided flags and file permission.
     *
     * @param path    File path
     * @param flags   Set of open flags. See {@link OpenFlags} for more details.
     * @param mode    Open mode (file permissions). See {@link FilePermission} for more details.
     * @param timeout Operation timeout.
     *
     * @return a {@link Promise} instance, which is resolved with the file descriptor if operation was successful or with error description if
     *     operation failed.
     */
    static Promise<FileDescriptor> open(Path path, Set<OpenFlags> flags, Set<FilePermission> mode, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.open(promise::resolve, path, flags, mode, timeout));
    }

    /**
     * Same as {@link #open(Path, Set, Set, Option)}, but no timeout is specified.
     *
     * @param path  File path
     * @param flags Set of open flags. See {@link OpenFlags} for more details.
     * @param mode  Open mode (file permissions). See {@link FilePermission} for more details.
     *
     * @return a {@link Promise} instance, which is resolved with the file descriptor if operation was successful or with error description if
     *     operation failed.
     */
    static Promise<FileDescriptor> open(Path path, Set<OpenFlags> flags, Set<FilePermission> mode) {
        return open(path, flags, mode, empty());
    }

    static Promise<FileDescriptor> socket(AddressFamily af, SocketType type, Set<SocketFlag> flags, Set<SocketOption> options) {
        return Promise.promise((promise, proactor) -> proactor.socket(promise::resolve, af, type, flags, options));
    }

    static <T extends InetAddress> Promise<ListenContext<T>> listen(SocketAddress<T> address, SocketType type,
                                                                    Set<SocketFlag> flags, SizeT len, Set<SocketOption> options) {
        return Promise.promise((promise, proactor) -> proactor.listen(promise::resolve, address, type, flags, len, options));
    }

    static <T extends InetAddress> Promise<ConnectionContext<T>> accept(FileDescriptor socket, Set<SocketFlag> flags, T addressType) {
        return Promise.promise((promise, proactor) -> proactor.accept(promise::resolve, socket, flags, addressType));
    }

    static Promise<ConnectionContext<Inet4Address>> acceptV4(FileDescriptor socket, Set<SocketFlag> flags) {
        return accept(socket, flags, Inet4Address.INADDR_ANY);
    }

    static Promise<ConnectionContext<Inet6Address>> acceptV6(FileDescriptor socket, Set<SocketFlag> flags) {
        return accept(socket, flags, Inet6Address.INADDR_ANY);
    }

    static <T extends InetAddress> Promise<FileDescriptor> connect(FileDescriptor socket, SocketAddress<T> address, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.connect(promise::resolve, socket, address, timeout));
    }

    static <T extends InetAddress> Promise<FileDescriptor> connect(FileDescriptor socket, SocketAddress<T> address) {
        return connect(socket, address, empty());
    }

    static Promise<FileStat> stat(Path path, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.stat(promise::resolve, path, flags, mask, timeout));
    }

    static Promise<FileStat> stat(Path path, Set<StatFlag> flags, Set<StatMask> mask) {
        return stat(path, flags, mask, empty());
    }

    static Promise<FileStat> stat(FileDescriptor fd, Set<StatFlag> flags, Set<StatMask> mask, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.stat(promise::resolve, fd, flags, mask, timeout));
    }

    static Promise<FileStat> stat(FileDescriptor fd, Set<StatFlag> flags, Set<StatMask> mask) {
        return stat(fd, flags, mask, empty());
    }

    static Promise<SizeT> readVector(FileDescriptor fd, OffsetT offset, Option<Timeout> timeout, OffHeapSlice... buffers) {
        return Promise.promise((promise, proactor) -> proactor.readVector(promise::resolve, fd, offset, timeout, buffers));
    }

    static Promise<SizeT> readVector(FileDescriptor fd, OffsetT offset, OffHeapSlice... buffers) {
        return readVector(fd, offset, empty(), buffers);
    }

    static Promise<SizeT> readVector(FileDescriptor fd, Option<Timeout> timeout, OffHeapSlice... buffers) {
        return readVector(fd, OffsetT.ZERO, timeout, buffers);
    }

    static Promise<SizeT> readVector(FileDescriptor fd, OffHeapSlice... buffers) {
        return readVector(fd, OffsetT.ZERO, empty(), buffers);
    }

    static Promise<SizeT> writeVector(FileDescriptor fd, OffsetT offset, Option<Timeout> timeout, OffHeapSlice... buffers) {
        return Promise.promise((promise, proactor) -> proactor.writeVector(promise::resolve, fd, offset, timeout, buffers));
    }

    static Promise<SizeT> writeVector(FileDescriptor fd, OffsetT offset, OffHeapSlice... buffers) {
        return writeVector(fd, offset, empty(), buffers);
    }

    static Promise<SizeT> writeVector(FileDescriptor fd, Option<Timeout> timeout, OffHeapSlice... buffers) {
        return writeVector(fd, OffsetT.ZERO, timeout, buffers);
    }

    static Promise<SizeT> writeVector(FileDescriptor fd, OffHeapSlice... buffers) {
        return writeVector(fd, OffsetT.ZERO, empty(), buffers);
    }

    static Promise<SizeT> readFixed(FileDescriptor fd, FixedBuffer fixedBuffer, OffsetT offset, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.readFixed(promise::resolve, fd, fixedBuffer, offset, timeout));
    }

    static Promise<SizeT> readFixed(FileDescriptor fd, FixedBuffer fixedBuffer, Option<Timeout> timeout) {
        return readFixed(fd, fixedBuffer, OffsetT.ZERO, timeout);
    }

    static Promise<SizeT> readFixed(FileDescriptor fd, FixedBuffer fixedBuffer, OffsetT offset) {
        return readFixed(fd, fixedBuffer, offset, empty());
    }

    static Promise<SizeT> readFixed(FileDescriptor fd, FixedBuffer fixedBuffer) {
        return readFixed(fd, fixedBuffer, OffsetT.ZERO, empty());
    }

    static Promise<SizeT> writeFixed(FileDescriptor fd, FixedBuffer fixedBuffer, OffsetT offset, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.writeFixed(promise::resolve, fd, fixedBuffer, offset, timeout));
    }

    static Promise<SizeT> writeFixed(FileDescriptor fd, FixedBuffer fixedBuffer, Option<Timeout> timeout) {
        return writeFixed(fd, fixedBuffer, OffsetT.ZERO, timeout);
    }

    static Promise<SizeT> writeFixed(FileDescriptor fd, FixedBuffer fixedBuffer, OffsetT offset) {
        return writeFixed(fd, fixedBuffer, offset, empty());
    }

    static Promise<SizeT> writeFixed(FileDescriptor fd, FixedBuffer fixedBuffer) {
        return writeFixed(fd, fixedBuffer, OffsetT.ZERO, empty());
    }

    static Promise<Unit> fsync(FileDescriptor fd, boolean syncMetadata, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.fsync(promise::resolve, fd, syncMetadata, timeout));
    }

    static Promise<Unit> fsync(FileDescriptor fd, boolean syncMetadata) {
        return fsync(fd, syncMetadata, empty());
    }

    static Promise<Unit> falloc(FileDescriptor fd, Set<FileAllocFlags> flags, long offset, long len, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.falloc(promise::resolve, fd, flags, offset, len, timeout));
    }

    static Promise<Unit> falloc(FileDescriptor fd, Set<FileAllocFlags> flags, long offset, long len) {
        return falloc(fd, flags, offset, len, empty());
    }
}
