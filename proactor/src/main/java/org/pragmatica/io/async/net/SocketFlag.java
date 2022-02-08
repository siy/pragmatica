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

package org.pragmatica.io.async.net;

import org.pragmatica.io.async.uring.Bitmask;

import java.util.EnumSet;

/**
 * Socket open/accept flags.
 */
public enum SocketFlag implements Bitmask {
    CLOEXEC(0x00080000),     /* Set close-on-exec flag for the descriptor */
    NONBLOCK(0x00000800);    /* Mark descriptor as non-blocking */

    private static final EnumSet<SocketFlag> NONE = EnumSet.noneOf(SocketFlag.class);
    private static final EnumSet<SocketFlag> CLOSE_ON_EXEC = EnumSet.of(CLOEXEC);

    private final int mask;

    SocketFlag(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }

    public static EnumSet<SocketFlag> none() {
        return NONE;
    }

    public static EnumSet<SocketFlag> closeOnExec() {
        return CLOSE_ON_EXEC;
    }
}
