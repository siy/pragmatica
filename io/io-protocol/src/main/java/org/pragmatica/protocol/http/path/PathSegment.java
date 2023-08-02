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

package org.pragmatica.protocol.http.path;

import org.pragmatica.lang.Tuple.*;
import org.pragmatica.lang.Tuple.Tuple1;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple9;

public sealed interface PathSegment<T> {
    non-sealed interface Segment0 extends PathSegment<Tuple0> {}
    non-sealed interface Segment1<T1> extends PathSegment<Tuple1<T1>> {}
    non-sealed interface Segment2<T1, T2> extends PathSegment<Tuple2<T1, T2>> {}
    non-sealed interface Segment9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends PathSegment<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {}
}
