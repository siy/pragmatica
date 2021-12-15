package org.pfj.lang;

import org.pfj.lang.Functions.FN1;

import java.util.function.Consumer;

public interface Promise<T> {
    <U> Promise<U> map(FN1<U, ? super T> mapper);

    <U> Promise<U> flatMap(FN1<Promise<U>, ? super T> mapper);

    Promise<T> thenDo(Consumer<Result<T>> action);

    default Promise<T> onSuccess(Consumer<T> action) {
        return thenDo(v -> v.onSuccess(action));
    }

    default Promise<T> onFailure(Consumer<? super Cause> action) {
        return thenDo(v -> v.onFailure(action));
    }

    Promise<T> resolve(Result<T> value);

    default Promise<T> success(T value) {
        return resolve(Result.success(value));
    }

    default Promise<T> failure(Cause cause) {
        return resolve(cause.result());
    }

    Promise<T> async(Consumer<Promise<T>> action);

    static <R> Promise<R> promise() {
        return new PromiseImpl<>(null);
    }

    static <R> Promise<R> promise(Consumer<Promise<R>> consumer) {
        var result = Promise.<R>promise();
        consumer.accept(result);
        return result;
    }

    static <R> Promise<R> promise(Result<R> value) {
        return new PromiseImpl<>(value);
    }
}
