package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkNotNull;
import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkParameter;

final class ExpirableBloomFilterConfig extends AbstractBloomFilterConfig<ExpirableBloomFilterConfig> {
    private Duration validPeriodAfterCreate;
    @Nullable
    private Duration validPeriodAfterAccess;

    ExpirableBloomFilterConfig() {
        this.validPeriodAfterCreate = Configuration.defaultValidPeriodAfterCreate();
    }

    ExpirableBloomFilterConfig(int expectedInsertions, double fpp) {
        super(expectedInsertions, fpp);
        this.validPeriodAfterCreate = Configuration.defaultValidPeriodAfterCreate();
    }

    Duration validPeriodAfterCreate() {
        return validPeriodAfterCreate;
    }

    ExpirableBloomFilterConfig setValidPeriodAfterCreate(Duration validPeriod) {
        checkNotNull("validPeriodAfterCreate", validPeriod);
        checkParameter("validPeriodAfterCreate",
                validPeriod.getSeconds() > 0L,
                "expected: > 0, actual: %s",
                validPeriod);

        this.validPeriodAfterCreate = validPeriod;
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

    /**
     * Compute the expiration time of the {@link BloomFilter}.
     *
     * @param creation the creation time of a {@link BloomFilter}
     * @return the expiration time for {@link BloomFilter}
     */
    ZonedDateTime expiration(ZonedDateTime creation) {
        ZonedDateTime expiration;
        if (validPeriodAfterAccess != null) {
            expiration = creation.plus(validPeriodAfterAccess);
        } else {
            expiration = creation.plus(validPeriodAfterCreate);
        }
        return expiration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ExpirableBloomFilterConfig that = (ExpirableBloomFilterConfig) o;
        return validPeriodAfterCreate.equals(that.validPeriodAfterCreate) &&
                (validPeriodAfterAccess == null || validPeriodAfterAccess.equals(that.validPeriodAfterAccess));
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), validPeriodAfterCreate, validPeriodAfterAccess);
    }

    @Override
    public String toString() {
        return "ExpirableBloomFilterConfig{" +
                super.toString() +
                "validPeriodAfterCreate=" + validPeriodAfterCreate +
                ", validPeriodAfterAccess=" + validPeriodAfterAccess +
                '}';
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        final ExpirableBloomFilterConfig config = (ExpirableBloomFilterConfig) super.clone();
        config.validPeriodAfterCreate = validPeriodAfterCreate;
        config.validPeriodAfterAccess = validPeriodAfterAccess;
        return config;
    }
}
