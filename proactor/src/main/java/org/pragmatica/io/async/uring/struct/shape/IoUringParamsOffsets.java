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

package org.pragmatica.io.async.uring.struct.shape;

import org.pragmatica.io.async.util.raw.RawProperty;

/**
 * Offsets/Sizes of the fields of the {@link org.pragmatica.io.async.uring.IoUringParams}
 */
public interface IoUringParamsOffsets {
    int SIZE = 120;
    RawProperty sq_entries = RawProperty.raw(0, 4);
    RawProperty cq_entries = RawProperty.raw(4, 4);
    RawProperty flags = RawProperty.raw(8, 4);
    RawProperty sq_thread_cpu = RawProperty.raw(12, 4);
    RawProperty sq_thread_idle = RawProperty.raw(16, 4);
    RawProperty features = RawProperty.raw(20, 4);
    RawProperty wq_fd = RawProperty.raw(24, 4);
    RawProperty sq_off = RawProperty.raw(40, 40);
    RawProperty cq_off = RawProperty.raw(80, 40);
}
