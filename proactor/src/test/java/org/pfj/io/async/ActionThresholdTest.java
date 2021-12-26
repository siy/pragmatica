/*
 * Copyright (c) 2020 Sergiy Yevtushenko
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

package org.pfj.io.async;

import org.junit.jupiter.api.Test;
import org.pfj.io.async.util.ActionableThreshold;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ActionThresholdTest {
    @Test
    void actionIsTriggeredOnceThresholdIsReached() {
        final AtomicBoolean marker = new AtomicBoolean();
        final ActionableThreshold threshold = ActionableThreshold.threshold(3, () -> marker.compareAndSet(false, true));

        assertFalse(marker.get());

        threshold.registerEvent();
        assertFalse(marker.get());

        threshold.registerEvent();
        assertFalse(marker.get());

        threshold.registerEvent();
        assertTrue(marker.get());
    }

    @Test
    void subsequentEventsDontTriggerAction() {
        final AtomicInteger counter = new AtomicInteger(0);
        final ActionableThreshold threshold = ActionableThreshold.threshold(3, counter::incrementAndGet);

        assertEquals(0, counter.get());

        threshold.registerEvent();
        assertEquals(0, counter.get());

        threshold.registerEvent();
        assertEquals(0, counter.get());

        threshold.registerEvent();
        assertEquals(1, counter.get());

        threshold.registerEvent();
        assertEquals(1, counter.get());
    }
}