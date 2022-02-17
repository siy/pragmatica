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

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.util.ActionableThreshold;
import org.pragmatica.io.async.util.DaemonThreadFactory;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.pragmatica.lang.Unit.unitResult;

final class TaskExecutorImpl implements TaskExecutor {
    private static final int _1K = 1024;
    private static final int _1M = _1K * _1K;
    private static final int _16M = 16 * _1M;

    private static final int FIXED_POOL_SIZE = _16M;

    private final int numThreads;
    private final ExecutorService executor;
    private final ActionableThreshold threshold;
    private final List<TaskRunner> runners = new ArrayList<>();
    private final Promise<Unit> shutdownPromise = Promise.promise();
    private final OffHeapSlice fixedBufferPool = OffHeapSlice.fixedSize(FIXED_POOL_SIZE);

    private int next;

    TaskExecutorImpl(int numThreads) {
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads, DaemonThreadFactory.threadFactory("TaskExecutor #%d"));
        this.threshold = ActionableThreshold.threshold(numThreads, () -> shutdownPromise.resolve(unitResult()));

        IntStream
            .range(0, numThreads)
            .forEach(n -> runners.add(new TaskRunner(threshold, fixedBufferPool)));

        runners.forEach(runner -> runner.start(executor));
    }

    @Override
    public TaskExecutor submit(Consumer<Proactor> task) {
        pushTask(task);

        return this;
    }

    private void pushTask(Consumer<Proactor> task) {
        runners.get(next++ % numThreads).push(task);
    }

    @Override
    public TaskExecutor submit(List<Consumer<Proactor>> tasks) {
        tasks.forEach(this::pushTask);

        return this;
    }

    @Override
    public TaskExecutor spread(Consumer<Proactor> task) {
        runners.forEach(runner -> runner.push(task));
        return this;
    }

    @Override
    public Promise<Unit> shutdown() {
        runners.forEach(TaskRunner::shutdown);

        executor.shutdown();

        return shutdownPromise;
    }

    @Override
    public int parallelism() {
        return numThreads;
    }
}
