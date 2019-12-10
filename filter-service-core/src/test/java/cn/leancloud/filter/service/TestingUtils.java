package cn.leancloud.filter.service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TestingUtils {
    private static final Duration validPeriodAfterAccess = Duration.ofSeconds(3);
    private static final int expectedInsertions = 1000000;
    private static final double fpp = 0.0001;
    private static final String testingFilterName = "testing_filter";

    public static String numberString(Number num) {
        return "" + num;
    }

    public static FilterRecord<BloomFilter> generateSingleFilterRecord() {
        return generateFilterRecords(1).get(0);
    }

    public static List<FilterRecord<BloomFilter>> generateFilterRecords(int size) {
        return generateFilterRecords(0, size);
    }

    public static List<FilterRecord<BloomFilter>> generateFilterRecords(int startNumName, int size) {
        List<FilterRecord<BloomFilter>> records = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
            final ZonedDateTime expiration = creation.plus(Duration.ofSeconds(10));
            final GuavaBloomFilter filter = new GuavaBloomFilter(
                    expectedInsertions + i,
                    fpp,
                    creation,
                    expiration,
                    validPeriodAfterAccess);
            final FilterRecord<BloomFilter> record = new FilterRecord<>(testingFilterName + "_" + i, filter);
            records.add(record);
        }

        return records;
    }

    public static GuavaBloomFilter generateInvalidFilter() {
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.minus(Duration.ofSeconds(10));
        return new GuavaBloomFilter(
                expectedInsertions,
                fpp,
                creation,
                expiration,
                validPeriodAfterAccess);
    }
}
