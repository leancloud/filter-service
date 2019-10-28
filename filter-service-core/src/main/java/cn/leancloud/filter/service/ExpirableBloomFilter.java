package cn.leancloud.filter.service;

import java.time.Instant;

/**
 * A bloom filter which have expiration and can expire after expiration.
 */
public interface ExpirableBloomFilter extends BloomFilter {
    /**
     * The time of this Bloom filter will expire.
     *
     * @return the expiration time of this Bloom filter
     */
    Instant expiration();

    /**
     * Check if this Bloom filter is already expired.
     *
     * @return true when this Bloom filter is already expired.
     */
    boolean expired();
}
