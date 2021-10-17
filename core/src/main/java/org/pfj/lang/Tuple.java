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

import org.pfj.lang.Functions.*;

import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Objects.hash;

/**
 * Tuples of various size (0-9).
 */
public interface Tuple {
    int size();

    interface Tuple0 extends Tuple {
        <T> T map(Supplier<T> mapper);

        default int size() {
            return 0;
        }
    }

    interface Tuple1<T1> extends Tuple {
        <T> T map(FN1<T, T1> mapper);

        default int size() {
            return 1;
        }
    }

    interface Tuple2<T1, T2> extends Tuple {
        <T> T map(FN2<T, T1, T2> mapper);

        default int size() {
            return 2;
        }

        T1 first();

        T2 last();
    }

    interface Tuple3<T1, T2, T3> extends Tuple {
        <T> T map(FN3<T, T1, T2, T3> mapper);

        default int size() {
            return 3;
        }
    }

    interface Tuple4<T1, T2, T3, T4> extends Tuple {
        <T> T map(FN4<T, T1, T2, T3, T4> mapper);

        default int size() {
            return 4;
        }
    }

    interface Tuple5<T1, T2, T3, T4, T5> extends Tuple {
        <T> T map(FN5<T, T1, T2, T3, T4, T5> mapper);

        default int size() {
            return 5;
        }
    }

    interface Tuple6<T1, T2, T3, T4, T5, T6> extends Tuple {
        <T> T map(FN6<T, T1, T2, T3, T4, T5, T6> mapper);

        default int size() {
            return 6;
        }
    }

    interface Tuple7<T1, T2, T3, T4, T5, T6, T7> extends Tuple {
        <T> T map(FN7<T, T1, T2, T3, T4, T5, T6, T7> mapper);

        default int size() {
            return 7;
        }
    }

    interface Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> extends Tuple {
        <T> T map(FN8<T, T1, T2, T3, T4, T5, T6, T7, T8> mapper);

        default int size() {
            return 8;
        }
    }

    interface Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Tuple {
        <T> T map(FN9<T, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper);

        default int size() {
            return 9;
        }
    }

    Tuple0 UNIT = new Tuple0() {
        @Override
        public <T> T map(Supplier<T> mapper) {
            return mapper.get();
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
        public String toString() {
            return "Tuple0()";
        }
    };

    static Tuple0 tuple() {
        return UNIT;
    }

    static Tuple0 unit() {
        return UNIT;
    }

    static <T1> Tuple1<T1> tuple(T1 param1) {
        return new Tuple1<>() {
            @Override
            public <T> T map(FN1<T, T1> mapper) {
                return mapper.apply(param1);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple1<?> tuple1)
                    && tuple1.map(v1 -> Objects.equals(v1, param1));
            }

            @Override
            public int hashCode() {
                return hash(param1);
            }

            @Override
            public String toString() {
                return "Tuple("
                    + param1.toString()
                    + ")";
            }
        };
    }

    static <T1, T2> Tuple2<T1, T2> tuple(T1 param1, T2 param2) {
        return new Tuple2<>() {
            @Override
            public <T> T map(FN2<T, T1, T2> mapper) {
                return mapper.apply(param1, param2);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple2<?, ?> tuple2)
                    && tuple2.map((v1, v2) -> Objects.equals(v1, param1) && Objects.equals(v2, param2));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2);
            }

            @Override
            public String toString() {
                return "Tuple(" + param1.toString() + ", " + param2.toString() + ")";
            }

            @Override
            public T1 first() {
                return param1;
            }

            @Override
            public T2 last() {
                return param2;
            }
        };
    }

    static <T1, T2, T3> Tuple3<T1, T2, T3> tuple(T1 param1, T2 param2, T3 param3) {
        return new Tuple3<>() {
            @Override
            public <T> T map(FN3<T, T1, T2, T3> mapper) {
                return mapper.apply(param1, param2, param3);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple3<?, ?, ?> tuple3)
                    && tuple3.map((v1, v2, v3) ->
                    Objects.equals(v1, param1)
                        && Objects.equals(v2, param2)
                        && Objects.equals(v3, param3));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2, param3);
            }

            @Override
            public String toString() {
                return "Tuple(" + param1.toString() + ", " + param2.toString() + ", " + param3.toString() + ")";
            }
        };
    }

    static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple(T1 param1, T2 param2, T3 param3, T4 param4) {
        return new Tuple4<>() {
            @Override
            public <T> T map(FN4<T, T1, T2, T3, T4> mapper) {
                return mapper.apply(param1, param2, param3, param4);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple4<?, ?, ?, ?> tuple4)
                    && tuple4.map((v1, v2, v3, v4) ->
                    Objects.equals(v1, param1)
                        && Objects.equals(v2, param2)
                        && Objects.equals(v3, param3)
                        && Objects.equals(v4, param4));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2, param3, param4);
            }

            @Override
            public String toString() {
                return "Tuple("
                    + param1.toString() + ", " + param2.toString() + ", " + param3.toString() + ", "
                    + param4.toString() + ")";
            }
        };
    }

    static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> tuple(
        T1 param1, T2 param2, T3 param3, T4 param4, T5 param5
    ) {
        return new Tuple5<>() {
            @Override
            public <T> T map(FN5<T, T1, T2, T3, T4, T5> mapper) {
                return mapper.apply(param1, param2, param3, param4, param5);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple5<?, ?, ?, ?, ?> tuple5)
                    && tuple5.map((v1, v2, v3, v4, v5) ->
                    Objects.equals(v1, param1)
                        && Objects.equals(v2, param2)
                        && Objects.equals(v3, param3)
                        && Objects.equals(v4, param4)
                        && Objects.equals(v5, param5));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2, param3, param4, param5);
            }

            @Override
            public String toString() {
                return "Tuple("
                    + param1.toString() + ", " + param2.toString() + ", " + param3.toString() + ", "
                    + param4.toString() + ", " + param5.toString() + ")";
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6
    ) {
        return new Tuple6<>() {
            @Override
            public <T> T map(FN6<T, T1, T2, T3, T4, T5, T6> mapper) {
                return mapper.apply(param1, param2, param3, param4, param5, param6);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple6<?, ?, ?, ?, ?, ?> tuple6)
                    && tuple6.map((v1, v2, v3, v4, v5, v6) ->
                    Objects.equals(v1, param1)
                        && Objects.equals(v2, param2)
                        && Objects.equals(v3, param3)
                        && Objects.equals(v4, param4)
                        && Objects.equals(v5, param5)
                        && Objects.equals(v6, param6));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2, param3, param4, param5, param6);
            }

            @Override
            public String toString() {
                return "Tuple("
                    + param1.toString() + ", " + param2.toString() + ", " + param3.toString() + ", "
                    + param4.toString() + ", " + param5.toString() + ", " + param6.toString() + ")";
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6,
        T7 param7
    ) {
        return new Tuple7<>() {
            @Override
            public <T> T map(FN7<T, T1, T2, T3, T4, T5, T6, T7> mapper) {
                return mapper.apply(param1, param2, param3, param4, param5, param6, param7);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple7<?, ?, ?, ?, ?, ?, ?> tuple7)
                    && tuple7.map((v1, v2, v3, v4, v5, v6, v7) ->
                    Objects.equals(v1, param1)
                        && Objects.equals(v2, param2)
                        && Objects.equals(v3, param3)
                        && Objects.equals(v4, param4)
                        && Objects.equals(v5, param5)
                        && Objects.equals(v6, param6)
                        && Objects.equals(v7, param7));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2, param3, param4, param5, param6, param7);
            }

            @Override
            public String toString() {
                return "Tuple("
                    + param1.toString() + ", " + param2.toString() + ", " + param3.toString() + ", "
                    + param4.toString() + ", " + param5.toString() + ", " + param6.toString() + ", "
                    + param7.toString() + ")";
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6,
        T7 param7, T8 param8
    ) {
        return new Tuple8<>() {
            @Override
            public <T> T map(FN8<T, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
                return mapper.apply(param1, param2, param3, param4, param5, param6, param7, param8);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple8<?, ?, ?, ?, ?, ?, ?, ?> tuple8)
                    && tuple8.map((v1, v2, v3, v4, v5, v6, v7, v8) ->
                    Objects.equals(v1, param1)
                        && Objects.equals(v2, param2)
                        && Objects.equals(v3, param3)
                        && Objects.equals(v4, param4)
                        && Objects.equals(v5, param5)
                        && Objects.equals(v6, param6)
                        && Objects.equals(v7, param7)
                        && Objects.equals(v8, param8));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2, param3, param4, param5, param6, param7, param8);
            }

            @Override
            public String toString() {
                return "Tuple("
                    + param1.toString() + ", " + param2.toString() + ", " + param3.toString() + ", "
                    + param4.toString() + ", " + param5.toString() + ", " + param6.toString() + ", "
                    + param7.toString() + ", " + param8.toString() + ")";
            }
        };
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> tuple(
        T1 param1, T2 param2, T3 param3,
        T4 param4, T5 param5, T6 param6,
        T7 param7, T8 param8, T9 param9
    ) {
        return new Tuple9<>() {
            @Override
            public <T> T map(FN9<T, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
                return mapper.apply(param1, param2, param3, param4, param5, param6, param7, param8, param9);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                return (obj instanceof Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> tuple9)
                    && tuple9.map((v1, v2, v3, v4, v5, v6, v7, v8, v9) ->
                    Objects.equals(v1, param1)
                        && Objects.equals(v2, param2)
                        && Objects.equals(v3, param3)
                        && Objects.equals(v4, param4)
                        && Objects.equals(v5, param5)
                        && Objects.equals(v6, param6)
                        && Objects.equals(v7, param7)
                        && Objects.equals(v8, param8)
                        && Objects.equals(v9, param9));
            }

            @Override
            public int hashCode() {
                return hash(param1, param2, param3, param4, param5, param6, param7, param8, param9);
            }

            @Override
            public String toString() {
                return "Tuple("
                    + param1.toString() + ", " + param2.toString() + ", " + param3.toString() + ", "
                    + param4.toString() + ", " + param5.toString() + ", " + param6.toString() + ", "
                    + param7.toString() + ", " + param8.toString() + ", " + param9.toString() + ")";
            }
        };
    }
}
