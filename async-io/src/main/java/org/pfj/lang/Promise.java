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

import org.pfj.io.async.Proactor;
import org.pfj.io.async.SystemError;
import org.pfj.io.async.Timeout;
import org.pfj.lang.Functions.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.pfj.io.async.util.ResultCollector.resultCollector;
import static org.pfj.lang.Tuple.*;

/**
 * The (perhaps not yet available) result of the asynchronous operation.
 */
public interface Promise<T> {
    /**
     * Resolve current instance. This action can be performed only once, all subsequent attempts will be ignored and state of the promise will remain
     * unchanged.
     *
     * @param value The value to resolve the Promise instance.
     * @return Current instance
     */
    Promise<T> resolve(Result<T> value);

    /**
     * Resolve current instance with {@link SystemError#ECANCELED} error.
     *
     * @return Current instance
     */
    default Promise<T> cancel() {
        return resolve(SystemError.ECANCELED.result());
    }

    boolean isResolved();

    /**
     * Transform current instance into the instance containing another type using provided transformation function.
     *
     * @param mapper The transformation function
     * @return Transformed instance
     */
    <U> Promise<U> map(FN1<U, ? super T> mapper);

    /**
     * Compose current instance with the function which returns a Promise of another type.
     *
     * @param mapper The function to compose with
     * @return Composed instance
     */
    <U> Promise<U> flatMap(FN1<Promise<U>, ? super T> mapper);

    /**
     * Run asynchronous task. The task will receive current instance of Promise as a parameter.
     *
     * @param action The task to run
     * @return Current instance
     */
    default Promise<T> async(Consumer<Promise<T>> action) {
        return async((promise, __) -> action.accept(promise));
    }

    /**
     * Run asynchronous task. The task will receive current instance of Promise and an instance of {@link Proactor} as a parameter.
     *
     * @param action The task to run
     * @return Current instance
     */
    Promise<T> async(BiConsumer<Promise<T>, Proactor> action);

    /**
     * Run asynchronous task after specified timeout.
     *
     * @param timeout The timeout before task will be started
     * @param action  The task to run
     * @return Current instance
     */
    default Promise<T> async(Timeout timeout, Consumer<Promise<T>> action) {
        return async(timeout, (promise, __) -> action.accept(promise));
    }

    /**
     * Run asynchronous task after specified timeout.
     *
     * @param timeout The timeout before task will be started
     * @param action  The task to run
     * @return Current instance
     */
    Promise<T> async(Timeout timeout, BiConsumer<Promise<T>, Proactor> action);

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
     * @return Value of the Promise or timeout error.
     */
    Result<T> join(Timeout timeout);

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance. If promise is already resolved by the time of
     * invocation of this method, then provided action will be executed immediately.
     *
     * @param action the action to execute
     * @return Current instance
     */
    Promise<T> onResult(Consumer<Result<T>> action);

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance. If promise is already resolved by the time of
     * invocation of this method, then provided action will be executed immediately. Note that unlike {@link Promise#onResult(Consumer)}, the action
     * passed to this method does not receive parameter.
     *
     * @param action the action to execute
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
     * @return Current instance
     */
    default Promise<T> onSuccess(Consumer<T> action) {
        return onResult(result -> result.onSuccess(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing {@link
     * Result.Success}. If instance is resolved with {@link Result} containing {@link Result.Failure}, then action will not be invoked. If promise is
     * already resolved by the time of invocation of this method and value is a {@link Result.Success}, then provided action will be executed
     * immediately. Note that unlike {@link Promise#onSuccess(Consumer)}, the action passed to this method does not receive parameter.
     *
     * @param action the action to execute
     * @return Current instance
     */
    default Promise<T> onSuccessDo(Runnable action) {
        return onResult(result -> result.onSuccessDo(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing {@link
     * Result.Failure}. If instance is resolved with {@link Result} containing {@link Result.Success}, then action will not be invoked. If promise is
     * already resolved by the time of invocation of this method and value is a {@link Result.Failure}, then provided action will be executed
     * immediately.
     *
     * @param action the action to execute
     * @return Current instance
     */
    default Promise<T> onFailure(Consumer<? super Cause> action) {
        return onResult(result -> result.onFailure(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing {@link
     * Result.Failure}. If instance is resolved with {@link Result} containing {@link Result.Success}, then action will not be invoked. If promise is
     * already resolved by the time of invocation of this method and value is a {@link Result.Failure}, then provided action will be executed
     * immediately. Note that unlike {@link Promise#onFailure(Consumer)}, the action passed to this method does not receive parameter.
     *
     * @param action the action to execute
     * @return Current instance
     */
    default Promise<T> onFailureDo(Runnable action) {
        return onResult(result -> result.onFailureDo(action));
    }

    /**
     * Resolve current instance with the {@link Result} containing {@link Result.Success}. If current instance is already resolved, then this method
     * invocation has no effect.
     *
     * @param value the value to resolve.
     * @return Current instance
     */
    default Promise<T> success(T value) {
        return resolve(Result.success(value));
    }

    /**
     * Resolve current instance with the {@link Result} containing {@link Result.Failure}. If current instance is already resolved, then this method
     * invocation has no effect.
     *
     * @param cause the failure cause.
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
     * @return Created instance
     */
    static <R> Promise<R> promise(Consumer<Promise<R>> consumer) {
        return Promise.<R>promise().async(consumer);
    }

    /**
     * Create a resolved instance.
     *
     * @param value The value which will be stored in the created instance
     * @return Created instance
     */
    static <R> Promise<R> resolved(Result<R> value) {
        return new PromiseImpl<>(value);
    }

    static <T1> Mapper1<T1> all(Promise<T1> promise1) {
        return () -> Promise.promise(
            promise -> promise1.onResult(
                result -> result.map(Tuple::tuple)
                    .onSuccess(promise::success)
                    .onFailure(promise::failure)));
    }

    private static <R> Promise<R> setup(FNx<R> transformer, Promise<?>... promises) {
        var promise = Promise.<R>promise();
        var collector = resultCollector(promises.length, values -> promise.success(transformer.apply(values)));

        int count = 0;
        for (var p : promises) {
            final var index = count++;
            p.onResult(result -> result.accept(promise::failure, value -> collector.registerEvent(index, value)));
        }

        return promise;
    }

    @SuppressWarnings("unchecked")
    static <T1, T2> Mapper2<T1, T2> all(Promise<T1> promise1, Promise<T2> promise2) {
        return () -> setup(values -> tuple((T1) values[0], (T2) values[1]), promise1, promise2);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3> Mapper3<T1, T2, T3> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3) {

        return () -> setup(values -> tuple((T1) values[1], (T2) values[2], (T3) values[3]), promise1, promise2, promise3);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4> Mapper4<T1, T2, T3, T4> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4) {

        return () -> setup(values -> tuple((T1) values[1], (T2) values[2], (T3) values[3], (T4) values[4]),
            promise1, promise2, promise3, promise4);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5> Mapper5<T1, T2, T3, T4, T5> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4, Promise<T5> promise5) {

        return () -> setup(values -> tuple((T1) values[1], (T2) values[2], (T3) values[3], (T4) values[4], (T5) values[5]),
            promise1, promise2, promise3, promise4, promise5);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6> Mapper6<T1, T2, T3, T4, T5, T6> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4,
        Promise<T5> promise5, Promise<T6> promise6) {

        return () -> setup(values -> tuple(
                (T1) values[1], (T2) values[2], (T3) values[3], (T4) values[4], (T5) values[5], (T6) values[6]),
            promise1, promise2, promise3, promise4, promise5, promise6);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7> Mapper7<T1, T2, T3, T4, T5, T6, T7> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4,
        Promise<T5> promise5, Promise<T6> promise6, Promise<T7> promise7) {

        return () -> setup(values -> tuple(
                (T1) values[1], (T2) values[2], (T3) values[3], (T4) values[4],
                (T5) values[5], (T6) values[6], (T7) values[7]),
            promise1, promise2, promise3, promise4, promise5, promise6, promise7);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4,
        Promise<T5> promise5, Promise<T6> promise6, Promise<T7> promise7, Promise<T8> promise8) {

        return () -> setup(values -> tuple(
                (T1) values[1], (T2) values[2], (T3) values[3], (T4) values[4],
                (T5) values[5], (T6) values[6], (T7) values[7], (T8) values[8]),
            promise1, promise2, promise3, promise4, promise5, promise6, promise7, promise8);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4, Promise<T5> promise5,
        Promise<T6> promise6, Promise<T7> promise7, Promise<T8> promise8, Promise<T9> promise9) {

        return () -> setup(values -> tuple(
            (T1) values[1], (T2) values[2], (T3) values[3], (T4) values[4], (T5) values[5],
            (T6) values[6], (T7) values[7], (T8) values[8], (T9) values[9]),
            promise1, promise2, promise3, promise4, promise5, promise6, promise7, promise8, promise9);
    }

    interface Mapper1<T1> {
        Promise<Tuple1<T1>> id();

        default <R> Promise<R> map(FN1<R, T1> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN1<Promise<R>, T1> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper2<T1, T2> {
        Promise<Tuple2<T1, T2>> id();

        default <R> Promise<R> map(FN2<R, T1, T2> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN2<Promise<R>, T1, T2> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper3<T1, T2, T3> {
        Promise<Tuple3<T1, T2, T3>> id();

        default <R> Promise<R> map(FN3<R, T1, T2, T3> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN3<Promise<R>, T1, T2, T3> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper4<T1, T2, T3, T4> {
        Promise<Tuple4<T1, T2, T3, T4>> id();

        default <R> Promise<R> map(FN4<R, T1, T2, T3, T4> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN4<Promise<R>, T1, T2, T3, T4> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper5<T1, T2, T3, T4, T5> {
        Promise<Tuple5<T1, T2, T3, T4, T5>> id();

        default <R> Promise<R> map(FN5<R, T1, T2, T3, T4, T5> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN5<Promise<R>, T1, T2, T3, T4, T5> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper6<T1, T2, T3, T4, T5, T6> {
        Promise<Tuple6<T1, T2, T3, T4, T5, T6>> id();

        default <R> Promise<R> map(FN6<R, T1, T2, T3, T4, T5, T6> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN6<Promise<R>, T1, T2, T3, T4, T5, T6> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper7<T1, T2, T3, T4, T5, T6, T7> {
        Promise<Tuple7<T1, T2, T3, T4, T5, T6, T7>> id();

        default <R> Promise<R> map(FN7<R, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN7<Promise<R>, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> {
        Promise<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> id();

        default <R> Promise<R> map(FN8<R, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    interface Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        Promise<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> id();

        default <R> Promise<R> map(FN9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(FN9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }
}
