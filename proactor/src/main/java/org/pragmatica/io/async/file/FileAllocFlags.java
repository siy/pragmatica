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

package org.pragmatica.io.async.file;

import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.lang.Option;

import java.util.Set;
import java.util.function.Consumer;

/**
 * File allocation flags.
 * <p>
 * These flags control behavior of {@link org.pragmatica.io.async.Proactor#fileAlloc(Consumer, FileDescriptor, Set, OffsetT, long, Option)} method.
 */
public enum FileAllocFlags implements Bitmask {
    KEEP_SIZE(0x01),    /* default is extend size */
    PUNCH_HOLE(0x02),   /* de-allocates range */
    NO_HIDE_STALE(0x04), /* reserved codepoint */
    COLLAPSE_RANGE(0x08), /* used to remove a range of a file without leaving a hole in the file */
    ZERO_RANGE(0x10), /* used to convert a range of file to zeros preferably without issuing data IO */
    INSERT_RANGE(0x20), /* use to insert space within the file size without overwriting any existing data */
    UNSHARE_RANGE(0x40); /* used to unshare shared blocks within the file size without overwriting any existing data */

    private final int mask;

    FileAllocFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
