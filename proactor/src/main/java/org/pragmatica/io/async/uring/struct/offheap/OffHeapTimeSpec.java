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

package org.pragmatica.io.async.uring.struct.offheap;

import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.uring.struct.shape.TimeSpecOffsets;

import static org.pragmatica.io.async.uring.struct.shape.TimeSpecOffsets.tv_nsec;
import static org.pragmatica.io.async.uring.struct.shape.TimeSpecOffsets.tv_sec;

/**
 * Container for data equivalent to {@code struct __kernel_timespec}.
 */
public class OffHeapTimeSpec extends AbstractOffHeapStructure<OffHeapTimeSpec> {
    private OffHeapTimeSpec() {
        super(TimeSpecOffsets.SIZE);
    }

    public OffHeapTimeSpec setSecondsNanos(long seconds, long nanos) {
        return putLong(tv_sec, seconds).putLong(tv_nsec, nanos);
    }

    public static OffHeapTimeSpec uninitialized() {
        return new OffHeapTimeSpec();
    }

    public static OffHeapTimeSpec forSecondsNanos(long seconds, long nanos) {
        return new OffHeapTimeSpec()
            .putLong(tv_sec, seconds)
            .putLong(tv_nsec, nanos);
    }

    public static OffHeapTimeSpec forTimeout(Timeout timeout) {
        return timeout.asSecondsAndNanos()
                      .map(OffHeapTimeSpec::forSecondsNanos);
    }
}
