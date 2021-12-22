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

package org.pfj.lang;

import org.pfj.io.async.SystemError;
import org.pfj.io.async.Timeout;
import org.pfj.lang.Functions.FN1;
import org.pfj.lang.io.scheduler.TaskExecutor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Consumer;

final class PromiseImpl<T> implements Promise<T> {
    @SuppressWarnings("rawtypes")
    private static final CompletionAction NOP = new CompletionAction<>(__ -> {}, null);

    @SuppressWarnings("unchecked")
    private volatile CompletionAction<T> head = NOP;
    private volatile CompletionAction<T> processed;
    private volatile Result<T> value;

    private static final VarHandle HEAD;
    private static final VarHandle VALUE;

    private static class CompletionAction<T> {
        private volatile CompletionAction<T> next;
        private final Consumer<Result<T>> action;
        private final PromiseImpl<?> dependency;

        private CompletionAction(Consumer<Result<T>> action, PromiseImpl<?> dependency) {
            this.action = action;
            this.dependency = dependency;
        }

        @Override
        public String toString() {
            return this == NOP ? "NOP" : "Action(" + (dependency == null ? "free" : dependency.toString()) + ')';
        }
    }

    static {
        try {
            final var lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(PromiseImpl.class, "head", CompletionAction.class);
            VALUE = lookup.findVarHandle(PromiseImpl.class, "value", Result.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    PromiseImpl(Result<T> value) {
        this.value = value;
        this.processed = value == null ? null : this.head;
    }

    @Override
    public <U> Promise<U> map(FN1<U, ? super T> mapper) {
        if (value != null) {
            return new PromiseImpl<>(value.map(mapper));
        }

        var result = new PromiseImpl<U>(null);

        push(new CompletionAction<T>(value -> result.resolve(value.map(mapper)), result));

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Promise<U> flatMap(FN1<Promise<U>, ? super T> mapper) {
        if (value != null) {
            return value.fold(f -> new PromiseImpl<>((Result<U>) value), mapper);
        }

        var result = new PromiseImpl<U>(null);

        push(new CompletionAction<T>(value -> value.fold(f -> new PromiseImpl<>((Result<U>) value), mapper)
                                                   .onResult(result::resolve),
                                     result));

        return result;
    }

    @Override
    public Promise<T> onResult(Consumer<Result<T>> action) {
        if (value != null) {
            action.accept(value);
        } else {
            push(new CompletionAction<T>(action, null));
        }

        return this;
    }

    @Override
    public Promise<T> resolve(Result<T> value) {
        if (VALUE.compareAndSet(this, null, value)) {
            ExecutorHolder.executor().submit(() -> runActions(value));
        }

        return this;
    }

    @Override
    public Result<T> join() {
        CompletionAction<T> action;

        while ((action = processed) == null) {
            Thread.yield();
        }

        while (action != NOP) {
            action.dependency.join();
            action = action.next;
        }

        return value;
    }

    @Override
    public Result<T> join(Timeout timeout) {
        return join(timeout.asNanos());
    }

    @Override
    public Promise<T> async(Consumer<Promise<T>> action) {
        ExecutorHolder.executor().submit(() -> action.accept(this));

        return this;
    }

    @Override
    public Promise<T> async(Timeout timeout, Consumer<Promise<T>> action) {
        return async(timeout.asNanos(), System.nanoTime(), action);
    }

    @Override
    public String toString() {
        return "Promise(" + (value == null ? "<>" : value.toString()) + ')';
    }

    private void runActions(Result<T> value) {
        CompletionAction<T> processed = NOP;
        CompletionAction<T> head;

        while ((head = swapHead()) != null) {
            while (head != null) {
                head.action.accept(value);
                var current = head;
                head = head.next;

                if (current.dependency != null) {
                    current.next = processed;
                    processed = current;
                }
            }
        }

        this.processed = processed;
    }

    private Result<T> join(long delayNanos) {
        var start = System.nanoTime();

        CompletionAction<T> action;

        while ((action = processed) == null) {
            Thread.yield();

            if (System.nanoTime() - start > delayNanos) {
                return SystemError.ETIME.result();
            }
        }

        while (action != NOP) {
            var currentNanoTime = System.nanoTime();

            if (currentNanoTime - start > delayNanos) {
                return SystemError.ETIME.result();
            }

            action.dependency.join(currentNanoTime - start);
            action = action.next;
        }

        return value;
    }

    private Promise<T> async(long delayNanos, long start, Consumer<Promise<T>> action) {
        ExecutorHolder.executor().submit(() -> {
            if (System.nanoTime() - start < delayNanos) {
                async(delayNanos, start, action);
            }
            action.accept(this);
        });

        return this;
    }

    private PromiseImpl<T> push(CompletionAction<T> newHead) {
        CompletionAction<T> oldHead;

        do {
            oldHead = head;
            newHead.next = oldHead;
        } while (!HEAD.compareAndSet(this, oldHead, newHead));

        return this;
    }

    private CompletionAction<T> swapHead() {
        CompletionAction<T> head;

        do {
            head = this.head;
        } while (!HEAD.compareAndSet(this, head, NOP));

        CompletionAction<T> current = head;
        CompletionAction<T> prev = null;
        CompletionAction<T> next;

        //Reverse list
        while (current != NOP) {
            next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }

        return prev;
    }

    private static final class ExecutorHolder {
        private static final TaskExecutor EXECUTOR = TaskExecutor.taskExecutor();

        static TaskExecutor executor() {
            return EXECUTOR;
        }
    }
}
