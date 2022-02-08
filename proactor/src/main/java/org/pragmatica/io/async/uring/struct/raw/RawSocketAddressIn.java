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

package org.pragmatica.io.async.uring.struct.raw;

import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.SocketAddress;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.SocketAddressInOffsets;
import org.pragmatica.lang.Result;

import static org.pragmatica.io.async.net.AddressFamily.addressFamily;
import static org.pragmatica.io.async.net.InetAddress.inet4Address;
import static org.pragmatica.io.async.net.InetPort.inetPort;
import static org.pragmatica.lang.Result.success;

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
        return getIntInNetOrder(SocketAddressInOffsets.sin_addr);
    }

    public RawSocketAddressIn inetAddress(int address) {
        return putIntInNetOrder(SocketAddressInOffsets.sin_addr, address);
    }

    public short port() {
        return getShortInNetOrder(SocketAddressInOffsets.sin_port);
    }

    public RawSocketAddressIn port(short port) {
        return putShortInNetOrder(SocketAddressInOffsets.sin_port, port);
    }

    public short family() {
        return getShort(SocketAddressInOffsets.sin_family);
    }

    public RawSocketAddressIn family(short family) {
        return putShort(SocketAddressInOffsets.sin_family, family);
    }

    @Override
    public void assign(SocketAddress<Inet4Address> input) {
        family(input.family().familyId());
        port(input.port().port());
        putBytes(SocketAddressInOffsets.sin_addr, input.address().asBytes());
    }

    @Override
    public Result<SocketAddress<Inet4Address>> extract() {
        return Result.all(addressFamily(family()),
                          success(inetPort(port())),
                          inet4Address(getBytes(SocketAddressInOffsets.sin_addr)))
                     .map(SocketAddress::socketAddress);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RawSocketAddressIn shape() {
        return this;
    }
}
