package cn.leancloud.filter.service;

/**
 * An abstract bloom filter interface used to decouple our service
 * with the actual bloom filter implementations.
 */
public interface BloomFilter {
    /**
     * Get the number of expected insertions to this {@code BloomFilter}.
     *
     * @return the number of expected insertions
     */
    int expectedInsertions();

    /**
     * Get the desired false positive probability of this {@code BloomFilter}.
     *
     * @return the desired false positive probability
     */
    double fpp();

    /**
     * Get the name of this {@code BloomFilter}.
     *
     * @return the name of this {@code BloomFilter}.
     */
    String name();

    /**
     * Returns {@code true} if the input {@code value} <i>might</i> have been
     * put in this Bloom filter before, {@code false} if this is
     * <i>definitely</i> not the case.
     *
     * @param value the testing value
     * @return true if the {@code value} <i>might</i> have been put in this
     *         filter, false if this is <i>definitely</i> not the case.
     */
    boolean mightContain(String value);

    /**
     * Puts an element into this {@code BloomFilter}. Ensures that subsequent invocations of {@link
     * #mightContain(String)} with the same element will always return {@code true}.
     *
     * @param value the value to put into this {@code BloomFilter}
     * @return true if the Bloom filter's bits changed as a result of this operation. If the bits
     *         changed, this is <i>definitely</i> the first time {@code object} has been added to the
     *         filter. If the bits haven't changed, this <i>might</i> be the first time {@code value} has
     *         been added to the filter. Note that {@code set(String)} always returns the <i>opposite</i>
     *         result to what {@code mightContain(String)} would have returned at the time it is called.
     */
    boolean set(String value);
}
