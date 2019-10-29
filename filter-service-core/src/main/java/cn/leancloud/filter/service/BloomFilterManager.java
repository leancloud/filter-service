package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A manager to manage {@link BloomFilter}s.
 *
 * @param <F> the type of the managed {@link BloomFilter}s
 * @param <C> the type of the configuration used by the managed {@link BloomFilter}s
 */
public interface BloomFilterManager<F extends BloomFilter, C extends BloomFilterConfig<? extends C>> {
    /**
     * Create a new Bloom filter with the input configuration.
     *
     * @param config the configuration used to create a new Bloom filter
     * @return the created Bloom filter
     */
    F createFilter(C config);

    /**
     * Get the Bloom filter with the input name.
     *
     * @param name the name of the target Bloom filter
     * @return the Bloom filter if it exists, or null if no Bloom filter with
     *         the target name in this manager
     */
    @Nullable
    F getFilter(String name);

    /**
     * Get the Bloom filter with the input name. The difference between this method
     * and {@link #getFilter(String)} is that this method will throw a {@link FilterNotFoundException}
     * instead of returns null when there's no Bloom filter with the target name in this manager.
     *
     * @param name the name of the target Bloom filter
     * @return the Bloom filter with the target name in this manager
     * @throws FilterNotFoundException when there's no Bloom filter with the target name
     *                                 exists in this manager
     */
    F safeGetFilter(String name) throws FilterNotFoundException;

    /**
     * Returns all the names of the Bloom filters in this manager.
     *
     * @return a list of names of the Bloom filters in this manager.
     */
    List<String> getAllFilterNames();

    /**
     * Returns how many Bloom filters managed by this manager.
     *
     * @return how many Bloom filters managed by this manager.
     */
    int size();

    /**
     * Remove a Bloom filter with target name from this manager.
     *
     * @param name the name of the Bloom filter to remove.
     */
    void remove(String name);
}
