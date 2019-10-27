package cn.leancloud.filter.service;

public class GuavaBloomFilterFactory implements BloomFilterFactory<GuavaBloomFilter, ExpirableBloomFilterConfig> {
    @Override
    public GuavaBloomFilter createFilter(ExpirableBloomFilterConfig config) {
        return new GuavaBloomFilter(
                config.getName(),
                config.getExpectedInsertions(),
                config.getFpp(),
                config.getValidPeriod());
    }
}
