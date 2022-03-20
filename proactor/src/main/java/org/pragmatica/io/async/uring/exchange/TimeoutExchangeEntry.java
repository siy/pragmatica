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

package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapTimeSpec;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Unit;

import static org.pragmatica.io.async.uring.AsyncOperation.LINK_TIMEOUT;

/**
 * Exchange entry for {@code timeout} request.
 */
public class TimeoutExchangeEntry extends AbstractExchangeEntry<TimeoutExchangeEntry, Unit> {
    private final OffHeapTimeSpec timeSpec = OffHeapTimeSpec.uninitialized();

    protected TimeoutExchangeEntry(PlainObjectPool<TimeoutExchangeEntry> pool) {
        super(LINK_TIMEOUT, pool);
    }

    @Override
    public void close() {
        timeSpec.dispose();
    }

    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
    }

    public TimeoutExchangeEntry prepare(Timeout timeout) {
        timeout.secondsAndNanos()
               .map(timeSpec::setSecondsNanos);

        return this;
    }

    @Override
    public SQEntry apply(SQEntry entry) {
        return super.apply(entry)
                    .addr(timeSpec.address())
                    .fd(-1)
                    .len(1);
    }
}
