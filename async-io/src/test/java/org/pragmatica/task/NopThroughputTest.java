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

package org.pragmatica.task;

import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.Proactor;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class NopThroughputTest {
    record SingleTask(AtomicLong count, AtomicLong start, AtomicLong stop, CountDownLatch latch) {}

    @Test
    void testPeakThroughput() throws InterruptedException {
        var list = new ConcurrentLinkedQueue<SingleTask>();
        var shutdown = new AtomicBoolean(false);
        var latch = new CountDownLatch(TaskExecutor.DEFAULT_THREAD_COUNT);

        Promise.<Unit>promise((__, ___, executor) -> executor.spread(proactor -> runNop(proactor, list, shutdown, latch)));

        Thread.sleep(30_000);

        shutdown.set(true);

        latch.await();

        var totalTime = list.stream().mapToLong(task -> task.stop().get() - task.start().get()).sum();
        var totalCount = list.stream().mapToLong(task -> task.count().get()).sum();

        var time = ((double) totalTime) / 10e9;
        var speed = ((double) totalCount) / time / 1e6;

        System.out.printf("Total time: %.2fs\nTotal count: %d\nPerformance: %.2fM IOPS total, %.2fM IOPS per core\n",
                          time, totalCount, speed, speed/TaskExecutor.DEFAULT_THREAD_COUNT);
    }

    private void runNop(Proactor proactor, ConcurrentLinkedQueue<SingleTask> list, AtomicBoolean shutdown, CountDownLatch latch) {
        var task = new SingleTask(new AtomicLong(0), new AtomicLong(System.nanoTime()), new AtomicLong(0), latch);

        list.offer(task);
        nop(proactor, task, shutdown);
    }

    private void nop(Proactor proactor, SingleTask task, AtomicBoolean shutdown) {
        task.count().incrementAndGet();

        if (shutdown.get()) {
            task.stop().set(System.nanoTime());
            task.latch.countDown();
            return;
        }

        proactor.nop((__, proactor1) -> nop(proactor1, task, shutdown));
    }
}
