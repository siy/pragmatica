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

package org.pragmatica.io.util;

import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.Timeout;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PeriodicTaskRunnerTest {
    @Test
    void testPeriodicRun() throws Exception {
        var counter = new CountDownLatch(10);
        var timestamp = new AtomicLong(System.nanoTime());
        var delays = new ArrayList<Long>();

        var ptr = PeriodicTaskRunner.periodicTaskRunner(Timeout.timeout(100).millis(), () -> {
            var time = System.nanoTime();
            var diff = time - timestamp.get();
            timestamp.set(time);
            delays.add(diff);
            counter.countDown();
        });

        ptr.start();
        counter.await();
        ptr.stop();

        var min = delays.stream().mapToLong(Long::longValue).min().orElseThrow()/1000_000L;
        var max = delays.stream().mapToLong(Long::longValue).max().orElseThrow()/1000_000L;
        var average = (long) delays.stream().mapToLong(Long::longValue).average().orElseThrow()/1000_000L;

        System.out.println("Min: " + min + "ms, Max: " + max + "ms, Avg: " + average + "ms");

        assertTrue(average > 90 && average < 110, "Expected average time must be between 90ms and 110ms");
    }
}