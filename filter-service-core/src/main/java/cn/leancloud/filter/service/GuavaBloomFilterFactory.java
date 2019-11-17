package cn.leancloud.filter.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class GuavaBloomFilterFactory implements BloomFilterFactory<GuavaBloomFilter, ExpirableBloomFilterConfig> {
    @Override
    public GuavaBloomFilter createFilter(ExpirableBloomFilterConfig config) {
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiration;

        if (config.validPeriodAfterAccess() != null) {
            expiration = creation.plus(config.validPeriodAfterAccess());
        } else {
            expiration = creation.plus(config.validPeriodAfterWrite());
        }

        return new GuavaBloomFilter(
                config.expectedInsertions(),
                config.fpp(),
                creation,
                expiration,
                config.validPeriodAfterAccess());
    }
}
