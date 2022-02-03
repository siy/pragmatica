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
 * File permission bits.
 */
public enum FilePermission implements Bitmask {
    USER_R(00_400),      /* user has read permission */
    USER_W(00_200),      /* user has write permission */
    USER_X(00_100),      /* user has execute permission */

    GROUP_R(00_040),     /* group has read permission */
    GROUP_W(00_020),     /* group has write permission */
    GROUP_X(00_010),     /* group has execute permission */

    OTHER_R(00_004),     /* others have read permission */
    OTHER_W(00_002),     /* others have write permission */
    OTHER_X(00_001),     /* others have execute permission */

    SUID_BIT(04_000),    /* set-user-ID bit */
    SGID_BIT(02_000),    /* set-group-ID bit */
    STICKY_BIT(01_000);  /* sticky bit */

    private static final EnumSet<FilePermission> USER_RWX = EnumSet.of(USER_R, USER_W, USER_X);
    private static final EnumSet<FilePermission> GROUP_RWX = EnumSet.of(GROUP_R, GROUP_W, GROUP_X);
    private static final EnumSet<FilePermission> OTHER_RWX = EnumSet.of(OTHER_R, OTHER_W, OTHER_X);
    private static final EnumSet<FilePermission> SUID = EnumSet.of(SUID_BIT);
    private static final EnumSet<FilePermission> SGID = EnumSet.of(SGID_BIT);
    private static final EnumSet<FilePermission> STICKY = EnumSet.of(STICKY_BIT);
    private static final EnumSet<FilePermission> NONE = EnumSet.noneOf(FilePermission.class);

    private final int mask;

    FilePermission(final int mask) {
        this.mask = mask;
    }

    public static EnumSet<FilePermission> none() {
        return NONE;
    }

    @Override
    public int mask() {
        return mask;
    }

    public static EnumSet<FilePermission> userRWX() {
        return USER_RWX;
    }

    public static EnumSet<FilePermission> groupRWX() {
        return GROUP_RWX;
    }

    public static EnumSet<FilePermission> otherRWX() {
        return OTHER_RWX;
    }

    public static EnumSet<FilePermission> suid() {
        return SUID;
    }

    public static EnumSet<FilePermission> sgid() {
        return SGID;
    }

    public static EnumSet<FilePermission> sticky() {
        return STICKY;
    }

    public static EnumSet<FilePermission> fromShort(final short value) {
        final var result = EnumSet.noneOf(FilePermission.class);

        for (var permission : values()) {
            if ((value & permission.mask()) != 0) {
                result.add(permission);
            }
        }

        return result;
    }
}
