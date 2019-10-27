package cn.leancloud.filter.service.metrics;

import io.micrometer.core.instrument.MeterRegistry;

public interface MetricsService {
    void start();

    MeterRegistry createMeterRegistry();

    void stop();
}
