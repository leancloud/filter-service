package cn.leancloud.filter.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.LongAdder;

public final class CountUpdateBloomFilterFactory<C extends BloomFilterConfig<? extends C>>
        implements BloomFilterFactory<CountUpdateBloomFilterWrapper, C> {
    private final BloomFilterFactory<?, C> factory;
    private final LongAdder counter;

    public CountUpdateBloomFilterFactory(BloomFilterFactory<?, C> factory, LongAdder counter) {
        this.factory = factory;
        this.counter = counter;
    }

    @Override
    public CountUpdateBloomFilterWrapper createFilter(C config) {
        return new CountUpdateBloomFilterWrapper(counter, factory.createFilter(config));
    }

    @Override
    public CountUpdateBloomFilterWrapper readFrom(InputStream stream) throws IOException {
        return new CountUpdateBloomFilterWrapper(counter, factory.readFrom(stream));
    }
}
