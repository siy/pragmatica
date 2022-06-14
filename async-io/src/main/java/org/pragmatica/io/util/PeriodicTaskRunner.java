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

package org.pragmatica.io.util;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.pragmatica.lang.Unit.unitResult;

public final class PeriodicTaskRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicTaskRunner.class);

    private final Promise<Unit> promise = Promise.promise();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private final Timeout timeout;
    private final Consumer<Proactor> task;

    private PeriodicTaskRunner(Timeout timeout, Consumer<Proactor> task) {
        this.timeout = timeout;
        this.task = task;
    }

    public static PeriodicTaskRunner periodicTaskRunner(Timeout timeout, Runnable task) {
        return new PeriodicTaskRunner(timeout, __ -> task.run());
    }

    public static PeriodicTaskRunner periodicTaskRunner(Timeout timeout, Consumer<Proactor> task) {
        return new PeriodicTaskRunner(timeout, task);
    }

    public PeriodicTaskRunner start() {
        promise.async(this::runTask);
        return this;
    }

    public Promise<Unit> stop() {
        stopFlag.compareAndSet(false, true);
        return promise;
    }

    private void runTask(Object unused, Proactor proactor) {
        if (stopFlag.get()) {
            promise.resolve(unitResult());
            return;
        }

        try {
            task.accept(proactor);
        } catch (Exception e) {
            LOG.info("Periodic task triggered exception", e);
        } finally {
            proactor.delay(this::runTask, timeout);
        }
    }
}
