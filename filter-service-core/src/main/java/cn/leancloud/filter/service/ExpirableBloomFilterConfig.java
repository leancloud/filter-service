package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;

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

    ExpirableBloomFilterConfig setValidPeriodAfterWrite(int validPeriod) {
        checkParameter("validPeriodAfterWrite",
                validPeriod >= 0,
                "expected: >= 0, actual: %s",
                validPeriod);

        if (validPeriod > 0) {
            this.validPeriodAfterWrite = Duration.ofSeconds(validPeriod);
        }
        return this;
    }

    @Nullable
    Duration validPeriodAfterAccess() {
        return validPeriodAfterAccess;
    }

    ExpirableBloomFilterConfig setValidPeriodAfterAccess(int validPeriod) {
        checkParameter("validPeriodAfterAccess",
                validPeriod >= 0,
                "expected: >= 0, actual: %s",
                validPeriod);

        if (validPeriod > 0) {
            this.validPeriodAfterAccess = Duration.ofSeconds(validPeriod);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ExpirableBloomFilterConfig that = (ExpirableBloomFilterConfig) o;
        return (validPeriodAfterWrite == null || validPeriodAfterWrite.equals(that.validPeriodAfterWrite)) &&
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
}
