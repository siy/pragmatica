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

import org.pragmatica.io.async.net.AddressFamily;
import org.pragmatica.io.async.net.SocketFlag;
import org.pragmatica.io.async.net.SocketOption;
import org.pragmatica.io.async.net.SocketType;
import org.pragmatica.io.async.uring.struct.raw.IoUring;
import org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn;
import org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn6;
import org.pragmatica.io.async.uring.utils.LibraryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native interface to Linux IO URING
 */
public final class UringNative {
    private static final Logger LOG = LoggerFactory.getLogger(UringNative.class);

    private UringNative() {
    }

    // Actual size of struct io_uring is 160 bytes at the moment of writing: May 2020
    public static final long SIZE = 256;

    static {
        try {
            LibraryLoader.fromJar("/liburingnative.so");
        } catch (final Exception e) {
            LOG.error("Error while loading JNI library for Uring class: ", e);
            System.exit(-1);
        }
    }

    // Start/Stop
    public static native int init(int numEntries, long baseAddress, int flags);
    public static native void close(long baseAddress);

    // Completion
    // note: it also advances CQ
//    public static native int peekCQ(long baseAddress, long completionsAddress, long count);
//    public static native int peekSQEntries(long baseAddress, long submissionsAddress, long count);
//    public static native long peekSQEntry(long baseAddress);
    public static native long submitAndWait(long baseAddress, int waitNr);

    // Syscall
    public static native long enter(long baseAddress, long toSubmit, long minComplete, int flags);

    // Socket API

    /**
     * Create socket. This call is a combination of socket(2) and setsockopt(2).
     *
     * @param domain  Socket domain. Refer to {@link AddressFamily} for set of recognized values.
     * @param type    Socket type and open flags. Refer to {@link SocketType} for possible types. The {@link
     *                SocketFlag} flags can be OR-ed if necessary.
     * @param options Socket option1s. Only subset of possible options are supported. Refer to {@link SocketOption} for details.
     *
     * @return socket (>0) or error (<0)
     */
    public static native int socket(int domain, int type, int options);

    /**
     * Configure socket for listening at specified address, port and with specified depth of backlog queue. It's a combination of bind(2) and
     * listen(2) calls.
     *
     * @param socket     Socket to configure.
     * @param address    Memory address with prepared socket address structure (See {@link RawSocketAddressIn} and
     *                   {@link RawSocketAddressIn6} for more details}.
     * @param len        Size of the prepared socket address structure.
     * @param queueDepth Set backlog queue dept.
     *
     * @return 0 for success and negative value of error code in case of error.
     */
    public static native int prepareForListen(int socket, long address, int len, int queueDepth);
}
