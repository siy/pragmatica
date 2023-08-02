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

package org.pragmatica.io.util;

import org.pragmatica.io.AsyncCloseable;
import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static org.pragmatica.lang.Unit.unitResult;

public final class PeriodicTaskRunner implements BiConsumer<Result<Duration>, Proactor>, AsyncCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicTaskRunner.class);

    private final Promise<Unit> promise = Promise.promise();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private final Timeout timeout;
    private final Runnable task;
    private final boolean stopOnFailure;

    private PeriodicTaskRunner(Timeout timeout, Runnable task, boolean stopOnFailure) {
        this.timeout = timeout;
        this.task = task;
        this.stopOnFailure = stopOnFailure;
    }

    public static PeriodicTaskRunner periodicTaskRunner(Timeout timeout, Runnable task) {
        return periodicTaskRunner(timeout, task, false);
    }

    public static PeriodicTaskRunner periodicTaskRunner(Timeout timeout, Runnable task, boolean stopOnFailure) {
        return new PeriodicTaskRunner(timeout, task, stopOnFailure);
    }

    public PeriodicTaskRunner start() {
        Proactor.proactor().delay(this, timeout);
        return this;
    }

    @Override
    public Promise<Unit> close() {
        stopFlag.compareAndSet(false, true);
        return promise;
    }

    @Override
    public void accept(Result<Duration> durationResult, Proactor proactor) {
        if (stopFlag.get()) {
            promise.resolve(unitResult());
            return;
        }

        try {
            Promise.runAsync(task);
        } catch (Exception e) {
            LOG.info("Periodic task triggered exception", e);

            if (stopOnFailure) {
                promise.resolve(Causes.fromThrowable(e).result());
                return;
            }
        }

        proactor.delay(this, timeout);
    }
}
