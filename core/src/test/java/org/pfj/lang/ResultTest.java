package org.pfj.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ResultTest {
	@Test
	void successResultsAreEqualIfValueEqual() {
		assertEquals(Result.ok("123"), Result.ok(123).map(Objects::toString));
		assertNotEquals(Result.ok("321"), Result.ok(123).map(Objects::toString));
	}

	@Test
	void failureResultsAreEqualIfFailureIsEqual() {
		assertEquals(Result.fail("123"), Result.ok(123).filter(v -> v < 0, "{0}"));
		assertNotEquals(Result.fail("321"), Result.ok(123).filter(v -> v < 0, "{0}"));
	}

	@Test
	void successResultCanBeTransformedWithMap() {
		Result.ok(123).map(Objects::toString)
			.onFailureDo(Assertions::fail)
			.onSuccess(value -> assertEquals("123", value));
	}

	@Test
	void successResultCanBeTransformedWithFlatMap() {
		Result.ok(123).flatMap(v -> Result.ok(v.toString()))
			.onFailureDo(Assertions::fail)
			.onSuccess(value -> assertEquals("123", value));
	}

	@Test
	void failureResultRemainsUnchangedAfterMap() {
		Result.<Integer>fail("Some error").map(Objects::toString)
			.onFailure(failure -> assertEquals("Some error", failure.message()))
			.onSuccessDo(Assertions::fail);
	}

	@Test
	void failureResultRemainsUnchangedAfterFlatMap() {
		Result.<Integer>fail("Some error").flatMap(v -> Result.ok(v.toString()))
			.onFailure(failure -> assertEquals("Some error", failure.message()))
			.onSuccessDo(Assertions::fail);
	}

	@Test
	void onlyOneMethodIsInvokedOnApply() {
		Result.ok(321).apply(
			failure -> fail(failure.message()),
			Functions::blackHole
		);

		Result.fail("Some error").apply(
			Functions::blackHole,
			value -> fail(value.toString())
		);
	}

	@Test
	void onSuccessIsInvokedForSuccessResult() {
		Result.ok(123)
			.onFailureDo(Assertions::fail)
			.onSuccess(value -> assertEquals(123, value));
		Result.<Integer>fail("123")
			.onFailure(failure -> assertEquals("123", failure.message()))
			.onSuccess(value -> fail(value.toString()));
	}

	@Test
	void onSuccessDoIsInvokedForSuccessResult() {
		var flag1 = new AtomicBoolean(false);

		Result.ok(123)
			.onFailureDo(Assertions::fail)
			.onSuccessDo(() -> flag1.set(true));

		assertTrue(flag1.get());

		var flag2 = new AtomicBoolean(false);

		Result.<Integer>fail("123")
			.onFailureDo(() -> flag2.set(true))
			.onSuccessDo(Assertions::fail);

		assertTrue(flag2.get());
	}

	@Test
	void onFailureIsInvokedForFailure() {
		Result.ok(123)
			.onFailure(failure -> Assertions.fail(failure.message()))
			.onSuccess(value -> assertEquals(123, value));
		Result.<Integer>fail("123")
			.onFailure(failure -> assertEquals("123", failure.message()))
			.onSuccess(value -> fail(value.toString()));
	}

	@Test
	void onFailureDoIsInvokedForFailureResult() {
		var flag1 = new AtomicBoolean(false);

		Result.ok(123)
			.onFailureDo(Assertions::fail)
			.onSuccessDo(() -> flag1.set(true));

		assertTrue(flag1.get());

		var flag2 = new AtomicBoolean(false);

		Result.<Integer>fail("123")
			.onFailureDo(() -> flag2.set(true))
			.onSuccessDo(Assertions::fail);

		assertTrue(flag2.get());
	}

	@Test
	void resultCanBeConvertedToOption() {
		Result.ok(123).toOption()
			.whenPresent(value -> assertEquals(123, value))
			.whenEmpty(Assertions::fail);

		var flag1 = new AtomicBoolean(false);

		Result.<Integer>fail("123").toOption()
			.whenPresent(__ -> Assertions.fail("Should not happen"))
			.whenEmpty(() -> flag1.set(true));

		assertTrue(flag1.get());
	}

	@Test
	void resultStatusCanBeChecked() {
		assertTrue(Result.ok(321).isSuccess());
		assertFalse(Result.ok(321).isFailure());
		assertFalse(Result.fail("321").isSuccess());
		assertTrue(Result.fail("321").isFailure());
	}

	@Test
	void successResultCanBeFiltered() {
		Result.ok(231)
			.onSuccess(value -> assertEquals(231, value))
			.onFailureDo(Assertions::fail)
			.filter(value -> value > 321, "Value {0} is below threshold")
			.onSuccessDo(Assertions::fail)
			.onFailure(failure -> assertEquals("Value 231 is below threshold", failure.message()));
	}
}