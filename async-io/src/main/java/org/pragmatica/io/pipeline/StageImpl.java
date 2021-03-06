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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class StageImpl<T> implements Stage<T> {
    private final List<Consumer<T>> listeners = new ArrayList<>();

    @Override
    public Stage<T> push(T value) {
        listeners.forEach(consumer -> consumer.accept(value));

        return this;
    }

    @Override
    public Stage<T> onValue(Consumer<T> action) {
        listeners.add(action);

        return this;
    }

    @Override
    public Stage<T> filter(Predicate<T> predicate) {
        var newStage = new StageImpl<T>();

        listeners.add(value -> {
            if (predicate.test(value)) {
                newStage.push(value);
            }
        });

        return newStage;
    }

    @Override
    public <R> Stage<R> map(FN1<R, ? super T> mapper) {
        var newStage = new StageImpl<R>();

        onValue(value -> newStage.push(mapper.apply(value)));

        return newStage;
    }

    @Override
    public <R> Stage<R> mapIf(Predicate<T> condition, FN1<R, ? super T> trueMapper, FN1<R, ? super T> falseMapper) {
        var newStage = new StageImpl<R>();

        onValue(value -> newStage.push(condition.test(value) ? trueMapper.apply(value) : falseMapper.apply(value)));

        return newStage;
    }
}
