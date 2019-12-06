package cn.leancloud.filter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class PurgeFiltersBackgroundJob<F extends BloomFilter> implements BackgroundJob {
    private final Timer purgeExpiredFiltersTimer;
    private final Purgatory purgatory;
    private final Duration purgeFilterInterval;
    @Nullable
    private ScheduledFuture<?> future;

    PurgeFiltersBackgroundJob(MeterRegistry registry,
                              Purgatory purgatory,
                              Duration purgeFilterInterval) {
        this.purgeExpiredFiltersTimer = registry.timer("filter-service.purgeExpiredFilters");
        this.purgatory = purgatory;
        this.purgeFilterInterval = purgeFilterInterval;
    }

    @Override
    public void start(ScheduledExecutorService scheduledExecutorService) {
        future = scheduledExecutorService.scheduleWithFixedDelay(purgeExpiredFiltersTimer.wrap(() -> {
            try {
                purgatory.purge();
            } catch (Throwable ex) {
                logger.error("Purge bloom filter service failed.", ex);
                throw ex;
            }
        }), purgeFilterInterval.toMillis(), purgeFilterInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (future != null) {
            future.cancel(false);
        }
    }
}
