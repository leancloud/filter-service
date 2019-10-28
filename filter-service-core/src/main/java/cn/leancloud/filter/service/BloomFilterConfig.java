package cn.leancloud.filter.service;

public interface BloomFilterConfig<T extends BloomFilterConfig<T>> {
    int DEFAULT_EXPECTED_INSERTIONS = 1000_000;
    double DEFAULT_FALSE_POSITIVE_PROBABILITY = 0.0001;

    String name();

    int expectedInsertions();

    T setExpectedInsertions(int expectedInsertions);

    double fpp();

    T setFpp(double fpp);
}
