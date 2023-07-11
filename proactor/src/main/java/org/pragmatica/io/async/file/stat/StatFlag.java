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

package org.pragmatica.io.async.file.stat;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.lang.Option;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Flags which control behavior of {@link Proactor#stat(BiConsumer, Path, Set, Set, Option)} and {@link Proactor#stat(BiConsumer, FileDescriptor, Set,
 * Set, Option)} methods.
 * <p>
 * Note that {@link #EMPTY_PATH} is used internally. This flag controls what is used to point to file - path or file descriptor. While this is not an
 * error to pass this flag to methods above, it is ignored.
 * <p>
 * Flags {@link #STATX_DONT_SYNC} and {@link #STATX_FORCE_SYNC} have mutually exclusive meaning so if they both are passed, result is undefined and
 * may depend on Linux kernel version. If none of these flags passed then default behavior is used. The default behavior depends on file system where
 * file resides. So, if consistent behavior is necessary then one of these flags should be provided. If underlying file system is remote, using {@link
 * #STATX_FORCE_SYNC} might trigger additional synchronization with relevant network communication.
 */
public enum StatFlag implements Bitmask {
    EMPTY_PATH(0x00001000),         /* Operate on FD rather than path (if path is empty) */
    SYMLINK_NOFOLLOW(0x00000100),   /* Don't follow symlink and return info about link itself */
    NO_AUTOMOUNT(0x00000800),       /* Don't trigger automount */
    STATX_FORCE_SYNC(0x00002000),   /* Force sync before obtaining info */
    STATX_DONT_SYNC(0x00004000);    /* Don't do sync at all */

    private final int mask;

    StatFlag(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
