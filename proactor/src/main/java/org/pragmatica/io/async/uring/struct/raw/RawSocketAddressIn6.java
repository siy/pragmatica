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

package org.pragmatica.io.async.uring.struct.raw;

import org.pragmatica.io.async.net.InetAddress.Inet6Address;
import org.pragmatica.io.async.net.SocketAddress;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.SocketAddressIn6Offsets;
import org.pragmatica.lang.Result;

import static org.pragmatica.io.async.net.AddressFamily.addressFamily;
import static org.pragmatica.io.async.net.Inet6FlowInfo.inet6FlowInfo;
import static org.pragmatica.io.async.net.Inet6ScopeId.inet6ScopeId;
import static org.pragmatica.io.async.net.InetAddress.inet6Address;
import static org.pragmatica.io.async.net.InetPort.inetPort;
import static org.pragmatica.lang.Result.success;

/**
 * IPv6 socket address storage.
 */
public final class RawSocketAddressIn6 extends AbstractExternalRawStructure<RawSocketAddressIn6>
    implements RawSocketAddress<Inet6Address> {

    private RawSocketAddressIn6(long address) {
        super(address, SocketAddressIn6Offsets.SIZE);
    }

    public static RawSocketAddressIn6 at(int address) {
        return new RawSocketAddressIn6(address);
    }

    public short family() {
        return getShort(SocketAddressIn6Offsets.sin6_family);
    }

    public short port() {
        return getShortInNetOrder(SocketAddressIn6Offsets.sin6_port);
    }

    public int flowInfo() {
        return getInt(SocketAddressIn6Offsets.sin6_flowinfo);
    }

    public int scopeId() {
        return getInt(SocketAddressIn6Offsets.sin6_scope_id);
    }

    public byte[] addr() {
        return getBytes(SocketAddressIn6Offsets.sin6_addr);
    }

    public RawSocketAddressIn6 family(short family) {
        putShort(SocketAddressIn6Offsets.sin6_family, family);
        return this;
    }

    public RawSocketAddressIn6 port(short port) {
        putShortInNetOrder(SocketAddressIn6Offsets.sin6_port, port);
        return this;
    }

    public RawSocketAddressIn6 flowInfo(int flowInfo) {
        putInt(SocketAddressIn6Offsets.sin6_flowinfo, flowInfo);
        return this;
    }

    public RawSocketAddressIn6 scopeId(int scopeId) {
        putInt(SocketAddressIn6Offsets.sin6_scope_id, scopeId);
        return this;
    }

    public RawSocketAddressIn6 addr(byte[] addr) {
        putBytes(SocketAddressIn6Offsets.sin6_addr, addr);
        return this;
    }

    @Override
    public void assign(SocketAddress<Inet6Address> input) {
        family(input.family().familyId());
        port(input.port().port());
        addr(input.address().asBytes());

        if (input instanceof SocketAddress.SocketAddressIn6 in6) {
            flowInfo(in6.flowInfo().value());
            scopeId(in6.scopeId().scopeId());
        }
    }

    @Override
    public Result<SocketAddress<Inet6Address>> extract() {
        return Result.all(addressFamily(family()),
                          success(inetPort(port())),
                          inet6Address(addr()),
                          success(inet6FlowInfo(flowInfo())),
                          success(inet6ScopeId(scopeId())))
                     .map(SocketAddress::socketAddress);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RawSocketAddressIn6 shape() {
        return this;
    }
}
