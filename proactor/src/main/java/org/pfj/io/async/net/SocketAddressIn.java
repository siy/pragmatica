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

package org.pfj.io.async.net;

import java.util.StringJoiner;

public class SocketAddressIn implements SocketAddress<Inet4Address> {
    private final AddressFamily family;
    private final InetPort port;
    private final Inet4Address address;

    private SocketAddressIn(AddressFamily family, InetPort port, Inet4Address address) {
        this.family = family;
        this.port = port;
        this.address = address;
    }

    @Override
    public AddressFamily family() {
        return family;
    }

    @Override
    public InetPort port() {
        return port;
    }

    @Override
    public Inet4Address address() {
        return address;
    }

    public static SocketAddressIn create(InetPort port, Inet4Address address) {
        return new SocketAddressIn(AddressFamily.INET, port, address);
    }

    public static SocketAddressIn create(AddressFamily addressFamily, InetPort port, Inet4Address address) {
        return new SocketAddressIn(addressFamily, port, address);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "SocketAddressIn(", ")")
            .add(family.toString())
            .add(port.toString())
            .add(address.toString())
            .toString();
    }
}
