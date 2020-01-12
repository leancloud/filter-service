package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.LongAdder;

public final class CountUpdateBloomFilterWrapper implements BloomFilter {
    private final LongAdder filterUpdateTimesCounter;
    @JsonUnwrapped
    private final BloomFilter filter;

    CountUpdateBloomFilterWrapper(BloomFilter filter, LongAdder filterUpdateTimesCounter) {
        this.filterUpdateTimesCounter = filterUpdateTimesCounter;
        this.filter = filter;
    }

    @Override
    public int expectedInsertions() {
        return filter.expectedInsertions();
    }

    @Override
    public double fpp() {
        return filter.fpp();
    }

    @Override
    public boolean mightContain(String value) {
        return filter.mightContain(value);
    }

    @Override
    public boolean set(String value) {
        filterUpdateTimesCounter.increment();
        return filter.set(value);
    }

    @Override
    public boolean valid() {
        return filter.valid();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        filter.writeTo(out);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CountUpdateBloomFilterWrapper wrapper = (CountUpdateBloomFilterWrapper) o;
        return filterUpdateTimesCounter.equals(wrapper.filterUpdateTimesCounter) &&
                filter.equals(wrapper.filter);
    }

    @Override
    public int hashCode() {
        return filter.hashCode();
    }
}
