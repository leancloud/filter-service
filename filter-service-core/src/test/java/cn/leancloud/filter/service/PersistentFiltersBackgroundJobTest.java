package cn.leancloud.filter.service;

import cn.leancloud.filter.service.Configuration.TriggerPersistenceCriteria;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SuppressWarnings("unchecked")
public class PersistentFiltersBackgroundJobTest {
    private final List<FilterRecord<BloomFilter>> records = TestingUtils.generateFilterRecords(10);

    private MeterRegistry registry;
    private Timer timer;
    private PersistentFiltersBackgroundJob job;
    private ScheduledExecutorService service;
    private PersistentManager<BloomFilter> persistentManager;
    private LongAdder filterUpdateTimesCounter;

    @Before
    public void setUp() {
        registry = mock(MeterRegistry.class);
        timer = mock(Timer.class);
        persistentManager = mock(PersistentManager.class);
        when(registry.timer(anyString())).thenReturn(timer);
        filterUpdateTimesCounter = new LongAdder();

        service = mock(ScheduledExecutorService.class);
    }

    @Test
    public void testStartAndStop() {
        final List<TriggerPersistenceCriteria> criteria = new ArrayList<>();
        criteria.add(new TriggerPersistenceCriteria(Duration.ofSeconds(1), 10));
        criteria.add(new TriggerPersistenceCriteria(Duration.ofSeconds(2), 20));
        criteria.add(new TriggerPersistenceCriteria(Duration.ofSeconds(3), 30));

        final List<ScheduledFuture> futures = new ArrayList<>();
        when(service.scheduleWithFixedDelay(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    ScheduledFuture future = mock(ScheduledFuture.class);
                    futures.add(future);
                    return future;
                });

        job = new PersistentFiltersBackgroundJob(registry,
                records,
                persistentManager,
                filterUpdateTimesCounter,
                criteria);


        job.start(service);
        job.stop();

        assertThat(futures.size()).isEqualTo(criteria.size());
        for (ScheduledFuture f : futures) {
            verify(f, times(1)).cancel(false);
        }
    }

    @Test
    public void testCriteriaNotMeet() throws IOException {
        final List<TriggerPersistenceCriteria> criteria = Collections.singletonList(
                new TriggerPersistenceCriteria(Duration.ofSeconds(1), 10));
        when(service.scheduleWithFixedDelay(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                });

        job = new PersistentFiltersBackgroundJob(registry,
                records,
                persistentManager,
                filterUpdateTimesCounter,
                criteria);

        filterUpdateTimesCounter.reset();
        job.start(service);
        verify(persistentManager, never()).freezeAllFilters(records);
    }

    @Test
    public void testPersistentWhenCriteriaMeet() throws IOException {
        final List<TriggerPersistenceCriteria> criteria = Collections.singletonList(
                new TriggerPersistenceCriteria(Duration.ofSeconds(1), 10));
        when(service.scheduleWithFixedDelay(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                });

        job = new PersistentFiltersBackgroundJob(registry,
                records,
                persistentManager,
                filterUpdateTimesCounter,
                criteria);

        filterUpdateTimesCounter.add(100);
        job.start(service);
        verify(persistentManager, times(1)).freezeAllFilters(records);
    }
}