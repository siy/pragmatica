/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
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
import org.pragmatica.lang.Result.Failure;
import org.pragmatica.lang.Result.Success;
import org.pragmatica.lang.utils.Causes;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    @Test
    void successResultsAreEqualIfValueEqual() {
        assertEquals(Result.success("123"), Result.success(123).map(Objects::toString));
        assertNotEquals(Result.success("321"), Result.success(123).map(Objects::toString));
    }

    @Test
    void failureResultsAreEqualIfFailureIsEqual() {
        assertEquals(Result.failure(Causes.cause("123")), Result.success(123).filter(Causes.with1("{0}"), v -> v < 0));
        assertNotEquals(Result.failure(Causes.cause("321")), Result.success(123).filter(Causes.with1("{0}"), v -> v < 0));
    }

    @Test
    void patterMatchingIsSupportedForSuccess() {
        var resultValue = Result.success(123);

        switch (resultValue) {
            case Success success -> assertEquals(123, success.value());
            case Failure failure -> Assertions.fail("Unexpected failure: " + failure.cause());
        }
    }

    @Test
    void patterMatchingIsSupportedForFailure() {
        var resultValue = Result.failure(Causes.cause("123"));

        switch (resultValue) {
            case Success success -> Assertions.fail("Unexpected success: " + success.value());
            case Failure failure -> assertEquals(Causes.cause("123"), failure.cause());
        }
    }

    @Test
    void successResultCanBeTransformedWithMap() {
        Result.success(123).map(Objects::toString)
              .onFailureDo(Assertions::fail)
              .onSuccess(value -> assertEquals("123", value));
    }

    @Test
    void successResultCanBeTransformedWithFlatMap() {
        Result.success(123).flatMap(v -> Result.success(v.toString()))
              .onFailureDo(Assertions::fail)
              .onSuccess(value -> assertEquals("123", value));
    }

    @Test
    void failureResultRemainsUnchangedAfterMap() {
        Result.<Integer>failure(Causes.cause("Some error")).map(Objects::toString)
              .onFailure(cause -> assertEquals("Some error", cause.message()))
              .onSuccessDo(Assertions::fail);
    }

    @Test
    void failureResultRemainsUnchangedAfterFlatMap() {
        Result.<Integer>failure(Causes.cause("Some error")).flatMap(v -> Result.success(v.toString()))
              .onFailure(cause -> assertEquals("Some error", cause.message()))
              .onSuccessDo(Assertions::fail);
    }

    @Test
    void onlyOneMethodIsInvokedOnApply() {
        Result.success(321).apply(
            failure -> fail(failure.message()),
            Functions::unitFn
        );

        Result.failure(Causes.cause("Some error")).apply(
            Functions::unitFn,
            value -> fail(value.toString())
        );
    }

    @Test
    void onSuccessIsInvokedForSuccessResult() {
        Result.success(123)
              .onFailureDo(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));
        Result.<Integer>failure(Causes.cause("123"))
              .onFailure(cause -> assertEquals("123", cause.message()))
              .onSuccess(value -> fail(value.toString()));
    }

    @Test
    void onSuccessDoIsInvokedForSuccessResult() {
        var flag1 = new AtomicBoolean(false);

        Result.success(123)
              .onFailureDo(Assertions::fail)
              .onSuccessDo(() -> flag1.set(true));

        assertTrue(flag1.get());

        var flag2 = new AtomicBoolean(false);

        Result.<Integer>failure(Causes.cause("123"))
              .onFailureDo(() -> flag2.set(true))
              .onSuccessDo(Assertions::fail);

        assertTrue(flag2.get());
    }

    @Test
    void onFailureIsInvokedForFailure() {
        Result.success(123)
              .onFailure(cause -> fail(cause.message()))
              .onSuccess(value -> assertEquals(123, value));
        Result.<Integer>failure(Causes.cause("123"))
              .onFailure(cause -> assertEquals("123", cause.message()))
              .onSuccess(value -> fail(value.toString()));
    }

    @Test
    void onFailureDoIsInvokedForFailureResult() {
        var flag1 = new AtomicBoolean(false);

        Result.success(123)
              .onFailureDo(Assertions::fail)
              .onSuccessDo(() -> flag1.set(true));

        assertTrue(flag1.get());

        var flag2 = new AtomicBoolean(false);

        Result.<Integer>failure(Causes.cause("123"))
              .onFailureDo(() -> flag2.set(true))
              .onSuccessDo(Assertions::fail);

        assertTrue(flag2.get());
    }

    @Test
    void resultCanBeConvertedToOption() {
        Result.success(123).toOption()
              .onPresent(value -> assertEquals(123, value))
              .onEmpty(Assertions::fail);

        var flag1 = new AtomicBoolean(false);

        Result.<Integer>failure(Causes.cause("123")).toOption()
              .onPresent(__ -> fail("Should not happen"))
              .onEmpty(() -> flag1.set(true));

        assertTrue(flag1.get());
    }

    @Test
    void resultStatusCanBeChecked() {
        assertTrue(Result.success(321).isSuccess());
        assertFalse(Result.success(321).isFailure());
        assertFalse(Result.failure(Causes.cause("321")).isSuccess());
        assertTrue(Result.failure(Causes.cause("321")).isFailure());
    }

    @Test
    void successResultCanBeFiltered() {
        Result.success(231)
              .onSuccess(value -> assertEquals(231, value))
              .onFailureDo(Assertions::fail)
              .filter(Causes.with1("Value {0} is below threshold"), value -> value > 321)
              .onSuccessDo(Assertions::fail)
              .onFailure(cause -> assertEquals("Value 231 is below threshold", cause.message()));
    }

    @Test
    void liftWrapsCodeWhichCanThrowExceptions() {
        Result.lift(Causes::fromThrowable, () -> throwingFunction(3))
              .onFailure(cause -> assertTrue(cause.message().startsWith("java.lang.IllegalStateException: Just throw exception 3")))
              .onSuccess(value -> fail("Expecting failure"));

        Result.lift(Causes::fromThrowable, () -> throwingFunction(4))
              .onFailure(cause -> fail(cause.message()))
              .onSuccess(value -> assertEquals("Input:4", value));
    }

    static <T> T unwrap(Result<T> value) {
        return value.fold(
            cause -> {
                throw new IllegalStateException(cause.message());
            },
            content -> content
        );
    }


    static String throwingFunction(int i) {
        if (i == 3) {
            throw new IllegalStateException("Just throw exception " + i);
        }

        return "Input:" + i;
    }
}
