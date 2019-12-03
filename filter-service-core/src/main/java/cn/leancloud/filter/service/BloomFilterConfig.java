package cn.leancloud.filter.service;

/**
 * Basic configurations for a {@link BloomFilter}.
 *
 * @param <T> The actual subtype of this {@link BloomFilterConfig}
 */
public interface BloomFilterConfig<T extends BloomFilterConfig<T>> {
    /**
     * Get the number of expected insertions to a {@link BloomFilter}.
     *
     * @return the configured number of expected insertions
     */
    int expectedInsertions();

    /**
     * Set the number of expected insertions to a {@link BloomFilter}.
     *
     * @param expectedInsertions the number of expected insertions  (must be positive)
     * @return this
     */
    T setExpectedInsertions(int expectedInsertions);

    /**
     * Get the desired false positive probability for a {@link BloomFilter}.
     *
     * @return the desired false positive probability.
     */
    double fpp();

    /**
     * Set the desired false positive probability for a {@link BloomFilter}.
     *
     * @param fpp the desired false positive probability (must be positive and less than 1.0).
     * @return this
     */
    T setFpp(double fpp);
}
