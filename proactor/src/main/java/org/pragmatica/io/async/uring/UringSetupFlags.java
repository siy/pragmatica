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

import java.util.EnumSet;

public enum UringSetupFlags implements Bitmask {
    IOPOLL(1 << 0),    /* io_context is polled */
    SQPOLL(1 << 1),    /* SQ poll thread */
    SQ_AFF(1 << 2),    /* sq_thread_cpu is valid */
    CQSIZE(1 << 3),    /* app defines CQ size */
    CLAMP(1 << 4),     /* clamp SQ/CQ ring sizes */
    ATTACH_WQ(1 << 5); /* attach to existing wq */

    private static final EnumSet<UringSetupFlags> DEFAULT = EnumSet.noneOf(UringSetupFlags.class);
    private int mask;

    UringSetupFlags(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }

    public static EnumSet<UringSetupFlags> defaultFlags() {
        return DEFAULT;
    }
}
