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
 *
 */

package org.pragmatica.io.async.net;

import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.InetAddress.Inet6Address;

/**
 * Generic IP socket address.
 */
public sealed interface SocketAddress<T extends InetAddress> {
    AddressFamily family();

    InetPort port();

    T address();

    static SocketAddress<Inet4Address> socketAddress(InetPort port, Inet4Address address) {
        return socketAddress(AddressFamily.INET, port, address);
    }

    static SocketAddress<Inet4Address> socketAddress(AddressFamily addressFamily, InetPort port, Inet4Address address) {
        return new SocketAddressIn(addressFamily, port, address);
    }

    static SocketAddress<Inet6Address> socketAddress(InetPort port, Inet6Address address) {
        return socketAddress(AddressFamily.INET6, port, address, Inet6FlowInfo.inet6FlowInfo(0), Inet6ScopeId.inet6ScopeId(0));
    }

    static SocketAddress<Inet6Address> socketAddress(AddressFamily family, InetPort port, Inet6Address address) {
        return socketAddress(family, port, address, Inet6FlowInfo.inet6FlowInfo(0), Inet6ScopeId.inet6ScopeId(0));
    }

    static SocketAddress<Inet6Address> socketAddress(AddressFamily family, InetPort port, Inet6Address address,
                                                     Inet6FlowInfo flowInfo, Inet6ScopeId scopeId) {
        return new SocketAddressIn6(family, port, address, flowInfo, scopeId);
    }

    @SuppressWarnings("unchecked")
    static <R extends InetAddress> SocketAddress<R> genericAddress(InetPort port, R address) {
        return switch (address) {
            case Inet4Address inet4Address -> (SocketAddress<R>) socketAddress(port, inet4Address);
            case Inet6Address inet6Address -> (SocketAddress<R>) socketAddress(port, inet6Address);
            default -> throw new IllegalStateException("Unexpected value: " + address);
        };
    }

    /**
     * Socket Address for IPv4.
     */
    record SocketAddressIn(AddressFamily family, InetPort port, Inet4Address address) implements SocketAddress<Inet4Address> {}

    /**
     * Socket Address for IPv6.
     */
    record SocketAddressIn6(AddressFamily family, InetPort port, Inet6Address address,
                            Inet6FlowInfo flowInfo, Inet6ScopeId scopeId) implements SocketAddress<Inet6Address> {}

}
