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

package org.pragmatica.io;

import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.Proactor;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class NopThroughputTest {
    //record SingleTask(int id, AtomicLong count, AtomicLong start, AtomicLong stop) {}
    private static class SingleTask implements BiConsumer<Result<Unit>, Proactor> {
        private final int id;
        private final CountDownLatch latch;
        private final long startTime;
        private long stopTime;
        private long count;
        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        public SingleTask(int id, CountDownLatch latch) {
            this.id = id;
            this.latch = latch;
            this.count = 0;
            this.startTime = System.nanoTime();
            this.stopTime = startTime;
        }

        public void shutdown() {
            this.shutdown.lazySet(true);
        }

        public long count() {
            return this.count;
        }

        public double speed() {
            return ((double) count) / (((double) (stopTime - startTime)) / 1e9) / 1e6;
        }

        @Override
        public void accept(Result<Unit> ignored, Proactor proactor) {
            if (shutdown.getOpaque()) {
                this.stopTime = System.nanoTime();
                this.latch.countDown();
            } else {
                count++;
                proactor.nop(this);
            }
        }
    }

    @SuppressWarnings("resource")
    @Test
    void testPeakThroughput() throws InterruptedException {
        var multiplicationFactor = 100;
        var cores = Runtime.getRuntime().availableProcessors();
        var tasks = cores * multiplicationFactor;
        var latch = new CountDownLatch(tasks);
        var list = IntStream.range(0, tasks)
                            .mapToObj(id -> new SingleTask(id, latch)).toList();

        var pool = ForkJoinPool.commonPool();
        list.forEach(task -> pool.execute(() -> task.accept(Result.ok(Unit.unit()), Proactor.proactor())));

        Thread.sleep(8_000);

        list.forEach(SingleTask::shutdown);

        latch.await(15, TimeUnit.SECONDS);

        var totalOps = list.stream()
                           .mapToLong(SingleTask::count)
                           .sum();
        var totalIops = list.stream()
                            .mapToDouble(SingleTask::speed)
                            .sum();
        var perCoreIops = totalIops / cores;

        System.out.printf("""
                          Total operation count: %d
                          Performance, IOPS: %.2fM
                          Performance, per core, IOPS: %.2fM
                          Incomplete tasks: %d""",
                          totalOps, totalIops, perCoreIops, latch.getCount());
    }
}
