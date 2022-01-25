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

package org.pragmatica.lang;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Tag("Benchmark")
public class OptionPerformanceTest {
    private static final int MAX_LEN = 255;
    private static final Random RANDOM = new SecureRandom();

    private static String generateRandomString() {
        var builder = new StringBuilder();

        for (int j = 0; j < MAX_LEN; j++) {
            char c = (char) ((RANDOM.nextBoolean() ? 'A' : 'a') + RANDOM.nextInt('Z' - 'A'));

            builder.append(c);
        }

        return builder.toString();
    }

    @Test
    void runBenchmarks() throws RunnerException {
        System.err.println("Test data ready, running benchmark");

        var options = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .warmupTime(TimeValue.seconds(1))
            .warmupIterations(5)
            .threads(1)
            .measurementIterations(6)
            .measurementTime(TimeValue.seconds(5))
            .forks(1)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

        new Runner(options).run();
    }

    public String testWithNullable(String nullableString) {
        return nullableString == null ? null : nullableString.toUpperCase();
    }

    public Option<String> testWithOption(Option<String> input) {
        return input.map(String::toUpperCase);
    }

    public void testWithNullableRate(int rate) {
        testWithNullable(RANDOM.nextInt(1000) < rate ? null : generateRandomString());
    }

    public void testWithOptionRate(int rate) {
        testWithOption(RANDOM.nextInt(1000) < rate ? Option.empty() : Option.present(generateRandomString()));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nullable0() {
        testWithNullableRate(0);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void option0() {
        testWithOptionRate(0);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nullable10() {
        testWithNullableRate(100);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void option10() {
        testWithOptionRate(100);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nullable25() {
        testWithNullableRate(250);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void option25() {
        testWithOptionRate(250);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nullable50() {
        testWithNullableRate(500);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void option50() {
        testWithOptionRate(500);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nullable75() {
        testWithNullableRate(750);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void option75() {
        testWithOptionRate(750);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nullable90() {
        testWithNullableRate(900);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void option90() {
        testWithOptionRate(900);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nullable100() {
        testWithNullableRate(1000);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void option100() {
        testWithOptionRate(1000);
    }
}
