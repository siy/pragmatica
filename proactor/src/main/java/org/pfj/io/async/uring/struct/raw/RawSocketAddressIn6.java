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

package org.pfj.io.async.uring.struct.raw;

import org.pfj.io.async.net.SocketAddressIn6;
import org.pfj.io.async.uring.struct.AbstractExternalRawStructure;
import org.pfj.io.async.uring.struct.shape.SocketAddressIn6Offsets;
import org.pfj.lang.Result;

import static org.pfj.io.async.net.AddressFamily.addressFamily;
import static org.pfj.io.async.net.Inet6Address.inet6Address;
import static org.pfj.io.async.net.Inet6FlowInfo.inet6FlowInfo;
import static org.pfj.io.async.net.Inet6ScopeId.inet6ScopeId;
import static org.pfj.io.async.net.InetPort.inetPort;
import static org.pfj.io.async.uring.struct.shape.SocketAddressIn6Offsets.sin6_addr;
import static org.pfj.io.async.uring.struct.shape.SocketAddressIn6Offsets.sin6_family;
import static org.pfj.io.async.uring.struct.shape.SocketAddressIn6Offsets.sin6_flowinfo;
import static org.pfj.io.async.uring.struct.shape.SocketAddressIn6Offsets.sin6_port;
import static org.pfj.io.async.uring.struct.shape.SocketAddressIn6Offsets.sin6_scope_id;
import static org.pfj.lang.Result.success;

public class RawSocketAddressIn6 extends AbstractExternalRawStructure<RawSocketAddressIn6>
    implements RawSocketAddress<SocketAddressIn6, RawSocketAddressIn6> {

    protected RawSocketAddressIn6(final long address) {
        super(address, SocketAddressIn6Offsets.SIZE);
    }

    public static RawSocketAddressIn6 at(final int address) {
        return new RawSocketAddressIn6(address);
    }

    public short family() {
        return getShort(sin6_family);
    }

    public short port() {
        return getShortInNetOrder(sin6_port);
    }

    public int flowinfo() {
        return getInt(sin6_flowinfo);
    }

    public int scopeId() {
        return getInt(sin6_scope_id);
    }

    public byte[] addr() {
        return getBytes(sin6_addr);
    }

    public RawSocketAddressIn6 family(final short family) {
        putShort(sin6_family, family);
        return this;
    }

    public RawSocketAddressIn6 port(final short port) {
        putShortInNetOrder(sin6_port, port);
        return this;
    }

    public RawSocketAddressIn6 flowinfo(final int flowinfo) {
        putInt(sin6_flowinfo, flowinfo);
        return this;
    }

    public RawSocketAddressIn6 scopeId(final int scopeId) {
        putInt(sin6_scope_id, scopeId);
        return this;
    }

    public RawSocketAddressIn6 addr(final byte[] addr) {
        putBytes(sin6_addr, addr);
        return this;
    }

    @Override
    public void assign(final SocketAddressIn6 input) {
        family(input.family().familyId());
        port(input.port().port());
        addr(input.address().asBytes());
        flowinfo(input.flowInfo().value());
        scopeId(input.scopeId().scopeId());
    }

    @Override
    public Result<SocketAddressIn6> extract() {
        return Result.all(addressFamily(family()),
                success(inetPort(port())),
                inet6Address(addr()),
                success(inet6FlowInfo(flowinfo())),
                success(inet6ScopeId(scopeId())))
            .map(SocketAddressIn6::create);
    }

    @Override
    public RawSocketAddressIn6 shape() {
        return this;
    }
}
