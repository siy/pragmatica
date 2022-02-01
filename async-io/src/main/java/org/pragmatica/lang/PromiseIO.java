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
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.OpenFlags;
import org.pragmatica.io.async.file.SpliceDescriptor;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.file.stat.StatFlag;
import org.pragmatica.io.async.file.stat.StatMask;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.InetAddress.Inet6Address;
import org.pragmatica.io.async.util.OffHeapBuffer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.pragmatica.lang.Option.empty;

/**
 * "Promisified" Proactor I/O API.
 */
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
     * completion, {@link OffHeapBuffer#used()} value is set to actual number of bytes read. Number of read bytes also used to resolve returned
     * promise.
     *
     * @param fd      File descriptor or socket
     * @param buffer  Buffer to store read data
     * @param offset  Offset in the source file if file descriptor points to file. Use {@link OffsetT#ZERO} if file descriptor belongs to socket or
     *                pipe
     * @param timeout Operation timeout
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapBuffer buffer, OffsetT offset, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.read(promise::resolve, fd, buffer, offset, timeout));
    }

    /**
     * Same as {@link #read(FileDescriptor, OffHeapBuffer, OffsetT, Option)}, but no timeout specified.
     *
     * @param fd     File descriptor or socket
     * @param buffer Buffer to store read data
     * @param offset Offset in the source file if file descriptor points to file. Use {@link OffsetT#ZERO} if file descriptor belongs to socket or
     *               pipe
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapBuffer buffer, OffsetT offset) {
        return read(fd, buffer, offset, empty());
    }

    /**
     * Same as {@link #read(FileDescriptor, OffHeapBuffer, OffsetT, Option)}, but no offset needs to be provided. Convenient for use with sockets or
     * pipes.
     *
     * @param fd      File descriptor or socket
     * @param buffer  Buffer to store read data
     * @param timeout Operation timeout
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapBuffer buffer, Option<Timeout> timeout) {
        return read(fd, buffer, OffsetT.ZERO, timeout);
    }

    /**
     * Same as {@link #read(FileDescriptor, OffHeapBuffer, Option)}, but no timeout is specified.
     *
     * @param fd      File descriptor or socket
     * @param buffer  Buffer to store read data
     *
     * @return a {@link Promise} instance, which is resolved with number of bytes read once operations is finished successfully.
     */
    static Promise<SizeT> read(FileDescriptor fd, OffHeapBuffer buffer) {
        return read(fd, buffer, OffsetT.ZERO, empty());
    }

    //TODO: finish documentation

    static Promise<SizeT> write(FileDescriptor fd, OffHeapBuffer buffer, OffsetT offset, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.write(promise::resolve, fd, buffer, offset, timeout));
    }

    static Promise<SizeT> write(FileDescriptor fd, OffHeapBuffer buffer, OffsetT offset) {
        return write(fd, buffer, offset, empty());
    }

    static Promise<SizeT> write(FileDescriptor fd, OffHeapBuffer buffer, Option<Timeout> timeout) {
        return write(fd, buffer, OffsetT.ZERO, timeout);
    }

    static Promise<SizeT> write(FileDescriptor fd, OffHeapBuffer buffer) {
        return write(fd, buffer, OffsetT.ZERO, empty());
    }

    static Promise<Unit> close(FileDescriptor fd, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.close(promise::resolve, fd, timeout));
    }

    static Promise<Unit> close(FileDescriptor fd) {
        return close(fd, empty());
    }

    static Promise<FileDescriptor> open(Path path, Set<OpenFlags> flags, Set<FilePermission> mode, Option<Timeout> timeout) {
        return Promise.promise((promise, proactor) -> proactor.open(promise::resolve, path, flags, mode, timeout));
    }

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

    static Promise<SizeT> read(FileDescriptor fd, OffsetT offset, Option<Timeout> timeout, OffHeapBuffer... buffers) {
        return Promise.promise((promise, proactor) -> proactor.read(promise::resolve, fd, offset, timeout, buffers));
    }

    static Promise<SizeT> read(FileDescriptor fd, OffsetT offset, OffHeapBuffer... buffers) {
        return read(fd, offset, empty(), buffers);
    }

    static Promise<SizeT> write(FileDescriptor fd, OffsetT offset, Option<Timeout> timeout, OffHeapBuffer... buffers) {
        return Promise.promise((promise, proactor) -> proactor.write(promise::resolve, fd, offset, timeout, buffers));
    }

    static Promise<SizeT> write(FileDescriptor fd, OffsetT offset, OffHeapBuffer... buffers) {
        return write(fd, offset, empty(), buffers);
    }
}
