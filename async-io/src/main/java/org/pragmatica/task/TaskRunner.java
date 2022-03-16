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
import org.pragmatica.lang.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Task execution pipeline.
 */
final class TaskRunner {
    private static final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    private final ActionableThreshold threshold;
    private final Proactor proactor;

    private volatile Task head;
    private volatile boolean shutdown = false;

    private static final VarHandle HEAD;

    static {
        try {
            final var lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(TaskRunner.class, "head", Task.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    TaskRunner(ActionableThreshold threshold, Proactor proactor) {
        this.threshold = threshold;
        this.proactor = proactor;
    }

    void start(ExecutorService executor) {
        executor.submit(this::run);
    }

    void push(final Consumer<Proactor> task) {
        final var newHead = new Task(task);
        Task oldHead;

        do {
            oldHead = head;
            newHead.next = oldHead;
        } while (!HEAD.compareAndSet(this, oldHead, newHead));
    }

    private void run() {
        while (!shutdown) {
            var head = swapHead();

            if (head == null) {
                var idleRunCount = 0;

                while (proactor.processCompletions() > 0) {
                    proactor.processSubmissions();

                    idleRunCount++;

                    if (idleRunCount == 4096) {
                        break;
                    }

                    Thread.onSpinWait();
                }

                if (idleRunCount == 0) {    // There were no tasks at all
                    Thread.onSpinWait();
                }
            } else {
                proactor.processCompletions();

                while (head != null) {
                    try {
                        head.task.accept(proactor);
                    } catch (Throwable e) {
                        LOG.error("Unexpected task exception", e);
                    }

                    head = head.next;
                }

                proactor.processSubmissions();
            }
        }
        threshold.registerEvent();
    }

    public void shutdown() {
        this.shutdown = true;
    }

    private static class Task {
        Consumer<Proactor> task;
        Task next;

        Task(Consumer<Proactor> task) {
            this.task = task;
        }
    }

    private Task swapHead() {
        Task head;

        do {
            head = this.head;
        } while (!HEAD.compareAndSet(this, head, null));

        Task current = head;
        Task prev = null;
        Task next;

        // Reverse list
        while (current != null) {
            next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }

        return prev;
    }
}
