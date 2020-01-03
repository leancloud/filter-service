package cn.leancloud.filter.service;

import com.linecorp.armeria.common.metric.NoopMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class BackgroundJobSchedulerTest {
    private static final String jobName = "TestingJobName";
    private ScheduledExecutorService scheduledExecutorService;
    private MeterRegistry registry;
    private BackgroundJobScheduler scheduler;

    @Before
    public void setUp() throws Exception {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        registry = NoopMeterRegistry.get();
        scheduler = new BackgroundJobScheduler(registry, scheduledExecutorService);
    }

    @After
    public void tearDown() throws Exception {
        scheduler.stop();
        scheduledExecutorService.shutdown();
    }

    @Test
    public void testStopScheduler() throws Exception {
        final CountDownLatch executed = new CountDownLatch(1);
        final AtomicInteger executedTimes = new AtomicInteger();
        final AtomicBoolean interrupt = new AtomicBoolean(false);
        scheduler.scheduleFixedIntervalJob(() -> {
            try {
                executed.countDown();
                Thread.sleep(1000);
                executedTimes.incrementAndGet();
            } catch (InterruptedException ex) {
                interrupt.set(true);
            }
        }, jobName, Duration.ofMillis(10));

        executed.await();
        scheduler.stop();
        assertThat(executedTimes).hasValue(1);
        assertThat(interrupt).isFalse();
    }
}