package cn.leancloud.filter.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class GuavaBloomFilterFactory implements BloomFilterFactory<GuavaBloomFilter, ExpirableBloomFilterConfig> {
    @Override
    public GuavaBloomFilter createFilter(ExpirableBloomFilterConfig config) {
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = config.expiration(creation);

        return new GuavaBloomFilter(
                config.expectedInsertions(),
                config.fpp(),
                creation,
                expiration,
                config.validPeriodAfterAccess());
    }

    public GuavaBloomFilter readFrom(InputStream stream) throws IOException {
        return GuavaBloomFilter.readFrom(stream);
    }
}
