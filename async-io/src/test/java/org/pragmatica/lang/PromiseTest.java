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

package org.pragmatica.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.Timeout;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.io.async.SystemError.*;

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
    void resolvedPromiseCanBeCreated() {
        var promise = Promise.resolved(Result.success(1));

        assertTrue(promise.isResolved());
    }

    @Test
    void promiseCanBeCancelled() {
        var promise = Promise.<Integer>promise();

        assertEquals(ECANCELED.result(), promise.cancel().join());
    }

    @Test
    void successActionsAreExecutedAfterResolutionWithSuccess() {
        var ref1 = new AtomicInteger();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onSuccess(ref1::set)
                             .onSuccessDo(() -> ref2.set(true));

        assertEquals(0, ref1.get());
        assertFalse(ref2.get());

        promise.resolve(Result.success(1)).join();

        assertEquals(1, ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void successActionsAreNotExecutedAfterResolutionWithFailure() {
        var ref1 = new AtomicInteger();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onSuccess(ref1::set)
                             .onSuccessDo(() -> ref2.set(true));

        assertEquals(0, ref1.get());
        assertFalse(ref2.get());

        promise.resolve(EFAULT.result()).join();

        assertEquals(0, ref1.get());
        assertFalse(ref2.get());
    }

    @Test
    void failureActionsAreExecutedAfterResolutionWithFailure() {
        var ref1 = new AtomicReference<Cause>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onFailure(ref1::set)
                             .onFailureDo(() -> ref2.set(true));

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.resolve(EFAULT.result()).join();

        assertEquals(EFAULT, ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void failureActionsAreNotExecutedAfterResolutionWithSuccess() {
        var ref1 = new AtomicReference<Cause>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onFailure(ref1::set)
                             .onFailureDo(() -> ref2.set(true));

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.resolve(Result.success(1)).join();

        assertNull(ref1.get());
        assertFalse(ref2.get());
    }

    @Test
    void resultActionsAreExecutedAfterResolutionWithSuccess() {
        var ref1 = new AtomicReference<Result<Integer>>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onResult(ref1::set)
                             .onResultDo(() -> ref2.set(true));

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.resolve(Result.success(1)).join();

        assertEquals(Result.success(1), ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void resultActionsAreExecutedAfterResolutionWithFailure() {
        var ref1 = new AtomicReference<Result<Integer>>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onResult(ref1::set)
                             .onResultDo(() -> ref2.set(true));

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.resolve(EFAULT.result()).join();

        Assertions.assertEquals(EFAULT.result(), ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void joinReturnsErrorAfterTimeoutThenPromiseRemainsInSameStateAndNoActionsAreExecuted() {
        var ref1 = new AtomicReference<Result<Integer>>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onResult(ref1::set)
                             .onResultDo(() -> ref2.set(true));

        assertNull(ref1.get());
        assertFalse(ref2.get());

        var result = promise.join(Timeout.timeout(10).millis());

        Assertions.assertEquals(result, ETIME.result());

        assertNull(ref1.get());
        assertFalse(ref2.get());
    }

    @Test
    void asyncActionIsExecutedAfterTimeout() {
        var ref1 = new AtomicLong(System.nanoTime());
        var ref2 = new AtomicLong();
        var ref3 = new AtomicLong();

        var promise = Promise.<Integer>promise()
                             .async(Timeout.timeout(10).millis(), p -> {
                                 ref2.set(System.nanoTime());
                                 p.resolve(Result.success(1))
                                  .onResultDo(() -> ref3.set(System.nanoTime()));
                             });

        promise.join();

        //For informational purposes
        System.out.println("Diff 1: " + (ref2.get() - ref1.get()));
        System.out.println("Diff 2: " + (ref3.get() - ref2.get()));
        System.out.println("Diff total: " + (ref3.get() - ref1.get()));

        // Expect that timeout should be between requested and twice as requested
        assertTrue((ref2.get() - ref1.get()) >= Timeout.timeout(10).millis().nanoseconds());
        assertTrue((ref2.get() - ref1.get()) < Timeout.timeout(20).millis().nanoseconds());
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

        promise.resolve(Result.success(1)).join();

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
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all2ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();

        var allPromise = Promise.all(promise1, promise2).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all3ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();

        var allPromise = Promise.all(promise1, promise2, promise3).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);
        promise3.success(3);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all4ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();

        var allPromise = Promise.all(
            promise1, promise2, promise3, promise4).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);
        promise3.success(3);
        promise4.success(4);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all5ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();

        var allPromise = Promise.all(
            promise1, promise2, promise3, promise4, promise5).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);
        promise3.success(3);
        promise4.success(4);
        promise5.success(5);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all6ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();

        var allPromise = Promise.all(
            promise1, promise2, promise3, promise4, promise5, promise6).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);
        promise3.success(3);
        promise4.success(4);
        promise5.success(5);
        promise6.success(6);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all7ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();
        var promise7 = Promise.<Integer>promise();

        var allPromise = Promise.all(
            promise1, promise2, promise3, promise4, promise5, promise6, promise7).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);
        promise3.success(3);
        promise4.success(4);
        promise5.success(5);
        promise6.success(6);
        promise7.success(7);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6, 7), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all8ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();
        var promise7 = Promise.<Integer>promise();
        var promise8 = Promise.<Integer>promise();

        var allPromise = Promise.all(
            promise1, promise2, promise3, promise4, promise5,
            promise6, promise7, promise8).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);
        promise3.success(3);
        promise4.success(4);
        promise5.success(5);
        promise6.success(6);
        promise7.success(7);
        promise8.success(8);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6, 7, 8), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void all9ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();
        var promise7 = Promise.<Integer>promise();
        var promise8 = Promise.<Integer>promise();
        var promise9 = Promise.<Integer>promise();

        var allPromise = Promise.all(
            promise1, promise2, promise3, promise4, promise5,
            promise6, promise7, promise8, promise9).id();

        assertFalse(allPromise.isResolved());

        promise1.success(1);
        promise2.success(2);
        promise3.success(3);
        promise4.success(4);
        promise5.success(5);
        promise6.success(6);
        promise7.success(7);
        promise8.success(8);
        promise9.success(9);

        allPromise.join()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6, 7, 8, 9), tuple))
                  .onFailureDo(Assertions::fail);
    }

    @Test
    void promiseCanBeConfiguredAsynchronously() throws InterruptedException {
        var ref = new AtomicInteger(0);
        var latch = new CountDownLatch(1);

        var promise = Promise.<Integer>promise(p -> p.onSuccess(ref::set).onSuccessDo(latch::countDown));

        promise.success(1);
        latch.await();

        assertEquals(1, ref.get());
    }
}