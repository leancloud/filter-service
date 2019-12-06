package cn.leancloud.filter.service;

import cn.leancloud.filter.service.Configuration.TriggerPersistenceCriteria;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class PersistentFiltersBackgroundJob<F extends BloomFilter> implements BackgroundJob {
    private final Timer persistentFiltersTimer;
    private final Iterable<FilterRecord<F>> records;
    private final PersistentManager<F> persistentManager;
    private final LongAdder filterUpdateTimesCounter;
    private final List<ScheduledFuture<?>> futures;
    private final Lock persistenceLock;
    private final Collection<TriggerPersistenceCriteria> persistenceCriteria;

    PersistentFiltersBackgroundJob(MeterRegistry registry,
                                   Iterable<FilterRecord<F>> records,
                                   PersistentManager<F> persistentManager,
                                   LongAdder filterUpdateTimesCounter,
                                   Collection<TriggerPersistenceCriteria> persistenceCriteria) {
        this.persistentFiltersTimer = registry.timer("filter-service.persistentFilters");
        this.records = records;
        this.persistentManager = persistentManager;
        this.filterUpdateTimesCounter = filterUpdateTimesCounter;
        this.futures = new ArrayList<>();
        this.persistenceLock = new ReentrantLock();
        this.persistenceCriteria = persistenceCriteria;
    }

    @Override
    public void start(ScheduledExecutorService scheduledExecutorService) {
        for (TriggerPersistenceCriteria c : persistenceCriteria) {
            futures.add(scheduledExecutorService.scheduleWithFixedDelay(() -> {
                persistenceLock.lock();
                try {
                    final long sum = filterUpdateTimesCounter.sum();
                    if (sum > c.updatesThreshold()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Updated {} times in last {} seconds meets threshold {} to persistence filters",
                                    sum, c.checkingPeriod().getSeconds(), c.updatesThreshold());
                        }
                        filterUpdateTimesCounter.reset();
                        doPersistence();
                    }
                } finally {
                    persistenceLock.unlock();
                }
            }, c.checkingPeriod().toMillis(), c.checkingPeriod().toMillis(), TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void stop() {
        for (ScheduledFuture<?> f : futures) {
            f.cancel(false);
        }
    }

    private void doPersistence() {
        final long start = System.nanoTime();
        try {
            persistentManager.freezeAllFilters(records);
        } catch (IOException ex) {
            logger.error("Persistent bloom filters failed.", ex);
        } catch (Throwable t) {
            // sorry for the duplication, but currently I don't figure out another way
            // to catch the direct buffer OOM when freeze filters to file
            logger.error("Persistent bloom filters failed.", t);
            throw t;
        } finally {
            persistentFiltersTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }
}
