/*
 *  Copyright (c) 2020-2023 Sergiy Yevtushenko.
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

package org.pragmatica.io.utils;

import org.pragmatica.io.AsyncCloseable;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.pragmatica.io.async.Timeout.timeout;
import static org.pragmatica.lang.Unit.unitResult;

public sealed interface PeriodicTaskRunner extends AsyncCloseable {
    static PeriodicTaskBuilder withFixedRate(Timeout timeout) {
        return task -> new FixedRateRunner(task, timeout);
    }

    static PeriodicTaskBuilder withFixedDelay(Timeout timeout) {
        return task -> new FixedDelayRunner(task, timeout);
    }

    PeriodicTaskRunner start();

    @Override
    Promise<Unit> close();

    interface PeriodicTaskBuilder {
        PeriodicTaskRunner run(Runnable task);
    }

    abstract non-sealed class TaskRunner implements PeriodicTaskRunner {
        private static final Logger LOG = LoggerFactory.getLogger(PeriodicTaskRunner.class);
        private final Promise<Unit> promise = Promise.promise();
        private final AtomicBoolean stopFlag = new AtomicBoolean(false);
        private final Runnable task;
        private final Timeout timeout;

        protected TaskRunner(Runnable task, Timeout timeout) {
            this.task = task;
            this.timeout = timeout;
        }

        @Override
        public Promise<Unit> close() {
            stopFlag.compareAndSet(false, true);
            return promise;
        }

        @Override
        public PeriodicTaskRunner start() {
            var timestamp = System.nanoTime();

            Thread.ofVirtual()
                  .start(() -> {
                      run(timestamp);
                      promise.resolve(unitResult());
                  });
            return this;
        }

        protected void runTask() {
            try {
                task.run();
            } catch (Throwable e) {
                LOG.info("Periodic task triggered exception", e);
            }
        }

        protected boolean isStopped() {
            return stopFlag.get();
        }

        protected long calculateDelay(long timestamp) {
            var delay = timestamp - System.nanoTime() + timeout.nanoseconds();
            var cnt = 0;

            while (delay < 0) {
                delay += timeout.nanoseconds();
                cnt++;
            }

            if (cnt > 0) {
                LOG.info("Periodic task took too long to execute, skipped {} execution(s)", cnt);
            }
            return delay;
        }

        protected Tuple2<Long, Integer> millisAndNanos() {
            return timeout.millisAndNanos();
        }

        protected static Result<Unit> sleep(Long seconds, Integer nanos) throws InterruptedException {
            Thread.sleep(seconds, nanos);
            return unitResult();
        }

        abstract void run(long timestamp);
    }

    final class FixedRateRunner extends TaskRunner {
        private FixedRateRunner(Runnable task, Timeout timeout) {
            super(task, timeout);
        }

        protected void run(long timestamp) {
            var delay = calculateDelay(timestamp);

            while (!isStopped()) {
                timeout(delay)
                    .nanos()
                    .millisAndNanos().lift(TaskRunner::sleep);

                var nextTimestamp = System.nanoTime();
                runTask();

                delay = calculateDelay(nextTimestamp);
            }
        }
    }

    final class FixedDelayRunner extends TaskRunner {
        private FixedDelayRunner(Runnable task, Timeout timeout) {
            super(task, timeout);
        }

        protected void run(long unused) {
            while (!isStopped()) {
                millisAndNanos().lift(TaskRunner::sleep);
                runTask();
            }
        }
    }
}
