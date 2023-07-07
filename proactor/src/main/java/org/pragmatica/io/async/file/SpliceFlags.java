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

package org.pragmatica.io.async.file;

import org.pragmatica.io.async.uring.Bitmask;

public enum SpliceFlags implements Bitmask {
    MOVE(1),        /* SPLICE_F_MOVE    : Move pages instead of copying.  */
    NONBLOCK(2),    /* SPLICE_F_NONBLOCK: Don't block on the pipe splicing */
    MORE(4);        /* SPLICE_F_MORE    : Expect more data.  */

    private final int mask;

    SpliceFlags(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
