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

package org.pragmatica.io.pipeline;

import org.pragmatica.lang.Functions.FN1;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Stage<T> {
    Stage<T> push(T value);

    Stage<T> onValue(Consumer<T> action);

    Stage<T> filter(Predicate<T> predicate);

    <R> Stage<R> map(FN1<R, ? super T> mapper);

    <R> Stage<R> mapIf(Predicate<T> condition, FN1<R, ? super T> trueMapper, FN1<R, ? super T> falseMapper);

    static <T> Stage<T> stage() {
        return new StageImpl<>();
    }
}
