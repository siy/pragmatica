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
    interface Fn0<R> {
        R apply();
    }

    /**
     * Function with one parameter.
     */
    @FunctionalInterface
    interface Fn1<R, T1> {
        R apply(T1 param1);

        default <N> Fn1<N, T1> then(Fn1<N, R> function) {
            return v1 -> function.apply(apply(v1));
        }

        default <N> Fn1<R, N> before(Fn1<T1, N> function) {
            return v1 -> apply(function.apply(v1));
        }

        static <T> Fn1<T, T> id() {
            return Functions::id;
        }
    }

    /**
     * Function with two parameters.
     */
    @FunctionalInterface
    interface Fn2<R, T1, T2> {
        R apply(T1 param1, T2 param2);
    }

    /**
     * Function with three parameters.
     */
    @FunctionalInterface
    interface Fn3<R, T1, T2, T3> {
        R apply(T1 param1, T2 param2, T3 param3);
    }

    /**
     * Function with four parameters.
     */
    @FunctionalInterface
    interface Fn4<R, T1, T2, T3, T4> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4);
    }

    /**
     * Function with five parameters.
     */
    @FunctionalInterface
    interface Fn5<R, T1, T2, T3, T4, T5> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5);
    }

    /**
     * Function with six parameters.
     */
    @FunctionalInterface
    interface Fn6<R, T1, T2, T3, T4, T5, T6> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6);
    }

    /**
     * Function with seven parameters.
     */
    @FunctionalInterface
    interface Fn7<R, T1, T2, T3, T4, T5, T6, T7> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7);
    }

    /**
     * Function with eight parameters.
     */
    @FunctionalInterface
    interface Fn8<R, T1, T2, T3, T4, T5, T6, T7, T8> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8);
    }

    /**
     * Function with nine parameters.
     */
    @FunctionalInterface
    interface Fn9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8, T9 param9);
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
    interface ThrowingFn0<T> {
        T apply() throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn1<R, T1> {
        R apply(T1 v1) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn2<R, T1, T2> {
        R apply(T1 v1, T2 v2) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn3<R, T1, T2, T3> {
        R apply(T1 param1, T2 param2, T3 param3) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn4<R, T1, T2, T3, T4> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn5<R, T1, T2, T3, T4, T5> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn6<R, T1, T2, T3, T4, T5, T6> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn7<R, T1, T2, T3, T4, T5, T6, T7> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn8<R, T1, T2, T3, T4, T5, T6, T7, T8> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowingFn9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        R apply(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8, T9 param9) throws Throwable;
    }

    /**
     * Function with variable argument list.
     */
    @FunctionalInterface
    interface FnX<R> {
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

    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> void unitFn(T1 param1,
                                                            T2 param2,
                                                            T3 param3,
                                                            T4 param4,
                                                            T5 param5,
                                                            T6 param6,
                                                            T7 param7,
                                                            T8 param8,
                                                            T9 param9) {
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
