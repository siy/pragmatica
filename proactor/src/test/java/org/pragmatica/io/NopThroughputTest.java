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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import static org.pragmatica.lang.Unit.unitResult;

public class NopThroughputTest {
    record SingleTask(int id, long[] count, AtomicLong start, AtomicLong stop) {}

    @Test
    void testPeakThroughput() throws InterruptedException {
        var list = new ConcurrentLinkedQueue<SingleTask>();
        var shutdown = new AtomicBoolean(false);
        var multiplicationFactor = 100;
        var cores = Runtime.getRuntime().availableProcessors();
        var tasks = cores * multiplicationFactor;
        var latch = new CountDownLatch(tasks);

        IntStream.range(0, tasks).forEach(ndx -> runNop(ndx, list, shutdown, latch));

        Thread.sleep(15_000);
        shutdown.setRelease(true);
        latch.await(15, TimeUnit.SECONDS);

        var minTime = list.stream().mapToLong(task -> task.start().get()).min().orElseThrow();
        var maxTime = list.stream().mapToLong(task -> task.stop().get()).max().orElseThrow();
        var totalTime = maxTime - minTime;
        var totalCount = list.stream().mapToLong(task -> task.count()[0]).sum();

        var time = ((double) totalTime) / 1e9;
        var speed = ((double) totalCount) / time / 1e6;

        System.out.printf("Total time: %.2fs\nTotal ops count: %d\nPerformance: %.2fM IOPS total, %.2fM IOPS per core (%d)\n",
                          time, totalCount, speed, speed / cores, latch.getCount());
    }

    private void runNop(int id, ConcurrentLinkedQueue<SingleTask> list, AtomicBoolean shutdown, CountDownLatch latch) {
        var task = new SingleTask(id, new long[1], new AtomicLong(System.nanoTime()), new AtomicLong(0));

        list.offer(task);

        var runnableTask = new RunnableTask(task, shutdown, latch);

        runnableTask.accept(unitResult(), Proactor.proactor());
    }

    record RunnableTask(SingleTask task, AtomicBoolean shutdown, CountDownLatch latch) implements BiConsumer<Result<Unit>, Proactor> {

        @Override
        public void accept(Result<Unit> ignored, Proactor proactor) {
            if (shutdown.getAcquire()) {
                task.stop().set(System.nanoTime());
                latch.countDown();
            } else {
                task.count()[0]++;
                proactor.nop(this);
            }
        }
    }
}
