package cn.leancloud.filter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.*;

final class BackgroundJobScheduler {
    private final ScheduledExecutorService scheduledExecutorService;
    private final MeterRegistry registry;
    private final CopyOnWriteArrayList<ScheduledFuture<?>> futures;

    BackgroundJobScheduler(MeterRegistry registry, ScheduledExecutorService scheduledExecutorService) {
        this.registry = registry;
        this.futures = new CopyOnWriteArrayList<>();
        this.scheduledExecutorService = scheduledExecutorService;
    }

    void scheduleFixedIntervalJob(Runnable runnable, String name, Duration interval) {
        Timer timer = registry.timer("filter-service." + name);
        ScheduledFuture<?> future = scheduledExecutorService.scheduleWithFixedDelay(
                timer.wrap(runnable),
                interval.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS);
        futures.add(future);
    }

    void stop() {
        for (ScheduledFuture<?> f : futures) {
            f.cancel(false);
        }

        final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
        scheduledExecutorService.execute(() ->
                shutdownFuture.complete(null)
        );

        shutdownFuture.join();
        scheduledExecutorService.shutdown();
    }
}
