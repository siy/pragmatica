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

package org.pragmatica.io.async.uring.struct.shape;

import org.pragmatica.io.async.util.raw.RawProperty;

/**
 * Offsets/Sizes of the fields of the {@link org.pragmatica.io.async.uring.struct.raw.RawSocketAddressIn}
 */
public interface SocketAddressInOffsets {
    int SIZE = 16;
    RawProperty sin_family = RawProperty.raw(0, 2);
    RawProperty sin_port = RawProperty.raw(2, 2);
    RawProperty sin_addr = RawProperty.raw(4, 4);
    RawProperty sin_zero = RawProperty.raw(8, 8);
}
