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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Disabled
public class ResultPerformanceTest {
    private static final int NUM_ITERATIONS = 100_000;
    private static final int MAX_LEN = 255;
    private static final Cause TEST_CAUSE = Causes.cause("Error");

    private static final List<String> TEST_DATA_0 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Result<String>> TEST_OPTION_DATA_0 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_10 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Result<String>> TEST_OPTION_DATA_10 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_25 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Result<String>> TEST_OPTION_DATA_25 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_50 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Result<String>> TEST_OPTION_DATA_50 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_90 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Result<String>> TEST_OPTION_DATA_90 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_75 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Result<String>> TEST_OPTION_DATA_75 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_100 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Result<String>> TEST_OPTION_DATA_100 = new ArrayList<>(NUM_ITERATIONS);

    static  {
        var random = new SecureRandom();

        for (var i = 0; i < NUM_ITERATIONS; i++) {
            var string = generateRandomString(random);
            var nextInt = random.nextInt(1000);

            TEST_DATA_0.add(string);
            TEST_OPTION_DATA_0.add(Result.success(string));

            addValues(100, nextInt, string, TEST_DATA_10, TEST_OPTION_DATA_10);
            addValues(250, nextInt, string, TEST_DATA_25, TEST_OPTION_DATA_25);
            addValues(500, nextInt, string, TEST_DATA_50, TEST_OPTION_DATA_50);
            addValues(750, nextInt, string, TEST_DATA_75, TEST_OPTION_DATA_75);
            addValues(900, nextInt, string, TEST_DATA_90, TEST_OPTION_DATA_90);

            TEST_DATA_100.add(null);
            TEST_OPTION_DATA_100.add(Result.failure(TEST_CAUSE));
        }
    }

    private static void addValues(int rate, int nextInt, String string, List<String> testData, List<Result<String>> testResultData) {
        if (nextInt < rate) {
            testData.add(null);
            testResultData.add(Result.failure(TEST_CAUSE));
        } else {
            testData.add(string);
            testResultData.add(Result.success(string));
        }
    }

    private static String generateRandomString(SecureRandom random) {
        var builder = new StringBuilder();

        for (int j = 0; j < MAX_LEN; j++) {
            char c = (char) ((random.nextBoolean() ? 'A' : 'a') + random.nextInt('Z' - 'A'));

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

    private Result<String> testWithResultNested(Result<String> input) {
        return input.map(String::toUpperCase);
    }

    private Result<String> testWithResult(Result<String> input) {
        return testWithResultNested(input);
    }

    private String testWithExceptionNested(String exceptionString) {
        return exceptionString.toUpperCase();
    }

    private String testWithException(String exceptionString) {
        try {
            return testWithExceptionNested(exceptionString);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void exception0() {
        TEST_DATA_0.forEach(this::testWithException);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void result0() {
        TEST_OPTION_DATA_0.forEach(this::testWithResult);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void exception10() {
        TEST_DATA_10.forEach(this::testWithException);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void result10() {
        TEST_OPTION_DATA_10.forEach(this::testWithResult);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void exception25() {
        TEST_DATA_25.forEach(this::testWithException);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void result25() {
        TEST_OPTION_DATA_25.forEach(this::testWithResult);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void exception50() {
        TEST_DATA_50.forEach(this::testWithException);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void result50() {
        TEST_OPTION_DATA_50.forEach(this::testWithResult);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void exception75() {
        TEST_DATA_75.forEach(this::testWithException);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void result75() {
        TEST_OPTION_DATA_75.forEach(this::testWithResult);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void exception90() {
        TEST_DATA_90.forEach(this::testWithException);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void result90() {
        TEST_OPTION_DATA_90.forEach(this::testWithResult);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void exception100() {
        TEST_DATA_100.forEach(this::testWithException);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void result100() {
        TEST_OPTION_DATA_100.forEach(this::testWithResult);
    }
}
