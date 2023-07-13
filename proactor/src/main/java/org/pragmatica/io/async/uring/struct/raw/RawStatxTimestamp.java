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

package org.pragmatica.io.async.uring.struct.raw;

import org.pragmatica.io.async.file.stat.StatTimestamp;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.StatxTimestampOffsets;

/**
 * Storage for timestamps used in file status information. Used to represent last access time (atime), birth time (btime),
 * last metadata change time (ctime) and last content modification time (mtime).
 */
public class RawStatxTimestamp extends AbstractExternalRawStructure<RawStatxTimestamp> {
    private RawStatxTimestamp(final long address) {
        super(address, StatxTimestampOffsets.SIZE);
    }

    public static RawStatxTimestamp at(final long address) {
        return new RawStatxTimestamp(address);
    }

    public long seconds() {
        return getLong(StatxTimestampOffsets.tv_sec);
    }

    public int nanos() {
        return getInt(StatxTimestampOffsets.tv_nsec);
    }

    public StatTimestamp detach() {
        return StatTimestamp.timestamp(seconds(), nanos());
    }
}
