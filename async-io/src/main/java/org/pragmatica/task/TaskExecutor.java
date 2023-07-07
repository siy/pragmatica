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
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.List;
import java.util.function.Consumer;

/**
 * Task executor suitable for processing of large number of small tasks. It assumes that all submitted tasks are relatively short and don't block.
 */
public interface TaskExecutor {
    int DEFAULT_THREAD_COUNT = Math.max(Runtime.getRuntime().availableProcessors() - 1, 2);

    /**
     * Submit tasks for execution. Implementation tries to distribute submitted tasks evenly across the worker threads.
     *
     * @param tasks tasks to execute
     *
     * @return Current instance
     */
    TaskExecutor submit(Consumer<Proactor> tasks);

    /**
     * Submit tasks for execution. Implementation tries to distribute submitted tasks evenly across the worker threads.
     *
     * @param tasks tasks to execute
     *
     * @return Current instance
     */
    TaskExecutor submit(List<Consumer<Proactor>> tasks);

    /**
     * Shutdown task executor. Returned promise is resolved once all internal processing is stopped and resources are released.
     *
     * @return {@link Promise} instance which is get resolved when shutdown is complete
     */
    Promise<Unit> shutdown();

    /**
     * Get information about number of processing threads.
     *
     * @return Number of threads in the underlying thread pool
     */
    int parallelism();

    /**
     * Create instance of task executor with default number of threads.
     *
     * @return Created instance
     */
    static TaskExecutor taskExecutor() {
        return taskExecutor(DEFAULT_THREAD_COUNT);
    }

    /**
     * Create instance of task executor with specified number of processing threads.
     *
     * @param threadCount requested number of threads. If requested number of threads is less than 2 then number of threads is set to 2.
     *
     * @return Created instance.
     */
    static TaskExecutor taskExecutor(int threadCount) {
        return new TaskExecutorImpl(Math.max(threadCount, 2));
    }
}
