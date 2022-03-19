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

package org.pragmatica.io.async.uring;

import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.IoUringParamsOffsets;

/**
 * Representation of the internals of the {@code io_uring_params} structure.
 */
public class IoUringParams extends AbstractExternalRawStructure<IoUringParams> {
    private IoUringParams(long address) {
        super(address, IoUringParamsOffsets.SIZE);
    }

    public static IoUringParams at(long address) {
        return new IoUringParams(address);
    }

    public int flags() {
        return getInt(IoUringParamsOffsets.flags);
    }

    public IoUringParams flags(int flags) {
        return putInt(IoUringParamsOffsets.flags, flags);
    }

    public int workQueueFD() {
        return getInt(IoUringParamsOffsets.wq_fd);
    }

    public IoUringParams workQueueFD(int fd) {
        return putInt(IoUringParamsOffsets.wq_fd, fd);
    }
}
