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

import org.pfj.lang.Functions.*;
import org.pfj.lang.Result.Failure;
import org.pfj.lang.Result.Success;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.pfj.lang.Tuple.*;


/**
 * Representation of the operation result. The result can be either success or failure.
 * In case of success it holds value returned by the operation. In case of failure it
 * holds a failure description.
 *
 * @param <T> Type of value in case of success.
 */
public sealed interface Result<T> permits Success, Failure {
    /**
     * Transform operation result value into value of other type and wrap new
     * value into {@link Result}. Transformation takes place if current instance
     * (this) contains successful result, otherwise current instance remains
     * unchanged and transformation function is not invoked.
     *
     * @param mapper Function to transform successful value
     *
     * @return transformed value (in case of success) or current instance (in case of failure)
     */
    @SuppressWarnings("unchecked")
    default <R> Result<R> map(FN1<R, ? super T> mapper) {
        return fold(l -> (Result<R>) this, r -> success(mapper.apply(r)));
    }

    /**
     * Transform operation result into another operation result. In case if current
     * instance (this) is an error, transformation function is not invoked
     * and value remains the same.
     *
     * @param mapper Function to apply to result
     *
     * @return transformed value (in case of success) or current instance (in case of failure)
     */
    @SuppressWarnings("unchecked")
    default <R> Result<R> flatMap(FN1<Result<R>, ? super T> mapper) {
        return fold(t -> (Result<R>) this, mapper);
    }

    /**
     * Apply consumers to result value. Note that depending on the result (success or failure) only one consumer will be
     * applied at a time.
     *
     * @param failureConsumer Consumer for failure result
     * @param successConsumer Consumer for success result
     *
     * @return current instance
     */
    default Result<T> apply(Consumer<? super Cause> failureConsumer, Consumer<? super T> successConsumer) {
        return fold(t -> {
            failureConsumer.accept(t);
            return this;
        }, t -> {
            successConsumer.accept(t);
            return this;
        });
    }

    /**
     * Pass successful operation result value into provided consumer.
     *
     * @param consumer Consumer to pass value to
     *
     * @return current instance for fluent call chaining
     */
    default Result<T> onSuccess(Consumer<T> consumer) {
        fold(v -> null, v -> {
            consumer.accept(v);
            return null;
        });
        return this;
    }

    /**
     * Run provided action in case of success.
     *
     * @return current instance for fluent call chaining
     */
    default Result<T> onSuccessDo(Runnable action) {
        fold(v -> null, v -> {
            action.run();
            return null;
        });
        return this;
    }

    /**
     * Pass failure operation result value into provided consumer.
     *
     * @param consumer Consumer to pass value to
     *
     * @return current instance for fluent call chaining
     */
    default Result<T> onFailure(Consumer<? super Cause> consumer) {
        fold(v -> {
            consumer.accept(v);
            return null;
        }, v -> null);
        return this;
    }

    /**
     * Run provided action in case of failure.
     *
     * @return current instance for fluent call chaining
     */
    default Result<T> onFailureDo(Runnable action) {
        fold(v -> {
            action.run();
            return null;
        }, v -> null);
        return this;
    }

    /**
     * Convert instance into {@link Option} of the same value type. Successful instance
     * is converted into present {@link Option} and failure - into empty {@link Option}.
     * Note that during such a conversion error information is get lost.
     *
     * @return {@link Option} instance which is present in case of success and missing
     * in case of failure.
     */
    default Option<T> toOption() {
        return fold(t1 -> Option.empty(), Option::option);
    }

    /**
     * Convert instance into {@link Optional} of the same value type. Successful instance
     * is converted into present {@link Optional} and failure - into empty {@link Optional}.
     * Note that during such a conversion error information is get lost.
     *
     * @return {@link Optional} instance which is present in case of success and missing
     * in case of failure.
     */
    default Optional<T> toOptional() {
        return fold(t1 -> Optional.empty(), Optional::of);
    }

    /**
     * Check if instance is success.
     *
     * @return {@code true} if instance is success and {@code false} otherwise
     */
    default boolean isSuccess() {
        return fold(__ -> false, __ -> true);
    }

    /**
     * Check if instance is failure.
     *
     * @return {@code true} if instance is failure and {@code false} otherwise
     */
    default boolean isFailure() {
        return fold(__ -> true, __ -> false);
    }

    /**
     * Filter instance against provided predicate. If predicate returns {@code true} then
     * instance remains unchanged. If predicate returns {@code false}, then failure instance in created
     * using given {@link Cause}.
     *
     * @param cause     failure to use in case if predicate returns {@code false}
     * @param predicate predicate to invoke
     *
     * @return current instance if predicate returns {@code true} or {@link Failure} instance if predicate returns {@code false}
     */
    default Result<T> filter(Cause cause, Predicate<T> predicate) {
        return fold(v -> this, v -> predicate.test(v) ? this : failure(cause));
    }

    /**
     * Filter instance against provided predicate. If predicate returns {@code true} then
     * instance remains unchanged. If predicate returns {@code false}, then failure instance in created
     * using {@link Cause} created by provided function.
     *
     * @param causeMapper function which transforms the tested value into instance of {@link Cause} if predicate returns {@code false}
     * @param predicate   predicate to invoke
     *
     * @return current instance if predicate returns {@code true} or {@link Failure} instance if predicate returns {@code false}
     */
    default Result<T> filter(FN1<Cause, T> causeMapper, Predicate<T> predicate) {
        return fold(v -> this, v -> predicate.test(v) ? this : failure(causeMapper.apply(v)));
    }

    /**
     * Handle both possible states (success/failure) and produce single value from it.
     *
     * @param failureMapper function to transform failure into value
     * @param successMapper function to transform success into value
     *
     * @return result of application of one of the mappers.
     */
    <R> R fold(FN1<? extends R, ? super Cause> failureMapper, FN1<? extends R, ? super T> successMapper);

    /**
     * Create an instance of successful operation result.
     *
     * @param value Operation result
     *
     * @return created instance
     */
    static <R> Result<R> success(R value) {
        return new Success<>(value);
    }

    final class Success<T> implements Result<T> {
        private final T value;

        private Success(T value) {
            this.value = value;
        }

        @Override
        public <R> R fold(FN1<? extends R, ? super Cause> failureMapper, FN1<? extends R, ? super T> successMapper) {
            return successMapper.apply(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            return o instanceof Success<?> success && value.equals(success.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Success(" + value.toString() + ")";
        }
    }

    /**
     * Create an instance of failure result.
     *
     * @param value Operation error value
     *
     * @return created instance
     */
    static <R> Result<R> failure(Cause value) {
        return new Failure<>(value);
    }

    final class Failure<T> implements Result<T> {
        private final Cause value;

        private Failure(Cause value) {
            this.value = value;
        }

        @Override
        public <R> R fold(FN1<? extends R, ? super Cause> failureMapper, FN1<? extends R, ? super T> successMapper) {
            return failureMapper.apply(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            return o instanceof Failure<?> failure && value.equals(failure.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Failure(" + value + ")";
        }
    }

    /**
     * Wrap value returned by provided lambda into success {@link Result} if call succeeds
     * or into failure {@link Result} if call throws exception.
     *
     * @param exceptionMapper the function which will transform exception into instance of {@link Cause}
     * @param supplier        the call to wrap
     *
     * @return result of execution of the provided lambda wrapped into {@link Result}
     */
    static <R> Result<R> lift(FN1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingSupplier<R> supplier) {
        try {
            return success(supplier.get());
        } catch (Throwable e) {
            return failure(exceptionMapper.apply(e));
        }
    }

    /**
     * Transform list of {@link Result} instances into {@link Result} with list of values.
     *
     * @param resultList input list
     *
     * @return success instance if all {@link Result} instances in list are successes or
     * failure instance with any instances in list is a failure
     */
    static <T> Result<List<T>> flatten(List<Result<T>> resultList) {
        var failure = new Cause[1];
        var values = new ArrayList<T>();

        resultList.forEach(val -> val.fold(f -> failure[0] = f, values::add));

        return failure[0] != null ? failure(failure[0]) : success(values);
    }

    /**
     * Find and return first success instance among provided.
     *
     * @param first   first input result
     * @param results remaining input results
     *
     * @return first success instance among provided
     */
    @SafeVarargs
    static <T> Result<T> any(Result<T> first, Result<T>... results) {
        if (first.isSuccess()) {
            return first;
        }

        for (var result : results) {
            if (result.isSuccess()) {
                return result;
            }
        }

        return first;
    }

    /**
     * Lazy version of the {@link #any(Result, Result[])}.
     *
     * @param first     first instance to check
     * @param suppliers suppliers which provide remaining instances for check
     *
     * @return first success instance among provided
     */
    @SafeVarargs
    static <T> Result<T> any(Result<T> first, Supplier<Result<T>>... suppliers) {
        if (first.isSuccess()) {
            return first;
        }

        for (var supplier : suppliers) {
            var result = supplier.get();

            if (result.isSuccess()) {
                return result;
            }
        }

        return first;
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper1} prepared for further transformation.
     */
    static <T1> Mapper1<T1> all(Result<T1> value) {
        return () -> value.flatMap(vv1 -> success(tuple(vv1)));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper2} prepared for further transformation.
     */
    static <T1, T2> Mapper2<T1, T2> all(Result<T1> value1, Result<T2> value2) {
        return () -> value1.flatMap(vv1 -> value2.flatMap(vv2 -> success(tuple(vv1, vv2))));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper3} prepared for further transformation.
     */
    static <T1, T2, T3> Mapper3<T1, T2, T3> all(Result<T1> value1, Result<T2> value2, Result<T3> value3) {
        return () -> value1.flatMap(vv1 -> value2.flatMap(vv2 -> value3.flatMap(vv3 -> success(tuple(vv1, vv2, vv3)))));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper4} prepared for further transformation.
     */
    static <T1, T2, T3, T4> Mapper4<T1, T2, T3, T4> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3, Result<T4> value4
    ) {
        return () -> value1.flatMap(
            vv1 -> value2.flatMap(
                vv2 -> value3.flatMap(
                    vv3 -> value4.flatMap(
                        vv4 -> success(tuple(vv1, vv2, vv3, vv4))))));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper5} prepared for further transformation.
     */
    static <T1, T2, T3, T4, T5> Mapper5<T1, T2, T3, T4, T5> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3, Result<T4> value4, Result<T5> value5
    ) {
        return () -> value1.flatMap(
            vv1 -> value2.flatMap(
                vv2 -> value3.flatMap(
                    vv3 -> value4.flatMap(
                        vv4 -> value5.flatMap(
                            vv5 -> success(tuple(vv1, vv2, vv3, vv4, vv5)))))));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper6} prepared for further transformation.
     */
    static <T1, T2, T3, T4, T5, T6> Mapper6<T1, T2, T3, T4, T5, T6> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6
    ) {
        return () -> value1.flatMap(
            vv1 -> value2.flatMap(
                vv2 -> value3.flatMap(
                    vv3 -> value4.flatMap(
                        vv4 -> value5.flatMap(
                            vv5 -> value6.flatMap(
                                vv6 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6))))))));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper7} prepared for further transformation.
     */
    static <T1, T2, T3, T4, T5, T6, T7> Mapper7<T1, T2, T3, T4, T5, T6, T7> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6,
        Result<T7> value7
    ) {
        return () -> value1.flatMap(
            vv1 -> value2.flatMap(
                vv2 -> value3.flatMap(
                    vv3 -> value4.flatMap(
                        vv4 -> value5.flatMap(
                            vv5 -> value6.flatMap(
                                vv6 -> value7.flatMap(
                                    vv7 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7)))))))));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper8} prepared for further transformation.
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6,
        Result<T7> value7, Result<T8> value8
    ) {
        return () -> value1.flatMap(
            vv1 -> value2.flatMap(
                vv2 -> value3.flatMap(
                    vv3 -> value4.flatMap(
                        vv4 -> value5.flatMap(
                            vv5 -> value6.flatMap(
                                vv6 -> value7.flatMap(
                                    vv7 -> value8.flatMap(
                                        vv8 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7, vv8))))))))));
    }

    /**
     * Transform provided results into single result containing tuple of values. The result is failure
     * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
     *
     * @return {@link Mapper9} prepared for further transformation.
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6,
        Result<T7> value7, Result<T8> value8, Result<T9> value9
    ) {
        return () -> value1.flatMap(
            vv1 -> value2.flatMap(
                vv2 -> value3.flatMap(
                    vv3 -> value4.flatMap(
                        vv4 -> value5.flatMap(
                            vv5 -> value6.flatMap(
                                vv6 -> value7.flatMap(
                                    vv7 -> value8.flatMap(
                                        vv8 -> value9.flatMap(
                                            vv9 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7, vv8, vv9)))))))))));
    }

    /**
     * Helper interface for convenient {@link Tuple1} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper1<T1> {
        Result<Tuple1<T1>> id();

        default <R> Result<R> map(FN1<R, T1> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN1<Result<R>, T1> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple2} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper2<T1, T2> {
        Result<Tuple2<T1, T2>> id();

        default <R> Result<R> map(FN2<R, T1, T2> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN2<Result<R>, T1, T2> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple3} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper3<T1, T2, T3> {
        Result<Tuple3<T1, T2, T3>> id();

        default <R> Result<R> map(FN3<R, T1, T2, T3> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN3<Result<R>, T1, T2, T3> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple4} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper4<T1, T2, T3, T4> {
        Result<Tuple4<T1, T2, T3, T4>> id();

        default <R> Result<R> map(FN4<R, T1, T2, T3, T4> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN4<Result<R>, T1, T2, T3, T4> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple5} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper5<T1, T2, T3, T4, T5> {
        Result<Tuple5<T1, T2, T3, T4, T5>> id();

        default <R> Result<R> map(FN5<R, T1, T2, T3, T4, T5> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN5<Result<R>, T1, T2, T3, T4, T5> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple6} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper6<T1, T2, T3, T4, T5, T6> {
        Result<Tuple6<T1, T2, T3, T4, T5, T6>> id();

        default <R> Result<R> map(FN6<R, T1, T2, T3, T4, T5, T6> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN6<Result<R>, T1, T2, T3, T4, T5, T6> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple7} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper7<T1, T2, T3, T4, T5, T6, T7> {
        Result<Tuple7<T1, T2, T3, T4, T5, T6, T7>> id();

        default <R> Result<R> map(FN7<R, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN7<Result<R>, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple8} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> {
        Result<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> id();

        default <R> Result<R> map(FN8<R, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN8<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    /**
     * Helper interface for convenient {@link Tuple9} transformation.
     * In case if you need to return a tuple, it might be more convenient to return this interface instead.
     * For example, instead of this:
     * <blockquote><pre>
     *     return tuple(value, ...);
     * </pre></blockquote>
     * return this:
     * <blockquote><pre>
     *     return () -> tuple(value, ...);
     * </pre></blockquote>
     */
    interface Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        Result<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> id();

        default <R> Result<R> map(FN9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(FN9<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }
}
