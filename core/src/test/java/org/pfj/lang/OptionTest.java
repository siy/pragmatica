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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pfj.lang.Option.all;
import static org.pfj.lang.Option.any;
import static org.pfj.lang.Option.empty;
import static org.pfj.lang.Option.option;
import static org.pfj.lang.Option.present;

class OptionTest {
	@Test
	void emptyOptionsAreEqual() {
		assertEquals(empty(), empty());
		assertEquals("Option()", empty().toString());
	}

	@Test
	void presentOptionsAreEqualIfContentEqual() {
		assertEquals(present(123), present(123));
		assertNotEquals(present(321), present(123));
		assertNotEquals(empty(), present(1));
		assertNotEquals(present(1), empty());
		assertEquals("Option(1)", present(1).toString());
		assertEquals(32, present(1).hashCode());
	}

	@Test
	void presentOptionCanBeTransformed() {
		present(123L)
			.whenPresent(value -> assertEquals(123L, value))
			.whenEmpty(Assertions::fail)
			.map(Object::toString)
			.whenPresent(value -> assertEquals("123", value))
			.whenEmpty(Assertions::fail);
	}

	@Test
	void emptyOptionRemainsEmptyAfterTransformation() {
		empty()
			.whenPresent(v -> fail())
			.map(Object::toString)
			.whenPresent(v -> fail());
	}

	@Test
	void presentOptionCanBeFlatMapped() {
		present(123L)
			.whenPresent(value -> assertEquals(123L, value))
			.whenEmpty(Assertions::fail)
			.flatMap(value -> present(value.toString()))
			.whenPresent(value -> assertEquals("123", value))
			.whenEmpty(Assertions::fail);
	}

	@Test
	void emptyOptionRemainsEmptyAfterFlatMap() {
		empty()
			.whenPresent(v -> fail())
			.flatMap(value -> present(value.toString()))
			.whenPresent(v -> fail());
	}

	@Test
	void presentOptionCanBeFilteredToPresentOption() {
		present(123L)
			.whenPresent(value -> assertEquals(123L, value))
			.whenEmpty(Assertions::fail)
			.filter(value -> value > 120L)
			.whenPresent(value -> assertEquals(123L, value))
			.whenEmpty(Assertions::fail);

	}

	@Test
	void presentOptionCanBeFilteredToEmptyOption() {
		present(123L)
			.whenPresent(value -> assertEquals(123L, value))
			.whenEmpty(Assertions::fail)
			.filter(value -> value < 120L)
			.whenPresent(value -> fail());
	}

	@Test
	void whenOptionPresentThenPresentSideEffectIsTriggered() {
		var flag = new AtomicBoolean(false);

		present(123L)
			.whenPresent(value -> flag.set(true));

		assertTrue(flag.get());
	}

	@Test
	void whenOptionEmptyThenPresentSideEffectIsNotTriggered() {
		var flag = new AtomicBoolean(false);

		empty()
			.whenPresent(value -> flag.set(true));

		assertFalse(flag.get());
	}

	@Test
	void whenOptionEmptyThenEmptySideEffectIsTriggered() {
		var flag = new AtomicBoolean(false);

		empty()
			.whenEmpty(() -> flag.set(true));

		assertTrue(flag.get());
	}

	@Test
	void whenOptionPresentThenEmptySideEffectIsNotTriggered() {
		var flag = new AtomicBoolean(false);

		present(123L)
			.whenEmpty(() -> flag.set(true));

		assertFalse(flag.get());
	}

	@Test
	void presentSideEffectIsInvokedForPresentOption() {
		var flagPresent = new AtomicLong(0L);
		var flagEmpty = new AtomicBoolean(false);

		present(123L)
			.apply(() -> flagEmpty.set(true), flagPresent::set);

		assertEquals(123L, flagPresent.get());
		assertFalse(flagEmpty.get());
	}

	@Test
	void emptySideEffectIsInvokedForEmptyOption() {
		var flagPresent = new AtomicLong(0L);
		var flagEmpty = new AtomicBoolean(false);

		Option.<Long>empty()
			.apply(() -> flagEmpty.set(true), flagPresent::set);

		assertEquals(0L, flagPresent.get());
		assertTrue(flagEmpty.get());
	}

	@Test
	void valueCanBeObtainedFromOption() {
		assertEquals(321L, present(321L).otherwise(123L));
		assertEquals(123L, empty().otherwise(123L));
	}

	@Test
	void valueCanBeLazilyObtainedFromOption() {
		var flag = new AtomicBoolean(false);
		assertEquals(321L, present(321L).otherwiseGet(() -> {
			flag.set(true);
			return 123L;
		}));
		assertFalse(flag.get());

		assertEquals(123L, empty().otherwiseGet(() -> {
			flag.set(true);
			return 123L;
		}));
		assertTrue(flag.get());
	}

	@Test
	void presentOptionCanBeStreamed() {
		assertEquals(1L, present(1).stream().collect(Collectors.summarizingInt(Integer::intValue)).getSum());
	}

	@Test
	void emptyOptionCanBeStreamedToEmptyStream() {
		assertEquals(0L, empty().stream().count());
	}

	@Test
	void presentOptionCanBeConvertedToSuccessResultInDifferentWays() {
		option(1).toResult("Not expected")
			.onSuccess(value -> assertEquals(1, value))
			.onFailureDo(Assertions::fail);

		option(1).toResult("Not expected {}", " parameter ")
			.onSuccess(value -> assertEquals(1, value))
			.onFailureDo(Assertions::fail);

		option(1).toResult(Failure.failure("Not expected"))
			.onSuccess(value -> assertEquals(1, value))
			.onFailureDo(Assertions::fail);
	}

	@Test
	void emptyOptionCanBeConvertedToFailureResult() {
		option(null).toResult("Expected")
			.onSuccess(value -> fail("Should not be a success"))
			.onFailure(failure -> assertEquals("Expected", failure.message()));
	}

	@Test
	void optionCanBeConvertedToOptional() {
		assertEquals(Optional.of(321), present(321).toOptional());
		assertEquals(Optional.empty(), empty().toOptional());
	}

	@Test
	void optionalCanBeConvertedToOption() {
		assertEquals(option(123), Option.from(Optional.of(123)));
		assertEquals(empty(), Option.from(Optional.empty()));
	}

	@Test
	void orReturnsReplacementIfOptionIsEmpty() {
		present(1).or(present(2))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(1, value));

		empty().or(present(2))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(2, value));
	}

	@Test
	void orLazilyEvaluatesReplacement() {
		var flag = new AtomicBoolean(false);

		present(1).or(
			() -> {
				flag.set(true);
				return present(2);
			})
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(1, value));

		assertFalse(flag.get());

		empty().or(
			() -> {
				flag.set(true);
				return present(2);
			})
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(2, value));

		assertTrue(flag.get());
	}

	@Test
	void anyFindsFirstNonEmptyOption() {
		any(empty(), present(2))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(2, value));

		any(empty(), empty(), present(3))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(3, value));

		any(empty(), empty(), empty(), present(4))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(4, value));

		any(empty(), empty(), empty(), empty(), present(5))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(5, value));

		any(empty(), empty(), empty(), empty(), empty(), present(6))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(6, value));

		any(empty(), empty(), empty(), empty(), empty(), empty(), present(7))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(7, value));

		any(empty(), empty(), empty(), empty(), empty(), empty(), empty(), present(8))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(8, value));

		any(empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), present(9))
			.whenEmpty(Assertions::fail)
			.whenPresent(value -> assertEquals(9, value));
	}

	@Test
	void allIsPresentIfAllInputsArePresent() {
		all(present(1))
			.map(v1 -> v1)
			.whenPresent(value -> assertEquals(1, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1))
			.map((v1, v2) -> v1 + v2)
			.whenPresent(value -> assertEquals(2, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1))
			.map((v1, v2, v3) -> v1 + v2 + v3)
			.whenPresent(value -> assertEquals(3, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1))
			.map((v1, v2, v3, v4) -> v1 + v2 + v3 + v4)
			.whenPresent(value -> assertEquals(4, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1))
			.map((v1, v2, v3, v4, v5) -> v1 + v2 + v3 + v4 + v5)
			.whenPresent(value -> assertEquals(5, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1))
			.map((v1, v2, v3, v4, v5, v6) -> v1 + v2 + v3 + v4 + v5 + v6)
			.whenPresent(value -> assertEquals(6, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1))
			.map((v1, v2, v3, v4, v5, v6, v7) -> v1 + v2 + v3 + v4 + v5 + v6 + v7)
			.whenPresent(value -> assertEquals(7, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1))
			.map((v1, v2, v3, v4, v5, v6, v7, v8) -> v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8)
			.whenPresent(value -> assertEquals(8, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1))
			.map((v1, v2, v3, v4, v5, v6, v7, v8, v9) -> v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9)
			.whenPresent(value -> assertEquals(9, value))
			.whenEmpty(Assertions::fail);
	}

	@Test
	void allCanBeFlatMappedIfAllInputsArePresent() {
		all(present(1))
			.flatMap(v1 -> present(v1))
			.whenPresent(value -> assertEquals(1, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1))
			.flatMap((v1, v2) -> present(v1 + v2))
			.whenPresent(value -> assertEquals(2, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1))
			.flatMap((v1, v2, v3) -> present(v1 + v2 + v3))
			.whenPresent(value -> assertEquals(3, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1))
			.flatMap((v1, v2, v3, v4) -> present(v1 + v2 + v3 + v4))
			.whenPresent(value -> assertEquals(4, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1))
			.flatMap((v1, v2, v3, v4, v5) -> present(v1 + v2 + v3 + v4 + v5))
			.whenPresent(value -> assertEquals(5, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1))
			.flatMap((v1, v2, v3, v4, v5, v6) -> present(v1 + v2 + v3 + v4 + v5 + v6))
			.whenPresent(value -> assertEquals(6, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1))
			.flatMap((v1, v2, v3, v4, v5, v6, v7) -> present(v1 + v2 + v3 + v4 + v5 + v6 + v7))
			.whenPresent(value -> assertEquals(7, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1))
			.flatMap((v1, v2, v3, v4, v5, v6, v7, v8) -> present(v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8))
			.whenPresent(value -> assertEquals(8, value))
			.whenEmpty(Assertions::fail);

		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1))
			.flatMap((v1, v2, v3, v4, v5, v6, v7, v8, v9) -> present(v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9))
			.whenPresent(value -> assertEquals(9, value))
			.whenEmpty(Assertions::fail);
	}

	@Test
	void allIsMissingIfAnyInputIsMissing1() {
		all(empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing2() {
		all(empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing3() {
		all(empty(), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing4() {
		all(empty(), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty(), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing5() {
		all(empty(), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty(), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), empty(), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing6() {
		all(empty(), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty(), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), empty(), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), empty(), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing7() {
		all(empty(), present(1), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty(), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), empty(), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), empty(), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), empty(), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), present(1), empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing8() {
		all(empty(), present(1), present(1), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty(), present(1), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), empty(), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), empty(), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), empty(), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), empty(), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), present(1), empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1), empty()).id().whenPresent(__ -> fail());
	}

	@Test
	void allIsMissingIfAnyInputIsMissing9() {
		all(empty(), present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), empty(), present(1), present(1), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), empty(), present(1), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), empty(), present(1), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), empty(), present(1), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), empty(), present(1), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), present(1), empty(), present(1), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1), empty(), present(1)).id().whenPresent(__ -> fail());
		all(present(1), present(1), present(1), present(1), present(1), present(1), present(1), present(1), empty()).id().whenPresent(__ -> fail());
	}
}