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

package org.pragmatica.io.async.uring.struct.shape;

import org.pragmatica.io.async.util.raw.RawProperty;

/**
 * Offsets/Sizes of the fields of the {@link org.pragmatica.io.async.uring.IoUring}
 */
public interface IoUringOffsets {
    // Rounded up to next power of two
    // WARNING: this offset is also hardcoded in the uring_api.c!
    int SIZE = 256;
    //int SIZE = 216;
    RawProperty sq = RawProperty.raw(0, 104);
    RawProperty cq = RawProperty.raw(104, 88);
    RawProperty flags = RawProperty.raw(192, 4);
    RawProperty ring_fd = RawProperty.raw(196, 4);
    RawProperty features = RawProperty.raw(200, 4);
}
