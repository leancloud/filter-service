package cn.leancloud.filter.service;

import java.util.Objects;

import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkNotNull;
import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkParameter;

public abstract class AbstractBloomFilterConfig<T extends AbstractBloomFilterConfig<T>> implements BloomFilterConfig<T> {
    private int expectedInsertions;
    private double fpp;
    private String name;

    AbstractBloomFilterConfig(String name) {
        this.name = name;
        this.expectedInsertions = DEFAULT_EXPECTED_INSERTIONS;
        this.fpp = DEFAULT_FALSE_POSITIVE_PROBABILITY;
    }

    public String name() {
        return name;
    }

    public int expectedInsertions() {
        checkNotNull("expectedInsertions", expectedInsertions);
        return expectedInsertions;
    }

    public double fpp() {
        checkNotNull("fpp", fpp);
        return fpp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setExpectedInsertions(int expectedInsertions) {
        checkParameter("expectedInsertions",
                expectedInsertions > 0,
                "expected: > 0, actual: %s",
                expectedInsertions);

        this.expectedInsertions = expectedInsertions;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setFpp(double fpp) {
        checkParameter("fpp",
                fpp > 0.0d && fpp < 1.0d,
                "expected: > 0 && < 1, actual: %s",
                fpp);

        this.fpp = fpp;
        return (T) this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractBloomFilterConfig<?> that = (AbstractBloomFilterConfig<?>) o;
        return expectedInsertions() == that.expectedInsertions() &&
                Double.compare(that.fpp(), fpp()) == 0 &&
                name().equals(that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(expectedInsertions(), fpp(), name());
    }

    @Override
    public String toString() {
        return "expectedInsertions=" + expectedInsertions +
                ", fpp=" + fpp +
                ", name='" + name + '\'';
    }
}
