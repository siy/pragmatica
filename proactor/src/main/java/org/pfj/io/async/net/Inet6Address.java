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

import org.pfj.lang.Result;

import static org.pfj.io.NativeFailureType.EFAULT;

//TODO: move to InetAddress and make it sealed
public class Inet6Address implements InetAddress {
    public static final int SIZE = 16;
    private final byte[] address;

    private Inet6Address(final byte[] address) {
        this.address = address;
    }

    public static Result<Inet6Address> inet6Address(final byte[] address) {
        return address.length != SIZE
               ? EFAULT.result()
               : Result.success(new Inet6Address(address));
    }

    @Override
    public byte[] asBytes() {
        return address;
    }
}
