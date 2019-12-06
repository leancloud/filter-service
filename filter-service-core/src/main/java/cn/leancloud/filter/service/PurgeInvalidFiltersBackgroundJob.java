package cn.leancloud.filter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class PurgeInvalidFiltersBackgroundJob<F extends BloomFilter> implements BackgroundJob {
    private final Timer purgeExpiredFiltersTimer;
    private final BloomFilterManager<F, ?> manager;
    private final Duration purgeFilterInterval;
    @Nullable
    private ScheduledFuture<?> future;

    PurgeInvalidFiltersBackgroundJob(MeterRegistry registry,
                                             BloomFilterManager<F, ?> manager,
                                             Duration purgeFilterInterval) {
        this.purgeExpiredFiltersTimer = registry.timer("filter-service.purgeExpiredFilters");
        this.manager = manager;
        this.purgeFilterInterval = purgeFilterInterval;
    }

    @Override
    public void start(ScheduledExecutorService scheduledExecutorService) {
        final InvalidBloomFilterPurgatory<F> purgatory
                = new InvalidBloomFilterPurgatory<>(manager);
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
