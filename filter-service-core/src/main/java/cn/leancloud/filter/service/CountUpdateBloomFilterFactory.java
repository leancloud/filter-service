package cn.leancloud.filter.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.LongAdder;

public final class CountUpdateBloomFilterFactory<C extends BloomFilterConfig<? extends C>>
        implements BloomFilterFactory<CountUpdateBloomFilterWrapper, C> {
    private final BloomFilterFactory<?, C> factory;
    private final LongAdder filterUpdateCounter;

    public CountUpdateBloomFilterFactory(BloomFilterFactory<?, C> factory, LongAdder filterUpdateCounter) {
        this.factory = factory;
        this.filterUpdateCounter = filterUpdateCounter;
    }

    @Override
    public CountUpdateBloomFilterWrapper createFilter(C config) {
        return new CountUpdateBloomFilterWrapper(factory.createFilter(config), filterUpdateCounter);
    }

    @Override
    public CountUpdateBloomFilterWrapper readFrom(InputStream stream) throws IOException {
        return new CountUpdateBloomFilterWrapper(factory.readFrom(stream), filterUpdateCounter);
    }
}
