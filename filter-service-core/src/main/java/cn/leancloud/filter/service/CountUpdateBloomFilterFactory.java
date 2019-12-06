package cn.leancloud.filter.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.LongAdder;

public final class CountUpdateBloomFilterFactory<C extends BloomFilterConfig<? extends C>>
        implements BloomFilterFactory<CountUpdateBloomFilterWrapper, C> {
    private final BloomFilterFactory<?, C> factory;
    private final LongAdder filterUpdateTimesCounter;

    CountUpdateBloomFilterFactory(BloomFilterFactory<?, C> factory, LongAdder filterUpdateTimesCounter) {
        this.factory = factory;
        this.filterUpdateTimesCounter = filterUpdateTimesCounter;
    }

    LongAdder filterUpdateTimesCounter() {
        return filterUpdateTimesCounter;
    }

    @Override
    public CountUpdateBloomFilterWrapper createFilter(C config) {
        filterUpdateTimesCounter.increment();
        return new CountUpdateBloomFilterWrapper(factory.createFilter(config), filterUpdateTimesCounter);
    }

    @Override
    public CountUpdateBloomFilterWrapper readFrom(InputStream stream) throws IOException {
        return new CountUpdateBloomFilterWrapper(factory.readFrom(stream), filterUpdateTimesCounter);
    }
}
