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
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.util.ActionableThreshold;
import org.pragmatica.io.async.util.OffHeapSlice;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.pragmatica.io.async.util.DaemonThreadFactory.threadFactory;

class TaskRunnerTest {

    @Test
    void runnerCanExecuteTasks() throws InterruptedException {
        var fixedBufferPool = OffHeapSlice.fixedSize(1024 * 1024);
        var threshold = ActionableThreshold.threshold(1, () -> {});
        var runner = new TaskRunner(threshold, fixedBufferPool);
        var executor = Executors.newFixedThreadPool(1, threadFactory("Test {}"));
        var latch = new CountDownLatch(1);

        runner.start(executor);

        runner.push(proactor -> proactor.delay(__ -> latch.countDown(), Timeout.timeout(100).millis()));

        latch.await();

        runner.shutdown();
        executor.shutdown();
    }
}