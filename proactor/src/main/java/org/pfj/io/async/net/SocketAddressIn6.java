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

//TODO: toString
public class SocketAddressIn6 implements SocketAddress<Inet6Address> {
    private final AddressFamily family;
    private final InetPort port;
    private final Inet6Address address;
    private final Inet6FlowInfo flowInfo;
    private final Inet6ScopeId scopeId;

    private SocketAddressIn6(final AddressFamily family,
                             final InetPort port,
                             final Inet6Address address,
                             final Inet6FlowInfo flowInfo,
                             final Inet6ScopeId scopeId) {
        this.family = family;
        this.port = port;
        this.address = address;
        this.flowInfo = flowInfo;
        this.scopeId = scopeId;
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
    public Inet6Address address() {
        return address;
    }

    public Inet6FlowInfo flowInfo() {
        return flowInfo;
    }

    public Inet6ScopeId scopeId() {
        return scopeId;
    }

    public static SocketAddressIn6 create(final AddressFamily family,
                                          final InetPort port,
                                          final Inet6Address address,
                                          final Inet6FlowInfo flowInfo,
                                          final Inet6ScopeId scopeId) {
        return new SocketAddressIn6(family, port, address, flowInfo, scopeId);
    }
}
