package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;

import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkNotNull;
import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkParameter;

final class ExpirableBloomFilterConfig extends AbstractBloomFilterConfig<ExpirableBloomFilterConfig> {
    static final Duration DEFAULT_VALID_PERIOD = Duration.ofDays(1);

    private Duration validPeriodAfterWrite;
    @Nullable
    private Duration validPeriodAfterAccess;

    ExpirableBloomFilterConfig() {
        this.validPeriodAfterWrite = DEFAULT_VALID_PERIOD;
    }

    ExpirableBloomFilterConfig(int expectedInsertions, double fpp) {
        super(expectedInsertions, fpp);
        this.validPeriodAfterWrite = DEFAULT_VALID_PERIOD;
    }

    Duration validPeriodAfterWrite() {
        return validPeriodAfterWrite;
    }

    ExpirableBloomFilterConfig setValidPeriodAfterWrite(Duration validPeriod) {
        checkNotNull("validPeriodAfterWrite", validPeriod);
        checkParameter("validPeriodAfterWrite",
                validPeriod.getSeconds() > 0L,
                "expected: > 0, actual: %s",
                validPeriod);

        this.validPeriodAfterWrite = validPeriod;
        return this;
    }

    @Nullable
    Duration validPeriodAfterAccess() {
        return validPeriodAfterAccess;
    }

    ExpirableBloomFilterConfig setValidPeriodAfterAccess(Duration validPeriod) {
        checkNotNull("validPeriodAfterAccess", validPeriod);
        checkParameter("validPeriodAfterAccess",
                validPeriod.getSeconds() > 0L,
                "expected: > 0, actual: %s",
                validPeriod);

        this.validPeriodAfterAccess = validPeriod;

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ExpirableBloomFilterConfig that = (ExpirableBloomFilterConfig) o;
        return validPeriodAfterWrite.equals(that.validPeriodAfterWrite) &&
                (validPeriodAfterAccess == null || validPeriodAfterAccess.equals(that.validPeriodAfterAccess));
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), validPeriodAfterWrite, validPeriodAfterAccess);
    }

    @Override
    public String toString() {
        return "ExpirableBloomFilterConfig{" +
                super.toString() +
                "validPeriodAfterWrite=" + validPeriodAfterWrite +
                ", validPeriodAfterAccess=" + validPeriodAfterAccess +
                '}';
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        final ExpirableBloomFilterConfig config = (ExpirableBloomFilterConfig) super.clone();
        config.validPeriodAfterWrite = validPeriodAfterWrite;
        config.validPeriodAfterAccess = validPeriodAfterAccess;
        return config;
    }
}
