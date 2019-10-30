package cn.leancloud.filter.service.metrics;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * This is a SPI interface to create a {@link MeterRegistry} to generate metrics from filter-service.
 * You can implement this interface as your requirements and provide the implementation
 * under {@code META-INF/services}. Then filter-service will load your implementation
 * by using {@link java.util.ServiceLoader}.
 */
public interface MetricsService {
    /**
     * A start hook called before filter-service start.
     */
    void start();

    /**
     * Create a new {@link MeterRegistry} used by filter-service to generate metrics.
     * @return a custom {@link MeterRegistry} which meet your requirements
     */
    MeterRegistry createMeterRegistry();

    /**
     * A stop hook called after filter-service is stopped.
     */
    void stop();
}
