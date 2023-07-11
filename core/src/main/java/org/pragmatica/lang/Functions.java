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

/**
 * Collection of basic functions for various use cases.
 */
public interface Functions {
    /**
     * Function with no parameters (supplier). Provided for consistency.
     */
    @FunctionalInterface
    interface FN0<R> {
        R apply();
    }

    /**
     * Function with one parameter.
     */
    @FunctionalInterface
    interface FN1<R, T1> {
        R apply(T1 param1);

        default <N> FN1<N, T1> then(FN1<N, R> function) {
            return v1 -> function.apply(apply(v1));
        }

        default <N> FN1<R, N> before(FN1<T1, N> function) {
            return v1 -> apply(function.apply(v1));
        }

        static <T> FN1<T, T> id() {
            return Functions::id;
        }
    }

    /**
     * Function with two parameters.
     */
    @FunctionalInterface
    interface FN2<R, T1, T2> {
        R apply(T1 param1, T2 param2);

        default FN1<R, T2> bind(T1 param) {
            return v2 -> apply(param, v2);
        }

        default <N> FN2<N, T1, T2> then(FN1<N, R> function) {
            return (v1, v2) -> function.apply(apply(v1, v2));
        }
    }

    /**
     * Function with three parameters.
     */
    @FunctionalInterface
    interface FN3<R, T1, T2, T3> {
        R apply(T1 param1, T2 param2, T3 param3);

        default FN2<R, T2, T3> bind(T1 param) {
            return (v2, v3) -> apply(param, v2, v3);
        }

        default <N> FN3<N, T1, T2, T3> then(FN1<N, R> function) {
            return (v1, v2, v3) -> function.apply(apply(v1, v2, v3));
        }
    }

    /**
     * Function with four parameters.
     */
    @FunctionalInterface
    interface FN4<R, T1, T2, T3, T4> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4);

        default FN3<R, T2, T3, T4> bind(T1 param) {
            return (v2, v3, v4) -> apply(param, v2, v3, v4);
        }

        default <N> FN4<N, T1, T2, T3, T4> then(FN1<N, R> function) {
            return (v1, v2, v3, v4) -> function.apply(apply(v1, v2, v3, v4));
        }
    }

    /**
     * Function with five parameters.
     */
    @FunctionalInterface
    interface FN5<R, T1, T2, T3, T4, T5> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5);

        default FN4<R, T2, T3, T4, T5> bind(T1 param) {
            return (v2, v3, v4, v5) -> apply(param, v2, v3, v4, v5);
        }

        default <N> FN5<N, T1, T2, T3, T4, T5> then(FN1<N, R> function) {
            return (v1, v2, v3, v4, v5) -> function.apply(apply(v1, v2, v3, v4, v5));
        }
    }

    /**
     * Function with six parameters.
     */
    @FunctionalInterface
    interface FN6<R, T1, T2, T3, T4, T5, T6> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6);

        default FN5<R, T2, T3, T4, T5, T6> bind(T1 param) {
            return (v2, v3, v4, v5, v6) -> apply(param, v2, v3, v4, v5, v6);
        }

        default <N> FN6<N, T1, T2, T3, T4, T5, T6> then(FN1<N, R> function) {
            return (v1, v2, v3, v4, v5, v6) -> function.apply(apply(v1, v2, v3, v4, v5, v6));
        }
    }

    /**
     * Function with seven parameters.
     */
    @FunctionalInterface
    interface FN7<R, T1, T2, T3, T4, T5, T6, T7> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7);

        default FN6<R, T2, T3, T4, T5, T6, T7> bind(T1 param) {
            return (v2, v3, v4, v5, v6, v7) -> apply(param, v2, v3, v4, v5, v6, v7);
        }

        default <N> FN7<N, T1, T2, T3, T4, T5, T6, T7> then(FN1<N, R> function) {
            return (v1, v2, v3, v4, v5, v6, v7) -> function.apply(apply(v1, v2, v3, v4, v5, v6, v7));
        }
    }

    /**
     * Function with eight parameters.
     */
    @FunctionalInterface
    interface FN8<R, T1, T2, T3, T4, T5, T6, T7, T8> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8);

        default FN7<R, T2, T3, T4, T5, T6, T7, T8> bind(T1 param) {
            return (v2, v3, v4, v5, v6, v7, v8) -> apply(param, v2, v3, v4, v5, v6, v7, v8);
        }

        default <N> FN8<N, T1, T2, T3, T4, T5, T6, T7, T8> then(FN1<N, R> function) {
            return (v1, v2, v3, v4, v5, v6, v7, v8) -> function.apply(apply(v1, v2, v3, v4, v5, v6, v7, v8));
        }
    }

    /**
     * Function with nine parameters.
     */
    @FunctionalInterface
    interface FN9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8, T9 param9);

        default FN8<R, T2, T3, T4, T5, T6, T7, T8, T9> bind(T1 param) {
            return (v2, v3, v4, v5, v6, v7, v8, v9) -> apply(param, v2, v3, v4, v5, v6, v7, v8, v9);
        }

        default <N> FN9<N, T1, T2, T3, T4, T5, T6, T7, T8, T9> then(FN1<N, R> function) {
            return (v1, v2, v3, v4, v5, v6, v7, v8, v9) -> function.apply(apply(v1, v2, v3, v4, v5, v6, v7, v8, v9));
        }
    }

    /**
     * Universal identity function.
     */
    static <T> T id(T value) {
        return value;
    }

    /**
     * Supplier which can throw an exception.
     */
    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

    /**
     * Consumer with three parameters.
     */
    @FunctionalInterface
    interface TriConsumer<T, K, V> {
        void accept(T t, K k, V v);
    }

    /**
     * Function with variable argument list.
     */
    @FunctionalInterface
    interface FNx<R> {
        R apply(Object... values);
    }

    /**
     * Universal consumers of values which do nothing with input values. Useful for cases when API requires function, but there is no need to do
     * anything with the received values.
     */
    static <T1> void unitFn() {
    }

    static <T1> void unitFn(T1 value) {
    }

    static <T1, T2> void unitFn(T1 param1, T2 param2) {
    }

    static <T1, T2, T3> void unitFn(T1 param1, T2 param2, T3 param3) {
    }

    static <T1, T2, T3, T4> void unitFn(T1 param1, T2 param2, T3 param3, T4 param4) {
    }

    static <T1, T2, T3, T4, T5> void unitFn(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5) {
    }

    static <T1, T2, T3, T4, T5, T6> void unitFn(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6) {
    }

    static <T1, T2, T3, T4, T5, T6, T7> void unitFn(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7) {
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> void unitFn(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8) {
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> void unitFn(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8, T9 param9) {
    }

    static <R, T1> R toNull(T1 value) {
        return null;
    }

    static <T1> boolean toTrue(T1 value) {
        return true;
    }

    static <T1> boolean toFalse(T1 value) {
        return false;
    }
}
