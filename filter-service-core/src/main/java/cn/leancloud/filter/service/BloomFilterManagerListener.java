package cn.leancloud.filter.service;

/**
 * A lister for {@link BloomFilterManager}.
 *
 * @param <F> the type of the Bloom filters managed by {@link BloomFilterManager}
 * @param <C> the type of the configuration used by {@link BloomFilterManager}
 */
public interface BloomFilterManagerListener<F extends BloomFilter, C extends BloomFilterConfig<? extends C>> {
    /**
     * Called when a Bloom filter was created by {@link BloomFilterManager}.
     * Please note do not block in this method, due to this maybe a synchronous method.
     *
     * @param config the configuration used to create the {@code filter}
     * @param filter the newly created Bloom filter
     */
    default void onBloomFilterCreated(String name, C config, F filter) {}

    /**
     * Called when a Bloom filter was removed from {@link BloomFilterManager}.
     * Please note do not block in this method, due to this maybe a synchronous method.
     *
     * @param filter the removed Bloom filter
     */
    default void onBloomFilterRemoved(String name, F filter) {}
}
