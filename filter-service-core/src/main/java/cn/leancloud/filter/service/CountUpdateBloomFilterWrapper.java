package cn.leancloud.filter.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.LongAdder;

public final class CountUpdateBloomFilterWrapper implements BloomFilter {
    private final LongAdder filterUpdateCounter;
    private final BloomFilter filter;

    CountUpdateBloomFilterWrapper(BloomFilter filter, LongAdder filterUpdateCounter) {
        this.filterUpdateCounter = filterUpdateCounter;
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
        filterUpdateCounter.increment();
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
}
