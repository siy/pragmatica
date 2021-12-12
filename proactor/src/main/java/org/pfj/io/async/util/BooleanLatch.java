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

package org.pfj.io.async.util;

import org.pfj.io.scheduler.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public final class BooleanLatch extends AbstractQueuedSynchronizer {
    private static final Logger LOG = LoggerFactory.getLogger(BooleanLatch.class);

    private BooleanLatch() {
    }

    public static BooleanLatch booleanLatch() {
        return new BooleanLatch();
    }

    @Override
    protected int tryAcquireShared(final int ignore) {
        return getState() == 0 ? -1 : 1;
    }

    @Override
    protected boolean tryReleaseShared(final int ignore) {
        setState(1);
        return true;
    }

    public void signal() {
        releaseShared(1);
    }

    public boolean await() {
        try {
            acquireSharedInterruptibly(1);
            return true;
        } catch (final InterruptedException e) {
            LOG.warn("await() interrupted", e);
            return false;
        }
    }

    public boolean await(final Timeout timeout) {
        try {
            return tryAcquireSharedNanos(1, timeout.asNanos());
        } catch (final InterruptedException e) {
            LOG.warn("await() interrupted", e);
            return false;
        }
    }
}
