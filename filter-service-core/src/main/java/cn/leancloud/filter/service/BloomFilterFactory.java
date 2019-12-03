package cn.leancloud.filter.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * A factory used to create a {@link BloomFilter}.
 *
 * @param <F> the type of the created {@link BloomFilter}
 * @param <C> the type of {@link BloomFilterConfig} used to create {@link BloomFilter}
 */
public interface BloomFilterFactory<F extends BloomFilter, C extends BloomFilterConfig> {
    /**
     * Create a {@link BloomFilter} with type T.
     *
     * @param config the configuration used to create {@link BloomFilter}
     * @return the created {@link BloomFilter}
     */
    F createFilter(C config);

    F readFrom(InputStream stream) throws IOException;
}
