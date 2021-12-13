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

package org.pfj.io.uring.struct.raw;

import org.pfj.io.uring.struct.shape.IoVectorOffsets;
import org.pfj.io.uring.struct.AbstractExternalRawStructure;

public class IoVector extends AbstractExternalRawStructure<IoVector> {
    private IoVector(final long address) {
        super(address, IoVectorOffsets.SIZE);
    }

    public static IoVector at(final long address) {
        return new IoVector(address);
    }

    public long base() {
        return getLong(IoVectorOffsets.iov_base);
    }

    public IoVector base(final long data) {
        return putLong(IoVectorOffsets.iov_base, data);
    }

    public long len() {
        return getLong(IoVectorOffsets.iov_len);
    }

    public IoVector len(final long data) {
        return putLong(IoVectorOffsets.iov_len, data);
    }
}