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

package org.pragmatica.io.async.net;

import org.pragmatica.io.async.uring.Bitmask;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Creation options for {@link org.pragmatica.io.async.Proactor#socket(BiConsumer, AddressFamily, SocketType, Set, Set)}.
 */
public enum SocketOption implements Bitmask {
    KEEP_ALIVE(0x0001), // Enable keep-alive packets
    REUSE_ADDR(0x0002), // Enable reuse address option
    REUSE_PORT(0x0004), // Enable reuse port option
    LINGER(0x0008); // Enable linger option and set linger time to 0

    private static final EnumSet<SocketOption> NONE = EnumSet.noneOf(SocketOption.class);
    private static final EnumSet<SocketOption> REUSE_ALL = EnumSet.of(REUSE_ADDR, REUSE_PORT);

    private final int mask;

    SocketOption(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }

    public static EnumSet<SocketOption> none() {
        return NONE;
    }

    public static EnumSet<SocketOption> reuseAll() {
        return REUSE_ALL;
    }
}
