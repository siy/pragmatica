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

package org.pragmatica.io.async.file;

import org.pragmatica.io.async.uring.Bitmask;

import java.util.EnumSet;

/**
 * File open flags.
 */
public enum OpenFlags implements Bitmask {
    READ_ONLY(00000000),
    WRITE_ONLY(00000001),
    READ_WRITE(00000002),
    CREATE(00000100),
    EXCL(00000200),
    NOCTTY(00000400),
    TRUNCATE(00001000),
    APPEND(00002000),
    NONBLOCK(00004000),
    DSYNC(00010000),
    DIRECT(00040000),
    LARGEFILE(00100000),
    DIRECTORY(00200000),
    NOFOLLOW(00400000),
    NOATIME(01000000),
    CLOEXEC(02000000),
    SYNC((04000000 | 00010000)),
    PATH(010000000),
    TMPFILE((020000000 | 00200000)),
    NDELAY(00004000);

    private static final EnumSet<OpenFlags> OPEN_RO = EnumSet.of(READ_ONLY);
    private static final EnumSet<OpenFlags> OPEN_RW = EnumSet.of(READ_WRITE);

    private final int mask;

    OpenFlags(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }

    public static EnumSet<OpenFlags> readOnly() {
        return OPEN_RO;
    }

    public static EnumSet<OpenFlags> readWrite() {
        return OPEN_RW;
    }
}
