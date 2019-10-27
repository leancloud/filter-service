package cn.leancloud.filter.service;

public interface BloomFilterFactory<F extends BloomFilter, C extends BloomFilterConfig> {
    F createFilter(C config);
}
