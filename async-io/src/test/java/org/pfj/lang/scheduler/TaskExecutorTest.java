package org.pfj.lang.scheduler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskExecutorTest {
    @Test
    void taskCanBeExecuted() throws InterruptedException {
        TaskExecutor executor = TaskExecutor.taskExecutor(2);

        var counter = new AtomicInteger(0);
        var latch = new CountDownLatch(1);

        executor.submit(__ -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        latch.await();

        assertEquals(1, counter.get());

        executor.shutdown();
    }
}