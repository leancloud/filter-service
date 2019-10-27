package cn.leancloud.filter.service;

import cn.leancloud.filter.service.metrics.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;

public class DefaultMetricsService implements MetricsService {
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
