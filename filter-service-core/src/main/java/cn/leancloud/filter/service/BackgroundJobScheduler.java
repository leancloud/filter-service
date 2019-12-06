package cn.leancloud.filter.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;

final class BackgroundJobScheduler {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundJobScheduler.class);
    private final ScheduledExecutorService scheduledExecutorService;
    private final MeterRegistry registry;
    private final CopyOnWriteArrayList<ScheduledFuture<?>> futures;

    BackgroundJobScheduler(MeterRegistry registry) {
        this.registry = registry;
        this.futures = new CopyOnWriteArrayList<>();
        this.scheduledExecutorService = Executors.newScheduledThreadPool(10,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-worker-%s")
                        .setUncaughtExceptionHandler((t, e) ->
                                logger.error("Scheduled worker thread: " + t.getName() + " got uncaught exception.", e))
                        .build());
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