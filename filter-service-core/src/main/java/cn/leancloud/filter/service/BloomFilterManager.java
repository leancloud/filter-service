package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A manager to manage {@link BloomFilter}s.
 *
 * @param <F> the type of the managed {@link BloomFilter}s
 * @param <C> the type of the configuration used by the managed {@link BloomFilter}s
 */
public interface BloomFilterManager<F extends BloomFilter, C extends BloomFilterConfig<? extends C>> extends Iterable<FilterHolder<F>> {
    /**
     * The result of the create filter operations.
     *
     * @param <F> same with the type F on {@link BloomFilterManager}
     */
    final class CreateFilterResult<F extends BloomFilter> {
        private final F filter;
        private final boolean created;

        /**
         * Create a result.
         *
         * @param filter  The newly created filter when {@code created} is true
         *                or the previous exists filter if {@code created} is false.
         * @param created true when {@code filter} is newly created,
         *                or false when {@code filter} is an exists filter
         */
        public CreateFilterResult(F filter, boolean created) {
            this.filter = filter;
            this.created = created;
        }

        /**
         * Get the contained {@code filter}.
         *
         * @return the contained {@code filter}.
         */
        public F getFilter() {
            return filter;
        }

        /**
         * Check if the contained {@code filter} is newly created.
         *
         * @return true when the {@code filter} is newly created
         */
        public boolean isCreated() {
            return created;
        }
    }

    /**
     * Create a new Bloom filter with the input {@code name} and {@code configuration} only when
     * there's no Bloom filter with the same {@code name} exists in this {@code BloomFilterManager}.
     *
     * @param name   the name of the Bloom filter to create
     * @param config the configuration used to create a new Bloom filter
     * @return a {@link CreateFilterResult} instance contains a newly created filter if no Bloom filter
     * with the same {@code name} exists or the previous exists Bloom filter with the same
     * {@code name}
     */
    default CreateFilterResult<F> createFilter(String name, C config) {
        return createFilter(name, config, false);
    }

    /**
     * Try to create a new Bloom filter with the input configuration.
     *
     * @param name      the name of the Bloom filter to create
     * @param config    the configuration used to create a new Bloom filter
     * @param overwrite true to create a new Bloom filter and put it into this {@code BloomFilterManager}
     *                  no matter whether there's already an Bloom filter with the same {@code name} exits.
     *                  false have the same effect with {@link #createFilter(String, BloomFilterConfig)}
     * @return a {@link CreateFilterResult} instance contains a newly created filter if no Bloom filter
     * with the same {@code name} exists or {@code overwrite} is true, otherwise contains the
     * previous exists Bloom filter with the same {@code name}
     */
    CreateFilterResult<F> createFilter(String name, C config, boolean overwrite);

    /**
     * Add existent Bloom filters to this manager.
     *
     * @param filters a {@link Iterable} of the filters to add
     */
    void addFilters(Iterable<FilterHolder<? extends F>> filters);

    /**
     * Get the Bloom filter with the input name.
     *
     * @param name the name of the target Bloom filter
     * @return the Bloom filter if it exists, or null if no Bloom filter with
     * the target name in this manager
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
    F ensureGetFilter(String name) throws FilterNotFoundException;

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

    /**
     * Remove a Bloom filter with target name from this manager only
     * if the target name currently mapped to a given filter.
     *
     * @param name   the name of the Bloom filter to remove.
     * @param filter the Bloom filter to remove.
     */
    void remove(String name, F filter);
}
