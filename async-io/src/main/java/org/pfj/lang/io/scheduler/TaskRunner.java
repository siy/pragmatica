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

    private final ExecutorService executor;
    private final ActionableThreshold threshold;

    private volatile int ioFactor = Integer.MAX_VALUE;
    private volatile Task head;

    private static final VarHandle HEAD;

    static {
        try {
            final var lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(TaskRunner.class, "head", Task.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    TaskRunner(ExecutorService executor, ActionableThreshold threshold) {
        this.executor = executor;
        this.threshold = threshold;
    }

    void start() {
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

    void adjustIoFactor(int newIoFactor) {
        ioFactor = newIoFactor;
    }

    private void run() {
        var proactor = createProactor();
        int idleRunCount = 0;

        while (!executor.isShutdown()) {
            var head = swapHead();

            if (head == null) {
                idleRunCount++;

                if (idleRunCount == 2048) {
                    Thread.yield();
                    idleRunCount = 0;
                }
            } else {
                int taskCount = 0;

                while (head != null) {
                    try {
                        head.task.accept(proactor);
                    } catch (Throwable e) {
                        LOG.error("Unexpected task exception", e);
                    }

                    head = head.next;

                    taskCount++;

                    if (taskCount > ioFactor) {
                        proactor.processIO();
                        taskCount = 0;
                    }
                }
            }
            proactor.processIO();
        }
        proactor.close();

        threshold.registerEvent();
    }

    private Proactor createProactor() {
        try {
            return Proactor.proactor();
        } catch (Throwable e) {
            LOG.error("Unable to init Proactor", e);

            System.exit(-1);
            throw new RuntimeException("Unreachable statement");
        }
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
