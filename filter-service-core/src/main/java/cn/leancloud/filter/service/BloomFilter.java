package cn.leancloud.filter.service;

public interface BloomFilter {
    int expectedInsertions();

    double fpp();

    String name();

    boolean mightContain(String value);

    void set(String value);
}
