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
 * Offsets/Sizes of the fields of the {@link org.pragmatica.io.async.uring.IoUringCQ}
 */
public interface IoUringCQOffsets {
    int SIZE = 88;
    RawProperty khead = RawProperty.raw(0, 8);
    RawProperty ktail = RawProperty.raw(8, 8);
    RawProperty kring_mask = RawProperty.raw(16, 8);
    RawProperty kring_entries = RawProperty.raw(24, 8);
    RawProperty kflags = RawProperty.raw(32, 8);
    RawProperty koverflow = RawProperty.raw(40, 8);
    RawProperty cqes = RawProperty.raw(48, 8);
    RawProperty ring_sz = RawProperty.raw(56, 8);
    RawProperty ring_ptr = RawProperty.raw(64, 8);
}
