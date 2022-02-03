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
//TODO: document all flags
public enum OpenFlags implements Bitmask {
    READ_ONLY(00_000_000),      // Open file for Read
    WRITE_ONLY(00_000_001),     // Open file for Write
    READ_WRITE(00_000_002),     // Open file for Read/Write
    CREATE(00_000_100),         // Create file if it does not exist
    EXCL(00_000_200),
    NOCTTY(00_000_400),
    TRUNCATE(00_001_000),
    APPEND(00_002_000),
    NONBLOCK(00_004_000),
    DSYNC(00_010_000),
    DIRECT(00_040_000),
    LARGEFILE(00_100_000),
    DIRECTORY(00_200_000),
    NOFOLLOW(00_400_000),
    NOATIME(01_000_000),
    CLOEXEC(02_000_000),
    SYNC(04_000_000 | 00_010_000),
    PATH(010_000_000),
    TMPFILE(020_000_000 | 00_200_000),
    NDELAY(00_004_000);

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
