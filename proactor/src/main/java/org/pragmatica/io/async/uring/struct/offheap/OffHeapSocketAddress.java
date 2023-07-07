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

package org.pragmatica.io.async.uring.struct.offheap;

import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.InetAddress.Inet6Address;
import org.pragmatica.io.async.net.SocketAddress;
import org.pragmatica.io.async.uring.struct.raw.RawSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn;
import org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn6;
import org.pragmatica.io.async.util.raw.RawProperty;
import org.pragmatica.lang.Result;

public class OffHeapSocketAddress extends AbstractOffHeapStructure<OffHeapSocketAddress> {
    private static final int SIZE = 128 + 4;    //Equal to sizeof(struct sockaddr_storage) + sizeof(socklen_t)
    private static final RawProperty sockaddrLen = RawProperty.raw(0, 4);

    @SuppressWarnings("rawtypes")
    private RawSocketAddress shape;
    private final RawSocketAddressIn shapeV4 = RawSocketAddressIn.at(0);
    private final RawSocketAddressIn6 shapeV6 = RawSocketAddressIn6.at(0);

    private OffHeapSocketAddress(boolean v6) {
        super(SIZE);
        protocolVersion(v6);
    }

    public final void protocolVersion(boolean v6) {
        this.shape = v6 ? shapeV6 : shapeV4;
        reset();
    }

    public void reset() {
        clear();
        shape.shape().reposition(address() + sockaddrLen.size());
        putInt(sockaddrLen, shape.shape().size());
    }

    @SuppressWarnings("unchecked")
    public <T extends InetAddress> Result<SocketAddress<T>> extract() {
        return shape.extract();
    }

    public long sizePtr() {
        return address();
    }

    public int sockAddrSize() {
        return getInt(sockaddrLen);
    }

    public long sockAddrPtr() {
        return shape.shape().address();
    }

    @SuppressWarnings("unchecked")
    public <T extends InetAddress> OffHeapSocketAddress assign(SocketAddress<T> input) {
        switch (input.address()) {
            case Inet4Address __1 -> protocolVersion(false);
            case Inet6Address __2 -> protocolVersion(true);
            default -> throw new IllegalStateException("Unexpected value: " + input.address());
        }

        shape.assign(input);
        return this;
    }

    public static OffHeapSocketAddress v4() {
        return new OffHeapSocketAddress(false);
    }

    public static OffHeapSocketAddress v6() {
        return new OffHeapSocketAddress(true);
    }

    public static <T extends InetAddress> OffHeapSocketAddress unsafeSocketAddress(SocketAddress<T> address) {
        return switch (address.address()) {
            case Inet4Address __1 -> new OffHeapSocketAddress(false).assign(address);
            case Inet6Address __2 -> new OffHeapSocketAddress(true).assign(address);
            default -> null;
        };
    }
}
