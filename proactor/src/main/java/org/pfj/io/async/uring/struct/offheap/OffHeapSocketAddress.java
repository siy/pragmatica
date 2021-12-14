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

package org.pfj.io.async.uring.struct.offheap;

import org.pfj.io.async.net.SocketAddress;
import org.pfj.io.async.net.SocketAddressIn;
import org.pfj.io.async.net.SocketAddressIn6;
import org.pfj.io.async.uring.struct.ExternalRawStructure;
import org.pfj.io.async.uring.struct.raw.RawSocketAddress;
import org.pfj.io.async.uring.struct.raw.RawSocketAddressIn;
import org.pfj.io.async.uring.struct.raw.RawSocketAddressIn6;
import org.pfj.io.async.util.raw.RawProperty;
import org.pfj.lang.Result;

public class OffHeapSocketAddress<T extends SocketAddress<?>, R extends ExternalRawStructure<?>> extends AbstractOffHeapStructure<OffHeapSocketAddress<T,R>> {
    private static final int SIZE = 128 + 4;    //Equal to sizeof(struct sockaddr_storage) + sizeof(socklen_t)
    private static final RawProperty sockaddrLen = RawProperty.raw(0, 4);
    private final RawSocketAddress<T, R> shape;

    private OffHeapSocketAddress(final RawSocketAddress<T, R> shape) {
        super(SIZE);
        this.shape = shape;
        reset();
    }

    public void reset() {
        clear();
        shape.shape().reposition(address() + sockaddrLen.size());
        putInt(sockaddrLen, shape.shape().size());
    }

    public Result<T> extract() {
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

    public OffHeapSocketAddress<T,R> assign(final T input) {
        shape.assign(input);
        return this;
    }

    public static OffHeapSocketAddress<SocketAddressIn, RawSocketAddressIn> addressIn() {
        return new OffHeapSocketAddress<>(RawSocketAddressIn.at(0));
    }

    public static OffHeapSocketAddress<SocketAddressIn, RawSocketAddressIn> addressIn(final SocketAddressIn addressIn) {
        return new OffHeapSocketAddress<>(RawSocketAddressIn.at(0)).assign(addressIn);
    }

    public static OffHeapSocketAddress<SocketAddressIn6, RawSocketAddressIn6> addressIn6() {
        return new OffHeapSocketAddress<>(RawSocketAddressIn6.at(0));
    }

    public static OffHeapSocketAddress<SocketAddressIn6, RawSocketAddressIn6> addressIn6(final SocketAddressIn6 addressIn6) {
        return new OffHeapSocketAddress<>(RawSocketAddressIn6.at(0)).assign(addressIn6);
    }

    @SuppressWarnings("unchecked")
    public static <SA extends SocketAddress<?>, RSA extends ExternalRawStructure<?>>
    OffHeapSocketAddress<SA, RSA> unsafeSocketAddress(final SocketAddress<?> address) {
        if (address instanceof SocketAddressIn addressIn) {
            return (OffHeapSocketAddress<SA, RSA>) addressIn(addressIn);
        } else if (address instanceof SocketAddressIn6 addressIn6) {
            return (OffHeapSocketAddress<SA, RSA>) addressIn6(addressIn6);
        } else {
            return null;
        }
    }
}
