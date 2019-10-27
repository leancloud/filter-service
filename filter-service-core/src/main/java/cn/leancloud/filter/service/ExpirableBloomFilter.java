package cn.leancloud.filter.service;

import java.time.Instant;

public interface ExpirableBloomFilter extends BloomFilter{
    Instant created();

    Instant expiration();

    boolean expired();
}
