package cn.leancloud.filter.service;

import java.util.Map;

public final class ExpirableBloomFilterPurgatory<F extends BloomFilter> implements Purgatory {
    private BloomFilterManager<F, ?> manager;

    public ExpirableBloomFilterPurgatory(BloomFilterManager<F, ?> manager) {
        this.manager = manager;
    }

    @Override
    public void purge() {
        for (Map.Entry<String, F> entry : manager) {
            final F filter = entry.getValue();
            if (filter instanceof ExpirableBloomFilter) {
                if (((ExpirableBloomFilter) filter).expired()) {
                    final String name = entry.getKey();
                    manager.remove(name);
                }
            }
        }
    }
}
