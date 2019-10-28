package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.util.List;

public interface BloomFilterManager<F extends BloomFilter, C extends BloomFilterConfig<? extends C>> {
    F createFilter(C configs);

    F getOrCreateDefaultFilter(String name);

    @Nullable
    F getFilter(String name);

    F safeGetFilter(String name) throws FilterNotFoundException;

    List<String> getAllFilterNames();

    int size();

    void remove(String name);
}
