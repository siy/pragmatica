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

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.io.CoreError;

/**
 * Tuples of various size (0-9).
 */
public interface Tuple {
    int size();

    interface Tuple0 extends Tuple {
        <T> T map(Fn0<T> mapper);

        <T> Result<T> lift(ThrowingFn0<T> mapper);

        default int size() {
            return 0;
        }
    }

    interface Tuple1<T1> extends Tuple {
        <T> T map(Fn1<T, T1> mapper);

        <T> Result<T> lift(ThrowingFn1<T, T1> mapper);

        default int size() {
            return 1;
        }
    }

    interface Tuple2<T1, T2> extends Tuple {
        <T> T map(Fn2<T, T1, T2> mapper);

        <T> Result<T> lift(ThrowingFn2<T, T1, T2> mapper);

        default int size() {
            return 2;
        }

        T1 first();

        T2 last();
    }

    interface Tuple3<T1, T2, T3> extends Tuple {
        <T> T map(Fn3<T, T1, T2, T3> mapper);

        <T> Result<T> lift(ThrowingFn3<T, T1, T2, T3> mapper);

        default int size() {
            return 3;
        }
    }

    interface Tuple4<T1, T2, T3, T4> extends Tuple {
        <T> T map(Fn4<T, T1, T2, T3, T4> mapper);

        <T> Result<T> lift(ThrowingFn4<T, T1, T2, T3, T4> mapper);

        default int size() {
            return 4;
        }
    }

    interface Tuple5<T1, T2, T3, T4, T5> extends Tuple {
        <T> T map(Fn5<T, T1, T2, T3, T4, T5> mapper);

        <T> Result<T> lift(ThrowingFn5<T, T1, T2, T3, T4, T5> mapper);

        default int size() {
            return 5;
        }
    }

    interface Tuple6<T1, T2, T3, T4, T5, T6> extends Tuple {
        <T> T map(Fn6<T, T1, T2, T3, T4, T5, T6> mapper);

        <T> Result<T> lift(ThrowingFn6<T, T1, T2, T3, T4, T5, T6> mapper);

        default int size() {
            return 6;
        }
    }

    interface Tuple7<T1, T2, T3, T4, T5, T6, T7> extends Tuple {
        <T> T map(Fn7<T, T1, T2, T3, T4, T5, T6, T7> mapper);

        <T> Result<T> lift(ThrowingFn7<T, T1, T2, T3, T4, T5, T6, T7> mapper);

        default int size() {
            return 7;
        }
    }

    interface Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> extends Tuple {
        <T> T map(Fn8<T, T1, T2, T3, T4, T5, T6, T7, T8> mapper);

        <T> Result<T> lift(ThrowingFn8<T, T1, T2, T3, T4, T5, T6, T7, T8> mapper);

        default int size() {
            return 8;
        }
    }

    interface Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Tuple {
        <T> T map(Fn9<T, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper);

        <T> Result<T> lift(ThrowingFn9<T, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper);

        default int size() {
            return 9;
        }
    }

    static <T1> Tuple1<T1> tuple(T1 param1) {
        record tuple1<T1>(T1 param1) implements Tuple1<T1> {
            @Override
            public <T> T map(Fn1<T, T1> mapper) {
                return mapper.apply(param1());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn1<T, T1> mapper) {
                try {
                    return Result.success(mapper.apply(param1()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple1<>(param1);
    }

    static <T1, T2> Tuple2<T1, T2> tuple(T1 param1, T2 param2) {
        record tuple2<T1, T2>(T1 param1, T2 param2) implements Tuple2<T1, T2> {
            @Override
            public <T> T map(Fn2<T, T1, T2> mapper) {
                return mapper.apply(param1(), param2());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn2<T, T1, T2> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }

            @Override
            public T1 first() {
                return param1();
            }

            @Override
            public T2 last() {
                return param2();
            }
        }

        return new tuple2<>(param1, param2);
    }

    static <T1, T2, T3> Tuple3<T1, T2, T3> tuple(T1 param1, T2 param2, T3 param3) {
        record tuple3<T1, T2, T3>(T1 param1, T2 param2, T3 param3) implements Tuple3<T1, T2, T3> {
            @Override
            public <T> T map(Fn3<T, T1, T2, T3> mapper) {
                return mapper.apply(param1(), param2(), param3());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn3<T, T1, T2, T3> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2(), param3()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple3<>(param1, param2, param3);
    }

    static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple(T1 param1, T2 param2, T3 param3, T4 param4) {
        record tuple4<T1, T2, T3, T4>(T1 param1, T2 param2, T3 param3, T4 param4) implements Tuple4<T1, T2, T3, T4> {
            @Override
            public <T> T map(Fn4<T, T1, T2, T3, T4> mapper) {
                return mapper.apply(param1(), param2(), param3(), param4());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn4<T, T1, T2, T3, T4> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2(), param3(), param4()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple4<>(param1, param2, param3, param4);
    }

    static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> tuple(
        T1 param1, T2 param2, T3 param3, T4 param4, T5 param5
    ) {
        record tuple5<T1, T2, T3, T4, T5>(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5) implements Tuple5<T1, T2, T3, T4, T5> {
            @Override
            public <T> T map(Fn5<T, T1, T2, T3, T4, T5> mapper) {
                return mapper.apply(param1(), param2(), param3(), param4(), param5());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn5<T, T1, T2, T3, T4, T5> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2(), param3(), param4(), param5()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple5<>(param1, param2, param3, param4, param5);
    }

    static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6
    ) {
        record tuple6<T1, T2, T3, T4, T5, T6>(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6)
            implements Tuple6<T1, T2, T3, T4, T5, T6> {
            @Override
            public <T> T map(Fn6<T, T1, T2, T3, T4, T5, T6> mapper) {
                return mapper.apply(param1(), param2(), param3(), param4(), param5(), param6());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn6<T, T1, T2, T3, T4, T5, T6> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2(), param3(), param4(), param5(), param6()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple6<>(param1, param2, param3, param4, param5, param6);
    }

    static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6, T7 param7
    ) {
        record tuple7<T1, T2, T3, T4, T5, T6, T7>(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7)
            implements Tuple7<T1, T2, T3, T4, T5, T6, T7> {
            @Override
            public <T> T map(Fn7<T, T1, T2, T3, T4, T5, T6, T7> mapper) {
                return mapper.apply(param1(), param2(), param3(), param4(), param5(), param6(), param7());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn7<T, T1, T2, T3, T4, T5, T6, T7> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2(), param3(), param4(), param5(), param6(), param7()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple7<>(param1, param2, param3, param4, param5, param6, param7);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6,
        T7 param7, T8 param8
    ) {
        record tuple8<T1, T2, T3, T4, T5, T6, T7, T8>(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8)
            implements Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Override
            public <T> T map(Fn8<T, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
                return mapper.apply(param1(), param2(), param3(), param4(), param5(), param6(), param7(), param8());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn8<T, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2(), param3(), param4(), param5(), param6(), param7(), param8()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple8<>(param1, param2, param3, param4, param5, param6, param7, param8);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6,
        T7 param7, T8 param8, T9 param9
    ) {
        record tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(T1 param1, T2 param2, T3 param3, T4 param4, T5 param5, T6 param6, T7 param7, T8 param8,
                                                          T9 param9)
            implements Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
            @Override
            public <T> T map(Fn9<T, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
                return mapper.apply(param1(), param2(), param3(), param4(), param5(), param6(), param7(), param8(), param9());
            }

            @Override
            public <T> Result<T> lift(ThrowingFn9<T, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
                try {
                    return Result.success(mapper.apply(param1(), param2(), param3(), param4(), param5(), param6(), param7(), param8(), param9()));
                } catch (Throwable throwable) {
                    return new CoreError.Exception(throwable).result();
                }
            }
        }

        return new tuple9<>(param1, param2, param3, param4, param5, param6, param7, param8, param9);
    }
}
