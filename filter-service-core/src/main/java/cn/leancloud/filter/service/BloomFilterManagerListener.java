package cn.leancloud.filter.service;

public interface BloomFilterManagerListener<F extends BloomFilter, C extends BloomFilterConfig<? extends C>> {
    void onBloomFilterCreated(C config, F filter);

    void onBloomFilterRemoved(F filter);
}
