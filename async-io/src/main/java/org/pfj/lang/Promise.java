/*
 * Copyright (c) 2021 Sergiy Yevtushenko.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.lang;

import org.pfj.io.async.Timeout;
import org.pfj.lang.Functions.FN1;

import java.util.function.Consumer;

/**
 * The (perhaps not yet available) result of the asynchronous operation.
 */
public interface Promise<T> {
    /**
     * Resolve the Promise. This action can be performed only once, all subsequent attempts will be ignored and state of the Promise will remain
     * unchanged.
     *
     * @param value The value to resolve the Promise instance.
     *
     * @return Current instance
     */
    Promise<T> resolve(Result<T> value);

    /**
     * Transform current instance into the instance containing another type using provided transformation function.
     *
     * @param mapper The transformation function
     *
     * @return Transformed instance
     */
    <U> Promise<U> map(FN1<U, ? super T> mapper);

    /**
     * Compose current instance with the function which returns a Promise of another type.
     *
     * @param mapper The function to compose with
     *
     * @return Composed instance
     */
    <U> Promise<U> flatMap(FN1<Promise<U>, ? super T> mapper);

    /**
     * Run asynchronous task. The task will receive current instance of Promise as a parameter.
     *
     * @param action The task to run
     *
     * @return Current instance
     */
    Promise<T> async(Consumer<Promise<T>> action);

    /**
     * Run asynchronous task after specified timeout.
     *
     * @param timeout The timeout before task will be started
     * @param action  The task to run
     *
     * @return Current instance
     */
    Promise<T> async(Timeout timeout, Consumer<Promise<T>> action);

    /**
     * Wait indefinitely for completion of the Promise and all attached actions (see {@link #onResult(Consumer)}).
     *
     * @return Value of the Promise
     */
    Result<T> join();

    /**
     * Wait for completion of the Promise and all attached actions. The waiting time is limited to specified timeout. If timeout expires before
     * Promise is resolved, then returned result contains {@link org.pfj.io.async.SystemError#ETIME} error.
     *
     * @param timeout How long to wait for promise resolution
     *
     * @return Value of the Promise or timeout error.
     */
    Result<T> join(Timeout timeout);

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance. If promise is already resolved by the time of
     * invocation of this method, then provided action will be executed immediately.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    Promise<T> onResult(Consumer<Result<T>> action);

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance. If promise is already resolved by the time of
     * invocation of this method, then provided action will be executed immediately. Note that unlike {@link Promise#onResult(Consumer)}, the action
     * passed to this method does not receive parameter.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onResultDo(Runnable action) {
        return onResult(__ -> action.run());
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing {@link
     * Result.Success}. If instance is resolved with {@link Result} containing {@link Result.Failure}, then action will not be invoked. If promise is
     * already resolved by the time of invocation of this method and value is a {@link Result.Success}, then provided action will be executed
     * immediately.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onSuccess(Consumer<T> action) {
        return onResult(v -> v.onSuccess(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing {@link
     * Result.Success}. If instance is resolved with {@link Result} containing {@link Result.Failure}, then action will not be invoked. If promise is
     * already resolved by the time of invocation of this method and value is a {@link Result.Success}, then provided action will be executed
     * immediately. Note that unlike {@link Promise#onSuccess(Consumer)}, the action passed to this method does not receive parameter.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onSuccessDo(Runnable action) {
        return onResult(v -> v.onSuccessDo(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing {@link
     * Result.Failure}. If instance is resolved with {@link Result} containing {@link Result.Success}, then action will not be invoked. If promise is
     * already resolved by the time of invocation of this method and value is a {@link Result.Failure}, then provided action will be executed
     * immediately.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onFailure(Consumer<? super Cause> action) {
        return onResult(v -> v.onFailure(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing {@link
     * Result.Failure}. If instance is resolved with {@link Result} containing {@link Result.Success}, then action will not be invoked. If promise is
     * already resolved by the time of invocation of this method and value is a {@link Result.Failure}, then provided action will be executed
     * immediately. Note that unlike {@link Promise#onFailure(Consumer)}, the action passed to this method does not receive parameter.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onFailureDo(Runnable action) {
        return onResult(v -> v.onFailureDo(action));
    }

    /**
     * Resolve current instance with the {@link Result} containing {@link Result.Success}. If current instance is already resolved, then this method
     * invocation has no effect.
     *
     * @param value the value to resolve.
     *
     * @return Current instance
     */
    default Promise<T> success(T value) {
        return resolve(Result.success(value));
    }

    /**
     * Resolve current instance with the {@link Result} containing {@link Result.Failure}. If current instance is already resolved, then this method
     * invocation has no effect.
     *
     * @param value the value to resolve.
     *
     * @return Current instance
     */
    default Promise<T> failure(Cause cause) {
        return resolve(cause.result());
    }

    /**
     * Create an unresolved instance.
     *
     * @return Created instance.
     */
    static <R> Promise<R> promise() {
        return new PromiseImpl<>(null);
    }

    /**
     * Create an unresolved instance and run asynchronous action which will receive created instance as a parameter.
     *
     * @param consumer The action to run
     *
     * @return Created instance
     */
    static <R> Promise<R> promise(Consumer<Promise<R>> consumer) {
        return Promise.<R>promise().async(consumer);
    }

    /**
     * Create a resolved instance.
     *
     * @param value The value which will be stored in the created instance
     *
     * @return Created instance
     */
    static <R> Promise<R> resolved(Result<R> value) {
        return new PromiseImpl<>(value);
    }
}
