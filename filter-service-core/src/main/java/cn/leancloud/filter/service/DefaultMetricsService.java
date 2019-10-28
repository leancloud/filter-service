package cn.leancloud.filter.service;

import cn.leancloud.filter.service.metrics.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;

/**
 * A default implementation of {@link MeterRegistry}. It is used when no SPI service
 * which implement {@link MeterRegistry} can be found under path {@code META-INF/services}.
 * It will write all the metrics to log.
 */
public final class DefaultMetricsService implements MetricsService {
    @Override
    public void start() {
    }

    @Override
    public MeterRegistry createMeterRegistry() {
        return new LoggingMeterRegistry();
    }

    @Override
    public void stop() {

    }
}
