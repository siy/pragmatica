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
import org.pragmatica.io.async.util.allocator.ChunkedAllocator;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.IntStream.range;
import static org.pragmatica.io.async.util.ActionableThreshold.threshold;
import static org.pragmatica.io.async.util.DaemonThreadFactory.shutdownThreadFactory;
import static org.pragmatica.io.async.util.DaemonThreadFactory.threadFactory;
import static org.pragmatica.io.async.util.Units._1MiB;
import static org.pragmatica.io.async.util.allocator.ChunkedAllocator.allocator;
import static org.pragmatica.lang.Unit.unitResult;

final class TaskExecutorImpl implements TaskExecutor {
    public static final int FIXED_POOL_SIZE = 32 * _1MiB;

    private static final boolean USE_SHARED_WQ = false; //With `true` performance is slightly worse

    private final int numThreads;
    private final ExecutorService executor;
    private final ActionableThreshold threshold;
    private final List<TaskRunner> runners = new ArrayList<>();
    private final List<Proactor> proactors = new ArrayList<>();
    private final Promise<Unit> shutdownPromise = Promise.promise();
    private final ChunkedAllocator allocator;

    private int next;

    TaskExecutorImpl(int numThreads) {
        this.numThreads = numThreads;
        this.executor = newFixedThreadPool(numThreads, threadFactory("TaskExecutor #%d"));
        this.threshold = threshold(numThreads, () -> shutdownPromise.resolve(unitResult()));
        this.allocator = allocator(FIXED_POOL_SIZE);

        Runtime.getRuntime().addShutdownHook(shutdownThreadFactory().newThread(this::shutdown));

        if (USE_SHARED_WQ) {
            var rootProactor = Proactor.proactor(allocator);

            range(1, numThreads)
                .forEach(__ -> proactors.add(Proactor.proactor(allocator, rootProactor)));

            proactors.add(rootProactor);
        } else {
            range(0, numThreads)
                .forEach(__ -> proactors.add(Proactor.proactor(allocator)));
        }

        proactors.forEach(proactor -> runners.add(new TaskRunner(threshold, proactor)));
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
    public TaskExecutor replicate(Consumer<Proactor> task) {
        runners.forEach(runner -> runner.push(task));
        return this;
    }

    @Override
    public Promise<Unit> shutdown() {
        runners.forEach(TaskRunner::shutdown);

        executor.shutdown();
        proactors.forEach(Proactor::shutdown);
        allocator.close();

        return shutdownPromise.onResultDo(allocator::close);
    }

    @Override
    public int parallelism() {
        return numThreads;
    }
}
