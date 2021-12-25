/*
 * Copyright (c) 2019, 2020 Sergiy Yevtushenko
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

package org.pfj.io.async.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Helper class used to track number of events along with results and trigger action once threshold is reached.
 * The action is triggered only once, when number of events exactly matches configured threshold.
 */
public class ResultCollector {
    private final Object[] results;
    private final AtomicInteger counter;
    private final Consumer<Object[]> action;

    private ResultCollector(final int count, final Consumer<Object[]> action) {
        this.counter = new AtomicInteger(count);
        this.action = action;
        this.results = new Object[count];
    }

    public ResultCollector apply(final Consumer<ResultCollector> setup) {
        setup.accept(this);
        return this;
    }

    /**
     * Create an instance configured for threshold and action.
     *
     * @param count
     *        Number of events to register
     * @param action
     *        Action to perform
     *
     * @return Created instance
     */
    public static ResultCollector resultCollector(final int count, final Consumer<Object[]> action) {
        assert count >= 0;
        return new ResultCollector(count, action);
    }

    /**
     * Create an instance configured for threshold and action.
     *
     * @param count
     *        Number of events to register
     * @param setup
     *        Consumer to invoke once instance of {@link ResultCollector} is created
     * @param action
     *        Action to perform
     *
     * @return Created instance
     */
    public static ResultCollector resultCollector(final int count, final Consumer<ResultCollector> setup, final Consumer<Object[]> action) {
        assert count >= 0;
        return resultCollector(count, action).apply(setup);
    }

    /**
     * Register event and perform action if threshold is reached. Once threshold is reached
     * no further events will trigger action execution.
     */
    public void registerEvent(int index, Object value) {
        if (counter.get() <= 0) {
            return;
        }

        assert results[index] == null;
        results[index] = value;

        if (counter.decrementAndGet() == 0) {
            action.accept(results);
        }
    }
}
