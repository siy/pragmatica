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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskExecutorTest {
    @Test
    void taskCanBeExecuted() throws InterruptedException {
        TaskExecutor executor = TaskExecutor.taskExecutor(2);

        var counter = new AtomicInteger(0);
        var latch = new CountDownLatch(1);

        executor.submit(__ -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        latch.await();

        assertEquals(1, counter.get());

        executor.shutdown();
    }
}