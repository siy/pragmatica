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

package org.pragmatica.io.async.net;

import org.pragmatica.lang.Result;

import static org.pragmatica.io.async.SystemError.EFAULT;

/**
 * Generic IP address.
 */
public sealed interface InetAddress {
    /**
     * Get byte representation of address in network byte order.
     *
     * @return byte representation of the address.
     */
    byte[] asBytes();

    /**
     * Convert byte representation of the IPv4 address into the address.
     *
     * @param address The byte representation of the address.
     *
     * @return Result of conversion of the address.
     */
    static Result<Inet4Address> inet4Address(final byte[] address) {
        return address.length != Inet4Address.SIZE
               ? EFAULT.result()
               : Result.success(new Inet4Address(address));
    }

    /**
     * Convert byte representation of the IPv6 address into the address.
     *
     * @param address The byte representation of the address.
     *
     * @return Result of conversion of the address.
     */
    static Result<Inet6Address> inet6Address(final byte[] address) {
        return address.length != Inet6Address.SIZE
               ? EFAULT.result()
               : Result.success(new Inet6Address(address));
    }

    /**
     * IPv4 address implementation.
     */
    record Inet4Address(byte[] asBytes) implements InetAddress {
        public static final int SIZE = 4;
        public static final Inet4Address INADDR_ANY = new Inet4Address(new byte[SIZE]);

        @Override
        public String toString() {
            return String.format("Inet4Address(%d.%d.%d.%d)",
                                 (int) asBytes[0] & 0xFF, (int) asBytes[1] & 0xFF,
                                 (int) asBytes[2] & 0xFF, (int) asBytes[3] & 0xFF);
        }
    }

    /**
     * IPv6 address implementation.
     */
    record Inet6Address(byte[] asBytes) implements InetAddress {
        public static final int SIZE = 16;
        public static final Inet6Address INADDR_ANY = new Inet6Address(new byte[SIZE]);

        @Override
        public String toString() {
            return String.format("Inet6Address(%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x)",
                                 (int) asBytes[0] & 0xFF, (int) asBytes[1] & 0xFF,
                                 (int) asBytes[2] & 0xFF, (int) asBytes[3] & 0xFF,
                                 (int) asBytes[4] & 0xFF, (int) asBytes[5] & 0xFF,
                                 (int) asBytes[6] & 0xFF, (int) asBytes[7] & 0xFF,
                                 (int) asBytes[8] & 0xFF, (int) asBytes[9] & 0xFF,
                                 (int) asBytes[10] & 0xFF, (int) asBytes[11] & 0xFF,
                                 (int) asBytes[12] & 0xFF, (int) asBytes[13] & 0xFF,
                                 (int) asBytes[14] & 0xFF, (int) asBytes[15] & 0xFF);
        }
    }
}
