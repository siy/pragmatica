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

package org.pfj.io.async.uring;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import org.pfj.io.async.uring.utils.LibraryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;

import static java.lang.invoke.MethodType.methodType;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;

/**
 * Native interface to Linux IO URING
 */
final class UringNative {
    private static final Logger LOG = LoggerFactory.getLogger(UringNative.class);

    private UringNative() {
    }

    // Actual size of struct io_uring is 160 bytes at the moment of writing: May 2020
    public static final long SIZE = 256;

    private static MethodHandle initHandle;
    private static MethodHandle closeHandle;
    private static MethodHandle peekCQHandle;
    private static MethodHandle advanceCQHandle;
    private static MethodHandle nextSQEntryHandle;
    private static MethodHandle peekSQEntriesHandle;
    private static MethodHandle submitAndWaitHandle;

    private static MethodHandle socketHandle;
    private static MethodHandle prepareForListenHandle;

    static {
        try {
            LibraryLoader.fromJar("/liburingnative.so");

            var lookup = SymbolLookup.loaderLookup();

            var initAddress = lookup.lookup("native_init").orElseThrow(IllegalStateException::new);
            var closeAddress = lookup.lookup("native_close").orElseThrow(IllegalStateException::new);
            var peekCQAddress = lookup.lookup("native_peekCQ").orElseThrow(IllegalStateException::new);
            var advanceCQAddress = lookup.lookup("native_advanceCQ").orElseThrow(IllegalStateException::new);
            var nextSQEntryAddress = lookup.lookup("native_nextSQEntry").orElseThrow(IllegalStateException::new);
            var peekSQEntriesAddress = lookup.lookup("native_peekSQEntries").orElseThrow(IllegalStateException::new);
            var submitAndWaitAddress = lookup.lookup("native_submitAndWait").orElseThrow(IllegalStateException::new);
            var socketAddress = lookup.lookup("native_socket").orElseThrow(IllegalStateException::new);
            var prepareForListenAddress = lookup.lookup("native_prepareForListen").orElseThrow(IllegalStateException::new);

            var linker = CLinker.getInstance();

            initHandle = linker.downcallHandle(initAddress, methodType(int.class, int.class, long.class, int.class), FunctionDescriptor.of(C_INT, C_INT, C_LONG, C_INT));
            closeHandle = linker.downcallHandle(closeAddress, methodType(void.class, long.class), FunctionDescriptor.ofVoid(C_LONG));
            peekCQHandle = linker.downcallHandle(peekCQAddress, methodType(int.class, long.class, long.class, long.class), FunctionDescriptor.of(C_INT, C_LONG, C_LONG, C_LONG));
            advanceCQHandle = linker.downcallHandle(advanceCQAddress, methodType(void.class, long.class, long.class), FunctionDescriptor.ofVoid(C_LONG, C_LONG));
            nextSQEntryHandle = linker.downcallHandle(nextSQEntryAddress, methodType(long.class, long.class), FunctionDescriptor.of(C_LONG, C_LONG));
            peekSQEntriesHandle = linker.downcallHandle(peekSQEntriesAddress, methodType(int.class, long.class, long.class, long.class), FunctionDescriptor.of(C_INT, C_LONG, C_LONG, C_LONG));
            submitAndWaitHandle = linker.downcallHandle(submitAndWaitAddress, methodType(long.class, long.class, int.class), FunctionDescriptor.of(C_LONG, C_LONG, C_INT));
            socketHandle = linker.downcallHandle(socketAddress, methodType(int.class, int.class, int.class, int.class), FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT));
            prepareForListenHandle = linker.downcallHandle(prepareForListenAddress, methodType(int.class, int.class, long.class, int.class, int.class), FunctionDescriptor.of(C_INT, C_INT, C_LONG, C_INT, C_INT));

        } catch (final Exception e) {
            LOG.error("Error while loading JNI library for Uring class: ", e);
            System.exit(-1);
        }
    }

    // Start/Stop
    public static int init(int numEntries, long baseAddress, int flags) {
        try {
            return (int) initHandle.invokeExact(numEntries, baseAddress, flags);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    public static void close(long baseAddress) {
        try {
            closeHandle.invokeExact(baseAddress);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    // Completion
    public static int peekCQ(long baseAddress, long completionsAddress, long count) {
        try {
            return (int) peekCQHandle.invokeExact(baseAddress, completionsAddress, count);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    public static void advanceCQ(long baseAddress, long count) {
        try {
            advanceCQHandle.invokeExact(baseAddress, count);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    // Submissions
    public static long nextSQEntry(long baseAddress) {
        try {
            return (long) nextSQEntryHandle.invokeExact(baseAddress);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    public static int peekSQEntries(long baseAddress, long submissionsAddress, long count) {
        try {
            return (int) peekSQEntriesHandle.invokeExact(baseAddress, submissionsAddress, count);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    public static long submitAndWait(long baseAddress, int waitNr) {
        try {
            return (long) submitAndWaitHandle.invokeExact(baseAddress, waitNr);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    // Socket API

    /**
     * Create socket. This call is a combination of socket(2) and setsockopt(2).
     *
     * @param domain  Socket domain. Refer to {@link org.pfj.io.async.net.AddressFamily} for set of recognized values.
     * @param type    Socket type and open flags. Refer to {@link org.pfj.io.async.net.SocketType} for possible types. The {@link
     *                org.pfj.io.async.net.SocketFlag} flags can be OR-ed if necessary.
     * @param options Socket option1s. Only subset of possible options are supported. Refer to {@link org.pfj.io.async.net.SocketOption} for details.
     *
     * @return socket (>0) or error (<0)
     */
    public static int socket(int domain, int type, int options) {
        try {
            return (int) socketHandle.invokeExact(domain, type, options);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure socket for listening at specified address, port and with specified depth of backlog queue. It's a combination of bind(2) and
     * listen(2) calls.
     *
     * @param socket     Socket to configure.
     * @param address    Memory address with prepared socket address structure (See {@link org.pfj.io.async.uring.struct.raw.RawSocketAddressIn} and
     *                   {@link org.pfj.io.async.uring.struct.raw.RawSocketAddressIn6} for more details}.
     * @param len        Size of the prepared socket address structure.
     * @param queueDepth Set backlog queue dept.
     *
     * @return 0 for success and negative value of error code in case of error.
     */
    public static int prepareForListen(int socket, long address, int len, int queueDepth) {
        try {
            return (int) prepareForListenHandle.invokeExact(socket, address, len, queueDepth);
        } catch (Throwable e) {
            LOG.error("Error while invoking method ", e);
            throw new RuntimeException(e);
        }
    }
}
