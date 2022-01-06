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

import org.pfj.io.async.net.Inet4Address;
import org.pfj.io.async.net.SocketAddress;
import org.pfj.io.async.uring.struct.AbstractExternalRawStructure;
import org.pfj.io.async.uring.struct.shape.SocketAddressInOffsets;
import org.pfj.lang.Result;

import static org.pfj.io.async.net.AddressFamily.addressFamily;
import static org.pfj.io.async.net.Inet4Address.inet4Address;
import static org.pfj.io.async.net.InetPort.inetPort;
import static org.pfj.io.async.uring.struct.shape.SocketAddressInOffsets.*;
import static org.pfj.lang.Result.success;

public final class RawSocketAddressIn
    extends AbstractExternalRawStructure<RawSocketAddressIn>
    implements RawSocketAddress<Inet4Address> {

    private RawSocketAddressIn(long address) {
        super(address, SocketAddressInOffsets.SIZE);
    }

    public static RawSocketAddressIn at(long address) {
        return new RawSocketAddressIn(address);
    }

    public int inetAddress() {
        return getIntInNetOrder(sin_addr);
    }

    public RawSocketAddressIn inetAddress(int address) {
        return putIntInNetOrder(sin_addr, address);
    }

    public short port() {
        return getShortInNetOrder(sin_port);
    }

    public RawSocketAddressIn port(short port) {
        return putShortInNetOrder(sin_port, port);
    }

    public short family() {
        return getShort(sin_family);
    }

    public RawSocketAddressIn family(short family) {
        return putShort(sin_family, family);
    }

    @Override
    public void assign(SocketAddress<Inet4Address> input) {
        family(input.family().familyId());
        port(input.port().port());
        putBytes(sin_addr, input.address().asBytes());
    }

    @Override
    public Result<SocketAddress<Inet4Address>> extract() {
        return Result.all(addressFamily(family()),
                          success(inetPort(port())),
                          inet4Address(getBytes(sin_addr)))
                     .map(SocketAddress::socketAddress);
    }

    @Override
    public RawSocketAddressIn shape() {
        return this;
    }
}
