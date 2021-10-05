package org.pfj.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.pfj.lang.Causes.cause;
import static org.pfj.lang.Causes.with1;
import static org.pfj.lang.Result.success;

class ResultTest {
    @Test
    void successResultsAreEqualIfValueEqual() {
        assertEquals(success("123"), success(123).map(Objects::toString));
        assertNotEquals(success("321"), success(123).map(Objects::toString));
    }

    @Test
    void failureResultsAreEqualIfFailureIsEqual() {
        assertEquals(Result.failure(cause("123")), success(123).filter(with1("{0}"), v -> v < 0));
        assertNotEquals(Result.failure(cause("321")), success(123).filter(with1("{0}"), v -> v < 0));
    }

    @Test
    void successResultCanBeTransformedWithMap() {
        success(123).map(Objects::toString)
                .onFailureDo(Assertions::fail)
                .onSuccess(value -> assertEquals("123", value));
    }

    @Test
    void successResultCanBeTransformedWithFlatMap() {
        success(123).flatMap(v -> success(v.toString()))
                .onFailureDo(Assertions::fail)
                .onSuccess(value -> assertEquals("123", value));
    }

    @Test
    void failureResultRemainsUnchangedAfterMap() {
        Result.<Integer>failure(cause("Some error")).map(Objects::toString)
                .onFailure(cause -> assertEquals("Some error", cause.message()))
                .onSuccessDo(Assertions::fail);
    }

    @Test
    void failureResultRemainsUnchangedAfterFlatMap() {
        Result.<Integer>failure(cause("Some error")).flatMap(v -> success(v.toString()))
                .onFailure(cause -> assertEquals("Some error", cause.message()))
                .onSuccessDo(Assertions::fail);
    }

    @Test
    void onlyOneMethodIsInvokedOnApply() {
        success(321).apply(
                failure -> fail(failure.message()),
                Functions::blackHole
        );

        Result.failure(cause("Some error")).apply(
                Functions::blackHole,
                value -> fail(value.toString())
        );
    }

    @Test
    void onSuccessIsInvokedForSuccessResult() {
        success(123)
                .onFailureDo(Assertions::fail)
                .onSuccess(value -> assertEquals(123, value));
        Result.<Integer>failure(cause("123"))
                .onFailure(cause -> assertEquals("123", cause.message()))
                .onSuccess(value -> fail(value.toString()));
    }

    @Test
    void onSuccessDoIsInvokedForSuccessResult() {
        var flag1 = new AtomicBoolean(false);

        success(123)
                .onFailureDo(Assertions::fail)
                .onSuccessDo(() -> flag1.set(true));

        assertTrue(flag1.get());

        var flag2 = new AtomicBoolean(false);

        Result.<Integer>failure(cause("123"))
                .onFailureDo(() -> flag2.set(true))
                .onSuccessDo(Assertions::fail);

        assertTrue(flag2.get());
    }

    @Test
    void onFailureIsInvokedForFailure() {
        success(123)
                .onFailure(cause -> Assertions.fail(cause.message()))
                .onSuccess(value -> assertEquals(123, value));
        Result.<Integer>failure(cause("123"))
                .onFailure(cause -> assertEquals("123", cause.message()))
                .onSuccess(value -> fail(value.toString()));
    }

    @Test
    void onFailureDoIsInvokedForFailureResult() {
        var flag1 = new AtomicBoolean(false);

        success(123)
                .onFailureDo(Assertions::fail)
                .onSuccessDo(() -> flag1.set(true));

        assertTrue(flag1.get());

        var flag2 = new AtomicBoolean(false);

        Result.<Integer>failure(cause("123"))
                .onFailureDo(() -> flag2.set(true))
                .onSuccessDo(Assertions::fail);

        assertTrue(flag2.get());
    }

    @Test
    void resultCanBeConvertedToOption() {
        success(123).toOption()
                .whenPresent(value -> assertEquals(123, value))
                .whenEmpty(Assertions::fail);

        var flag1 = new AtomicBoolean(false);

        Result.<Integer>failure(cause("123")).toOption()
                .whenPresent(__ -> Assertions.fail("Should not happen"))
                .whenEmpty(() -> flag1.set(true));

        assertTrue(flag1.get());
    }

    @Test
    void resultStatusCanBeChecked() {
        assertTrue(success(321).isSuccess());
        assertFalse(success(321).isFailure());
        assertFalse(Result.failure(cause("321")).isSuccess());
        assertTrue(Result.failure(cause("321")).isFailure());
    }

    @Test
    void successResultCanBeFiltered() {
        success(231)
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
                cause -> { throw new IllegalStateException(cause.message()); },
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
