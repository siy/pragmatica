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
 */

package org.pragmatica.io.async.file.stat;

import org.pragmatica.io.async.uring.Bitmask;

import java.util.EnumSet;

/**
 * Flags which control which information is requested and returned from the {@link FileStat}.
 */
public enum StatMask implements Bitmask {
    TYPE(0x000000001),    /* Want/got stx_mode & S_IFMT */
    MODE(0x000000002),    /* Want/got stx_mode & ~S_IFMT */
    NLINK(0x000000004),   /* Want/got stx_nlink */
    UID(0x000000008),     /* Want/got stx_uid */
    GID(0x000000010),     /* Want/got stx_gid */
    ATIME(0x000000020),   /* Want/got stx_atime */
    MTIME(0x000000040),   /* Want/got stx_mtime */
    CTIME(0x000000080),   /* Want/got stx_ctime */
    INO(0x000000100),     /* Want/got stx_ino */
    FSIZE(0x000000200),    /* Want/got stx_size */
    BLOCKS(0x000000400),  /* Want/got stx_blocks */
    BTIME(0x000000800);   /* Want/got stx_btime */

    private final int mask;
    private static final EnumSet<StatMask> BASIC = EnumSet.complementOf(EnumSet.of(BTIME));
    private static final EnumSet<StatMask> ALL = EnumSet.allOf(StatMask.class);

    public static EnumSet<StatMask> basic() {
        return BASIC;
    }

    public static EnumSet<StatMask> all() {
        return ALL;
    }

    StatMask(int mask) {
        this.mask = mask;
    }

    public static EnumSet<StatMask> fromInt(int value) {
        final var result = EnumSet.noneOf(StatMask.class);

        for (var statMask : values()) {
            if ((value & statMask.mask) != 0) {
                result.add(statMask);
            }
        }

        return result;
    }

    @Override
    public int mask() {
        return mask;
    }
}
