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

package org.pragmatica.io.async.uring;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.exchange.ExchangeEntry;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn;
import org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn6;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.LibraryLoader;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.pragmatica.io.async.SystemError.ENOTSOCK;
import static org.pragmatica.io.async.SystemError.result;
import static org.pragmatica.io.async.uring.RegisterOperation.IORING_REGISTER_BUFFERS;
import static org.pragmatica.lang.Result.success;

/**
 * Low-level IO URING API
 */
public class UringApi implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(UringApi.class);

    public static final int MIN_QUEUE_SIZE = 128;

    private final int threshold;
    private final SQEntry sqEntry;
    private final IoUringData ioUring;

    private boolean closed = false;
    private int count = 0;
    private int inFlight = 0;

    static {
        try {
            LibraryLoader.fromJar("/liburingnative.so");
        } catch (final Exception e) {
            LOG.error("Error while loading JNI library for UringApi class: ", e);
            System.exit(-1);
        }
    }

    //------------------------------------------------------------------------------------------------
    // Native IO_URING API & helpers
    //------------------------------------------------------------------------------------------------
    // Start/Stop
    static native int init(int numEntries, long baseAddress);
    static native void close(long baseAddress);

    // System calls
    static native long enter(int fd, long toSubmit, long minComplete, int flags);
    static native int register(int fd, int op, long arg1, long arg2);

    // Socket API
    /**
     * Create socket. This call is a combination of socket(2) and setsockopt(2).
     *
     * @param domain  Socket domain. Refer to {@link AddressFamily} for set of recognized values.
     * @param type    Socket type and open flags. Refer to {@link SocketType} for possible types. The {@link SocketFlag} flags can be OR-ed if
     *                necessary.
     * @param options Socket option1s. Only subset of possible options are supported. Refer to {@link SocketOption} for details.
     *
     * @return socket (>0) or error (<0)
     */
    static native int socket(int domain, int type, int options);

    /**
     * Configure socket for listening at specified address, port and with specified depth of backlog queue. It's a combination of bind(2) and
     * listen(2) calls.
     *
     * @param socket     Socket to configure.
     * @param address    Memory address with prepared socket address structure (See {@link RawSocketAddressIn} and {@link RawSocketAddressIn6} for
     *                   more details}.
     * @param len        Size of the prepared socket address structure.
     * @param queueDepth Set backlog queue dept.
     *
     * @return 0 for success and negative value of error code in case of error.
     */
    static native int prepareForListen(int socket, long address, int len, int queueDepth);
    //------------------------------------------------------------------------------------------------

    private UringApi(IoUringData ioUring) {
        this.ioUring = ioUring;
        this.threshold = ioUring.numEntries() - 2;
        this.sqEntry = SQEntry.at(0);
    }

    public static Result<UringApi> uringApi(int requestedEntries, Set<UringSetupFlags> openFlags) {
        return uringApi(requestedEntries, openFlags, 0);
    }

    public static Result<UringApi> uringApi(int requestedEntries, Set<UringSetupFlags> openFlags, int workQueueFD) {
        var ioUring = IoUringData.create(requestedEntries, openFlags, workQueueFD);
        var rc = ioUring.init();

        if (rc != 0) {
            ioUring.dispose();
            return SystemError.fromCode(rc).result();
        }

        return success(new UringApi(ioUring));
    }

    int register(RegisterOperation op, long arg1, long arg2) {
        return register(ioUring.fd(), op.ordinal(), arg1, arg2);
    }

    public Result<OffHeapSlice[]> registerBuffers(OffHeapSlice... buffers) {
        var vector = OffHeapIoVector.withReadBuffers(buffers);

        try {
            int rc = register(IORING_REGISTER_BUFFERS, vector.address(), vector.length());

            if (rc < 0) {
                return SystemError.result(rc);
            }

            return Result.success(buffers);
        } finally {
            vector.dispose();
        }
    }

    public Result<Unit> unregisterBuffers() {
        int rc = register(IORING_REGISTER_BUFFERS, 0L, 0L);

        return rc < 0 ? SystemError.result(rc) : Unit.unitResult();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        ioUring.close();
        closed = true;
    }

    public int processCompletions(ObjectHeap<CompletionHandler> pendingCompletions, Proactor proactor) {
        if (inFlight == 0) {
            return 0;
        }

        int completed = ioUring.completionQueue().processCompletions(pendingCompletions, proactor);

        inFlight -= completed;

        return completed;
    }

    public void processSubmissions() {
        if (count > 0) {
            ioUring.submitAndWait(0);
            count = 0;
        }
    }

    public void submit(ExchangeEntry<?> entry) {
        while (true) {
            var sqe = ioUring.submissionQueue().nextSQE();

            if (sqe == 0) {
                ioUring.submitAndWait(0);
                count = 0;
                continue;
            }

            count++;

            sqEntry.reposition(sqe);
            entry.apply(sqEntry.clear());
            inFlight++;
            break;
        }

        if (count >= threshold) {
            ioUring.submitAndWait(0);
            count = 0;
        }
    }

    public int numEntries() {
        return ioUring.numEntries();
    }

    public int fd() {
        return ioUring.fd();
    }

    public static Result<FileDescriptor> socket(AddressFamily af, SocketType type, Set<SocketFlag> flags, Set<SocketOption> options) {
        return result(socket(af.familyId(), type.code() | Bitmask.combine(flags), Bitmask.combine(options)),
                      (af == AddressFamily.INET6) ? FileDescriptor::socket6 : FileDescriptor::socket);
    }

    public static <T extends InetAddress> Result<ListenContext<T>> listen(SocketAddress<T> address, SocketType type, Set<SocketFlag> flags,
                                                                          Set<SocketOption> options, SizeT queueLen) {
        var len = (int) queueLen.value();

        return socket(address.family(), type, flags, options)
            .flatMap(fd -> configureForListen(fd, address, len))
            .map(fd -> ListenContext.listenContext(fd, address, len));
    }

    private static <T extends InetAddress> Result<FileDescriptor> configureForListen(FileDescriptor fd, SocketAddress<T> address, int queueLen) {
        if (!fd.isSocket()) {
            return ENOTSOCK.result();
        }

        var offHeapAddress = OffHeapSocketAddress.unsafeSocketAddress(address);
        var rc = prepareForListen(fd.descriptor(), offHeapAddress.sockAddrPtr(), offHeapAddress.sockAddrSize(), queueLen);

        return result(rc, __ -> fd);
    }
}
