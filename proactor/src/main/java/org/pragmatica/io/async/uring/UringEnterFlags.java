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

/**
 * Flags for {@link UringNative#enter(long, long, long, int)} method.
 */
public enum UringEnterFlags implements Bitmask {
    IORING_ENTER_GETEVENTS(1 << 0),
    IORING_ENTER_SQ_WAKEUP(1 << 1),
    IORING_ENTER_SQ_WAIT(1 << 2),
    IORING_ENTER_EXT_ARG(1 << 3);

    private static final EnumSet<UringEnterFlags> GET_EVENTS = EnumSet.of(IORING_ENTER_GETEVENTS);
    private final int mask;

    UringEnterFlags(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }

    public static EnumSet<UringEnterFlags> getEvents() {
        return GET_EVENTS;
    }
}