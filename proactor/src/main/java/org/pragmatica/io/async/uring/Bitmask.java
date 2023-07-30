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

package org.pragmatica.io.async.uring;

import java.util.Set;

/**
 * Common interface for enums which represent a set of independent bits.
 */
public interface Bitmask {
    int mask();
    default byte byteMask() {
        return (byte) (mask() & 0xFF);
    }

    static int combine(final Set<? extends Bitmask> flags) {
        return flags.stream().mapToInt(Bitmask::mask).sum();
    }
}
