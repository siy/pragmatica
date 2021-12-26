package org.pfj.lang;

import org.junit.jupiter.api.Disabled;
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

@Disabled
public class ResultPerformanceTest {
    private static final int MAX_LEN = 255;
    private static final Random RANDOM = new SecureRandom();
    private static final Cause TEST_CAUSE = Causes.cause("Error");

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

    public String testWithException(String nullableString) {
        return nullableString.toUpperCase();
    }

    public Result<String> testWithResult(Result<String> input) {
        return input.map(String::toUpperCase);
    }

    public String testWithExceptionRate(int rate) {
        return testWithException(RANDOM.nextInt(1000) < rate ? null : generateRandomString());
    }

    public Result<String> testWithResultRate(int rate) {
        return testWithResult(RANDOM.nextInt(1000) < rate
                              ? Result.failure(TEST_CAUSE)
                              : Result.success(generateRandomString()));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String exception0() {
        try {
            return testWithExceptionRate(0);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Result<String> result0() {
        return testWithResultRate(0);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String exception10() {
        try {
            return testWithExceptionRate(100);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Result<String> result10() {
        return testWithResultRate(100);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String exception25() {
        try {
            return testWithExceptionRate(250);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Result<String> result25() {
        return testWithResultRate(250);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String exception50() {
        try {
            return testWithExceptionRate(500);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Result<String> result50() {
        return testWithResultRate(500);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String exception75() {
        try {
            return testWithExceptionRate(750);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Result<String> result75() {
        return testWithResultRate(750);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String exception90() {
        try {
            return testWithExceptionRate(900);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Result<String> result90() {
        return testWithResultRate(900);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String exception100() {
        try {
            return testWithExceptionRate(1000);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Result<String> result100() {
        return testWithResultRate(1000);
    }
}
