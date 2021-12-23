/*
 *  Copyright (c) 2021 Sergiy Yevtushenko.
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

package org.pfj.lang.io.scheduler;

import org.pfj.io.async.Proactor;
import org.pfj.io.async.util.ActionableThreshold;
import org.pfj.io.async.util.DaemonThreadFactory;
import org.pfj.lang.Promise;
import org.pfj.lang.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.pfj.lang.Unit.unitResult;

final class TaskExecutorImpl implements TaskExecutor {
    private final int numThreads;
    private final ExecutorService executor;
    private final ActionableThreshold threshold;
    private final List<TaskRunner> runners = new ArrayList<>();
    private final Promise<Unit> shutdownPromise = Promise.promise();

    private int next;

    TaskExecutorImpl(int numThreads) {
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads, DaemonThreadFactory.threadFactory("TaskExecutor #%d"));
        this.threshold = ActionableThreshold.threshold(numThreads, () -> shutdownPromise.resolve(unitResult()));

        IntStream
            .range(0, numThreads)
            .forEach(n -> runners.add(new TaskRunner(executor, threshold)));

        runners.forEach(TaskRunner::start);
    }

    @Override
    public TaskExecutor submit(Consumer<Proactor> task) {
        runners.get(next++ % numThreads).push(task);

        return this;
    }

    @Override
    public Promise<Unit> shutdown() {
        executor.shutdown();
        return shutdownPromise;
    }

    @Override
    public int parallelism() {
        return numThreads;
    }
}
