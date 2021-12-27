/*
 * Copyright (c) 2020 Sergiy Yevtushenko
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

import java.util.function.Supplier;

/**
 * From https://en.wikipedia.org/wiki/Unit_type :
 * <blockquote>
 * In the area of mathematical logic and computer science known as type theory, a unit type is a type that allows only one value (and thus can hold no
 * information). The carrier (underlying set) associated with a unit type can be any singleton set. There is an isomorphism between any two such sets,
 * so it is customary to talk about the unit type and ignore the details of its value. One may also regard the unit type as the type of 0-tuples, i.e.
 * the product of no types.
 * <p>
 * The unit type is the terminal object in the category of types and typed functions. It should not be confused with the zero or bottom type, which
 * allows no values and is the initial object in this category. Similarly, the Boolean is the type with two values.
 * <p>
 * The unit type is implemented in most functional programming languages. The void type that is used in some imperative programming languages serves
 * some of its functions, but because its carrier set is empty, it has some limitations.
 * </blockquote>
 */
public final class Unit implements Tuple.Tuple0 {
    private Unit() {}

    private static final Unit UNIT = new Unit();
    private static final Result<Unit> UNIT_RESULT = Result.success(UNIT);

    public static Unit unit() {
        return UNIT;
    }

    public static <T> Unit unit(final T ignored) {
        return UNIT;
    }

    public static Result<Unit> unitResult() {
        return UNIT_RESULT;
    }

    @Override
    public String toString() {
        return "()";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Tuple0;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public <T> T map(Supplier<T> mapper) {
        return mapper.get();
    }
}
