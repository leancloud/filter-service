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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SuppressWarnings("unchecked")
public class PersistentFiltersJobTest {
    private PersistentFiltersJob job;
    private BloomFilterManager<BloomFilter, ?> bloomFilterManager;
    private PersistentManager<BloomFilter> persistentManager;
    private LongAdder filterUpdateTimesCounter;
    private TriggerPersistenceCriteria criteria = new TriggerPersistenceCriteria(Duration.ofSeconds(1), 10);

    @Before
    public void setUp() {
        bloomFilterManager = mock(BloomFilterManager.class);
        persistentManager = mock(PersistentManager.class);
        filterUpdateTimesCounter = new LongAdder();
        job = new PersistentFiltersJob(bloomFilterManager, persistentManager, filterUpdateTimesCounter, criteria);
    }

    @Test
    public void testCriteriaNotMeet() throws IOException {
        filterUpdateTimesCounter.reset();
        filterUpdateTimesCounter.add(5);
        job.run();
        assertThat(filterUpdateTimesCounter.sum()).isEqualTo(5);
        verify(persistentManager, never()).freezeAllFilters(bloomFilterManager);
    }

    @Test
    public void testPersistentWhenCriteriaMeet() throws IOException {
        filterUpdateTimesCounter.add(100);
        job.run();
        assertThat(filterUpdateTimesCounter.sum()).isZero();
        verify(persistentManager, times(1)).freezeAllFilters(bloomFilterManager);
    }

    @Test
    public void testPersistentThrowsException() throws IOException {
        final Exception exception = new IOException("expected exception");
        doThrow(exception).when(persistentManager).freezeAllFilters(bloomFilterManager);
        filterUpdateTimesCounter.add(100);
        // eat exception
        job.run();
        assertThat(filterUpdateTimesCounter.sum()).isZero();
        verify(persistentManager, times(1)).freezeAllFilters(bloomFilterManager);
    }

    @Test
    public void testPersistentThrowsError() throws IOException {
        final Error error = new Error("expected error");
        doThrow(error).when(persistentManager).freezeAllFilters(bloomFilterManager);
        filterUpdateTimesCounter.add(100);

        assertThatThrownBy(() -> job.run()).isSameAs(error);
        assertThat(filterUpdateTimesCounter.sum()).isZero();
        verify(persistentManager, times(1)).freezeAllFilters(bloomFilterManager);
    }
}