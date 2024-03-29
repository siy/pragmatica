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

package org.pragmatica.io.async.uring;

import org.pragmatica.io.async.uring.utils.LibraryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Native interface to Linux IO URING
 */
final class UringNative {
    private static final Logger LOG = LoggerFactory.getLogger(UringNative.class);

    private UringNative() {
    }

    public static final int SIZE = 256;

    private static MethodHandle initHandle;
    private static MethodHandle exitHandle;
    private static MethodHandle copyCQESHandle;
    private static MethodHandle submitHandle;
    private static MethodHandle registerHandle;
    private static MethodHandle socketHandle;
    private static MethodHandle listenHandle;

    static {
        try {
            LibraryLoader.fromJar("/liburingnative.so");

            var lookup = SymbolLookup.loaderLookup();

            initHandle = prepare(lookup, "ring_open", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_INT));
            exitHandle = prepare(lookup, "ring_close", FunctionDescriptor.ofVoid(JAVA_LONG));
            copyCQESHandle = prepare(lookup, "ring_copy_cqes", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT));
            submitHandle = prepare(lookup, "ring_direct_submit", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT));
            registerHandle = prepare(lookup, "ring_register", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_LONG));
            socketHandle = prepare(lookup, "ring_socket", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
            listenHandle = prepare(lookup, "ring_listen", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT));
        } catch (final Exception e) {
            LOG.error("Error while loading native library: ", e);
            System.exit(-1);
        }
    }

    private static MethodHandle prepare(SymbolLookup lookup, String symbol, FunctionDescriptor descriptor) {
        var address = lookup.find(symbol)
                            .orElseThrow(IllegalStateException::new);

        return Linker.nativeLinker()
                     .downcallHandle(address, descriptor, Linker.Option.isTrivial());
    }

    public static int init(int num_entries, long base_address, int flags) {
        try {
            return (int) initHandle.invokeExact(num_entries, base_address, flags);
        } catch (Throwable e) {
            LOG.error("Attempt to invoke method init failed", e);
            throw new RuntimeException(e);
        }
    }

    public static void exit(long base_address) {
        try {
            exitHandle.invokeExact(base_address);
        } catch (Throwable e) {
            LOG.error("Attempt to invoke method exit failed", e);
            throw new RuntimeException(e);
        }
    }

    public static int copyCQES(long base_address, long completions_address, int count) {
        try {
            return (int) copyCQESHandle.invokeExact(base_address, completions_address, count);
        } catch (Throwable e) {
            LOG.error("Attempt to invoke method peek_batch_cqe failed", e);
            throw new RuntimeException(e);
        }
    }

    public static int submit(long base_address, long entries_address, int count, int flags) {
        try {
            return (int) submitHandle.invokeExact(base_address, entries_address, count, flags);
        } catch (Throwable e) {
            LOG.error("Attempt to invoke method direct_submit failed", e);
            throw new RuntimeException(e);
        }
    }

    public static int register(long base_address, int opcode, long arg, long nr_args) {
        try {
            return (int) registerHandle.invokeExact(base_address, opcode, arg, nr_args);
        } catch (Throwable e) {
            LOG.error("Attempt to invoke method register failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Create socket. This call is a combination of socket(2) and setsockopt(2).
     *
     * @param domain  Socket domain. Refer to {@link org.pragmatica.io.async.net.AddressFamily} for set of recognized values.
     * @param type    Socket type and open flags. Refer to {@link org.pragmatica.io.async.net.SocketType} for possible types. The
     *                {@link org.pragmatica.io.async.net.SocketFlag} flags can be OR-ed if necessary.
     * @param options Socket option1s. Only subset of possible options are supported. Refer to {@link org.pragmatica.io.async.net.SocketOption} for
     *                details.
     *
     * @return socket (>0) or error (<0)
     */
    public static int socket(int domain, int type, int options) {
        try {
            return (int) socketHandle.invokeExact(domain, type, options);
        } catch (Throwable e) {
            LOG.error("Attempt to invoke method socket failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure socket for listening at specified address, port and with specified depth of backlog queue. It's a combination of bind(2) and
     * listen(2) calls.
     *
     * @param socket     Socket to configure.
     * @param address    Memory address with prepared socket address structure (See
     *                   {@link org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn} and
     *                   {@link org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn6} for more details}.
     * @param len        Size of the prepared socket address structure.
     * @param queueDepth Set backlog queue dept.
     *
     * @return 0 for success and negative value of error code in case of error.
     */
    public static int listen(int socket, long address, int len, int queueDepth) {
        try {
            return (int) listenHandle.invokeExact(socket, address, len, queueDepth);
        } catch (Throwable e) {
            LOG.error("Attempt to invoke method listen failed", e);
            throw new RuntimeException(e);
        }
    }
}
