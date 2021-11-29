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
public class OptionPerformanceTest {
    private static final int NUM_ITERATIONS = 100_000;
    private static final int MAX_LEN = 255;

    private static final List<String> TEST_DATA_0 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Option<String>> TEST_OPTION_DATA_0 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_10 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Option<String>> TEST_OPTION_DATA_10 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_25 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Option<String>> TEST_OPTION_DATA_25 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_50 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Option<String>> TEST_OPTION_DATA_50 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_90 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Option<String>> TEST_OPTION_DATA_90 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_75 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Option<String>> TEST_OPTION_DATA_75 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<String> TEST_DATA_100 = new ArrayList<>(NUM_ITERATIONS);
    private static final List<Option<String>> TEST_OPTION_DATA_100 = new ArrayList<>(NUM_ITERATIONS);

    static  {
        var random = new SecureRandom();

        for (var i = 0; i < NUM_ITERATIONS; i++) {
            var string = generateRandomString(random);
            var nextInt = random.nextInt(1000);

            TEST_DATA_0.add(string);
            TEST_OPTION_DATA_0.add(Option.present(string));

            addValues(100, nextInt, string, TEST_DATA_10, TEST_OPTION_DATA_10);
            addValues(250, nextInt, string, TEST_DATA_25, TEST_OPTION_DATA_25);
            addValues(500, nextInt, string, TEST_DATA_50, TEST_OPTION_DATA_50);
            addValues(750, nextInt, string, TEST_DATA_75, TEST_OPTION_DATA_75);
            addValues(900, nextInt, string, TEST_DATA_90, TEST_OPTION_DATA_90);

            TEST_DATA_100.add(null);
            TEST_OPTION_DATA_100.add(Option.empty());
        }
    }

    private static void addValues(int rate, int nextInt, String string, List<String> testData, List<Option<String>> testOptionData) {
        if (nextInt < rate) {
            testData.add(null);
            testOptionData.add(Option.empty());
        } else {
            testData.add(string);
            testOptionData.add(Option.present(string));
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

    private Option<String> testWithOption(Option<String> input) {
        return input.map(String::toUpperCase);
    }

    private String testWithNullable(String nullableString) {
        return nullableString == null ? null : nullableString.toUpperCase();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void nullable0() {
        TEST_DATA_0.forEach(this::testWithNullable);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void option0() {
        TEST_OPTION_DATA_0.forEach(this::testWithOption);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void nullable10() {
        TEST_DATA_10.forEach(this::testWithNullable);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void option10() {
        TEST_OPTION_DATA_10.forEach(this::testWithOption);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void nullable25() {
        TEST_DATA_25.forEach(this::testWithNullable);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void option25() {
        TEST_OPTION_DATA_25.forEach(this::testWithOption);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void nullable50() {
        TEST_DATA_50.forEach(this::testWithNullable);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void option50() {
        TEST_OPTION_DATA_50.forEach(this::testWithOption);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void nullable75() {
        TEST_DATA_75.forEach(this::testWithNullable);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void option75() {
        TEST_OPTION_DATA_75.forEach(this::testWithOption);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void nullable90() {
        TEST_DATA_90.forEach(this::testWithNullable);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void option90() {
        TEST_OPTION_DATA_90.forEach(this::testWithOption);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void nullable100() {
        TEST_DATA_100.forEach(this::testWithNullable);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void option100() {
        TEST_OPTION_DATA_100.forEach(this::testWithOption);
    }
}
