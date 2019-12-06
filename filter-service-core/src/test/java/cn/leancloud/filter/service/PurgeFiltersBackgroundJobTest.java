package cn.leancloud.filter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class PurgeFiltersBackgroundJobTest {
    private final Duration purgeFilterInterval = Duration.ofSeconds(10);
    private MeterRegistry registry;
    private Purgatory purgatory;
    private PurgeFiltersBackgroundJob job;
    private ScheduledExecutorService service;
    private Timer timer;

    @Before
    public void setUp() {
        registry = mock(MeterRegistry.class);
        purgatory = mock(Purgatory.class);
        timer = mock(Timer.class);
        when(registry.timer(anyString())).thenReturn(timer);
        job = new PurgeFiltersBackgroundJob(registry, purgatory, purgeFilterInterval);
        service = mock(ScheduledExecutorService.class);
    }

    @Test
    public void testStartPurge() {
        doNothing().when(purgatory).purge();
        when(timer.wrap(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(service.scheduleWithFixedDelay(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    Runnable job = invocation.getArgument(0);
                    job.run();
                    return null;
                });

        job.start(service);

        verify(purgatory, times(1)).purge();
    }

    @Test
    public void testStartThenStop() {
        final ScheduledFuture future = mock(ScheduledFuture.class);
        when(timer.wrap(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(service.scheduleWithFixedDelay(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)))
                .thenAnswer(invocation -> future);

        job.start(service);
        job.stop();

        verify(future, times(1)).cancel(false);
    }
}