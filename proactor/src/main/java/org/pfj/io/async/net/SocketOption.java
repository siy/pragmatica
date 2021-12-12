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

import org.pfj.io.Bitmask;

import java.util.EnumSet;

public enum SocketOption implements Bitmask {
    SO_KEEPALIVE(0x0001), // Enable keep-alive packets
    SO_REUSEADDR(0x0002), // Enable reuse address option
    SO_REUSEPORT(0x0004), // Enable reuse port option
    SO_LINGER   (0x0008); // Enable linger option and set linger time to 0

    private static final EnumSet<SocketOption> NONE = EnumSet.noneOf(SocketOption.class);
    private static final EnumSet<SocketOption> REUSE_ALL = EnumSet.of(SO_REUSEADDR, SO_REUSEPORT);

    private final int mask;

    SocketOption(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return 0;
    }

    public static EnumSet<SocketOption> none() {
        return NONE;
    }

    public static EnumSet<SocketOption> reuseAll() {
        return REUSE_ALL;
    }
}
