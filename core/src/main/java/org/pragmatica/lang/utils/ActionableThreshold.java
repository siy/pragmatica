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

package org.pragmatica.lang.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Helper class used to track number of events and trigger action once threshold is reached. The action is triggered only once, when number of events
 * exactly matches configured threshold.
 */
public record ActionableThreshold(AtomicInteger counter, Runnable action) {
    /**
     * Create an instance configured for threshold and action.
     *
     * @param count  Number of events to register
     * @param action Action to perform
     *
     * @return Created instance
     */
    public static ActionableThreshold threshold(int count, Runnable action) {
        return new ActionableThreshold(new AtomicInteger(count), action);
    }

    /**
     * Perform operation on current instance.
     *
     * @return Current instance
     */
    public ActionableThreshold apply(Consumer<ActionableThreshold> setup) {
        setup.accept(this);
        return this;
    }

    /**
     * Register event and perform action if threshold is reached. Once threshold is reached no further events will trigger action execution.
     */
    public void registerEvent() {
        if (counter.get() <= 0) {
            return;
        }

        if (counter.decrementAndGet() == 0) {
            action.run();
        }
    }
}
