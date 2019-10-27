package cn.leancloud.filter.service;

import java.time.Duration;
import java.util.Objects;

import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkParameter;

final class ExpirableBloomFilterConfig extends AbstractBloomFilterConfig<ExpirableBloomFilterConfig> {
    private static final Duration DEFAULT_VALID_PERIOD = Duration.ofDays(1);

    private Duration validPeriod;

    ExpirableBloomFilterConfig(String name) {
        super(name);
        this.validPeriod = DEFAULT_VALID_PERIOD;
    }

    Duration getValidPeriod() {
        return validPeriod;
    }

    ExpirableBloomFilterConfig setValidPeriod(long validPeriod) {
        checkParameter("validPeriod",
                validPeriod >= 0,
                "expected: >= 0, actual: %s",
                validPeriod);

        if (validPeriod > 0) {
            this.validPeriod = Duration.ofSeconds(validPeriod);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ExpirableBloomFilterConfig that = (ExpirableBloomFilterConfig) o;
        return getValidPeriod().equals(that.getValidPeriod());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getValidPeriod());
    }

    @Override
    public String toString() {
        return "ExpirableBloomFilterConfig{" +
                super.toString() +
                "validPeriod=" + validPeriod +
                '}';
    }
}
