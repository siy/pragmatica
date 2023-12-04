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

package org.pragmatica.io.utils;

import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.Timeout;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodicTaskRunnerTest {
    @Test
    void testPeriodicRunWithFixedRate() throws Exception {
        var counter = new CountDownLatch(30);
        var timestamp = new AtomicLong();
        var delays = new ArrayList<Long>();

        var ptr = PeriodicTaskRunner.withFixedRate(Timeout.timeout(100).millis())
                                    .run(() -> {
                                        var time = System.nanoTime();
                                        var diff = time - timestamp.get();
                                        timestamp.set(time);
                                        delays.add(diff);
                                        counter.countDown();
                                    });

        timestamp.set(System.nanoTime());
        ptr.start();
        counter.await();
        ptr.close();

        var min = delays.stream().mapToLong(Long::longValue).min().orElseThrow() / 1000_000L;
        var max = delays.stream().mapToLong(Long::longValue).max().orElseThrow() / 1000_000L;
        var average = (long) delays.stream().mapToLong(Long::longValue).average().orElseThrow() / 1000_000L;

        System.err.println("Fixed rate Min: " + min + "ms, Max: " + max + "ms, Avg: " + average + "ms");

        assertTrue(average > 80 && average <= 120, "Expected average time must be between 80ms and 120ms");
    }

    @Test
    void testPeriodicRunWithFixedDelay() throws Exception {
        var counter = new CountDownLatch(30);
        var timestamp = new AtomicLong();
        var delays = new ArrayList<Long>();

        var ptr = PeriodicTaskRunner.withFixedDelay(Timeout.timeout(100).millis())
                                    .run(() -> {
                                        var time = System.nanoTime();
                                        var diff = time - timestamp.get();
                                        timestamp.set(time);
                                        delays.add(diff);
                                        counter.countDown();
                                    });

        timestamp.set(System.nanoTime());
        ptr.start();
        counter.await();
        ptr.close();

        var min = delays.stream().mapToLong(Long::longValue).min().orElseThrow() / 1000_000L;
        var max = delays.stream().mapToLong(Long::longValue).max().orElseThrow() / 1000_000L;
        var average = (long) delays.stream().mapToLong(Long::longValue).average().orElseThrow() / 1000_000L;

        System.err.println("Min: " + min + "ms, Max: " + max + "ms, Avg: " + average + "ms");

        assertTrue(average > 80 && average <= 120, "Expected average time must be between 80ms and 120ms");
    }
}