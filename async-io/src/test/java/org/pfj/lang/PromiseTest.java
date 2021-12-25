package org.pfj.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.pfj.lang.Tuple.tuple;

class PromiseTest {
    @Test
    void promiseCanBeResolved() {
        var promise = Promise.<Integer>promise();
        var ref = new AtomicInteger();

        promise.resolve(Result.success(1));
        promise.onSuccess(ref::set);

        promise.join().onSuccess(v -> assertEquals(1, v));

        assertEquals(1, ref.get());
    }

    @Test
    void actionIsExecutedAfterResolution() {
        var ref = new AtomicInteger();
        var promise = Promise.<Integer>promise().onSuccess(ref::set);

        assertEquals(0, ref.get());

        promise.resolve(Result.success(1));
        promise.join();

        assertEquals(1, ref.get());
    }

    @Test
    void multipleActionsAreExecutedAfterResolution() {
        var ref1 = new AtomicReference<Integer>();
        var ref2 = new AtomicReference<String>();
        var ref3 = new AtomicReference<Long>();
        var ref4 = new AtomicInteger();

        var promise = Promise.<Integer>promise();

        promise
            .onSuccess(ref1::set)
            .map(Objects::toString)
            .onSuccess(ref2::set)
            .map(Long::parseLong)
            .onSuccess(ref3::set)
            .onSuccessDo(() -> {
                try {
                    Thread.sleep(50);
                    ref4.incrementAndGet();
                } catch (InterruptedException e) {
                    //ignore
                }
            });

        assertNull(ref1.get());
        assertNull(ref2.get());
        assertNull(ref3.get());

        promise.resolve(Result.success(1));
        promise.join();

        assertEquals(1, ref1.get());
        assertEquals("1", ref2.get());
        assertEquals(1L, ref3.get());
        assertEquals(1, ref4.get());
    }

    @Test
    void all1ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();

        var allPromise = Promise.all(promise1).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);

        allPromise.join()
            .onSuccess(tuple -> assertEquals(tuple(1), tuple))
            .onFailureDo(Assertions::fail);
    }

    @Disabled
    @Test
    //TODO: fix
    void all2ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();

        var allPromise = Promise.all(promise1, promise2).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise1.success(2);

        allPromise.join()
            .onSuccess(tuple -> assertEquals(tuple(1, 2), tuple))
            .onFailureDo(Assertions::fail);
    }
}