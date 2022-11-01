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

import org.pragmatica.lang.Tuple;
import org.pragmatica.lang.Tuple.*;
import org.pragmatica.lang.Unit;
//import org.pragmatica.protocol.http.path.PathSegment.*;
import org.pragmatica.protocol.http.path.Segment.*;

import java.util.ArrayList;
import java.util.List;

import static org.pragmatica.lang.Tuple.tuple;

public final class SegmentedPathBuilder {
    private SegmentedPathBuilder() {}

    public static Builder0 builder() {
        return new Builder0();
    }

    public final class Builder0 {
        private final List<Segment> segments = new ArrayList<>();

        Builder0 plain(Plain segment) {
            segments.add(segment);
            return this;
        }

        <T1> Builder1<T1> typed(Typed<T1> segment) {

        }

        SegmentedPath<Unit> build() {
            return new SegmentedPath<>(segments);
        }
    }

    public interface Builder1<T1> {
        Builder1<T1> plain(Segment segment);
        <T2> Builder2<T1, T2> typed(Typed<T2> segment);
        SegmentedPath<Tuple1<T1>> build();
    }

    public interface Builder2<T1, T2> {
        Builder2<T1, T2> plain(Segment segment);
        <T3> Builder3<T1, T2, T3> typed(Typed<T3> segment);
        SegmentedPath<Tuple2<T1, T2>> build();
    }

    public interface Builder3<T1, T2, T3> {
        Builder3<T1, T2, T3> plain(Segment segment);
        SegmentedPath<Tuple3<T1, T2, T3>> build();
    }
}
