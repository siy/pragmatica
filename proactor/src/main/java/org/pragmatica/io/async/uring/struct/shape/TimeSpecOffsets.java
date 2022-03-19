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
 * Offsets/Sizes of the fields of the {@link org.pragmatica.io.async.uring.struct.offheap.OffHeapTimeSpec}
 */
public interface TimeSpecOffsets {
    int SIZE = 16;
    RawProperty tv_sec = RawProperty.raw(0, 8);
    RawProperty tv_nsec = RawProperty.raw(8, 8);
}