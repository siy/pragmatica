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

import static org.pfj.io.async.SystemError.EFAULT;

//TODO: move to InetAddress and make it sealed
public final class Inet4Address implements InetAddress {
    public static final int SIZE = 4;
    public static final Inet4Address INADDR_ANY = new Inet4Address(new byte[SIZE]);

    private final byte[] address;

    private Inet4Address(final byte[] address) {
        this.address = address;
    }

    public static Result<Inet4Address> inet4Address(final byte[] address) {
        return address.length != SIZE
               ? EFAULT.result()
               : Result.success(new Inet4Address(address));
    }

    @Override
    public byte[] asBytes() {
        return address;
    }

    @Override
    public String toString() {
        return String.format("Inet4Address(%d.%d.%d.%d)",
                             (int) address[0] & 0xFF,
                             (int) address[1] & 0xFF,
                             (int) address[2] & 0xFF,
                             (int) address[3] & 0xFF);
    }
}
