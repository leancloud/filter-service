package cn.leancloud.filter.service;

public final class ExpirableBloomFilterPurgatory<F extends BloomFilter> implements Purgatory {
    private BloomFilterManager<F, ?> manager;

    public ExpirableBloomFilterPurgatory(BloomFilterManager<F, ?> manager) {
        this.manager = manager;
    }

    @Override
    public void purge() {
        for (FilterHolder<F> holder : manager) {
            final F filter = holder.filter();
            if (filter instanceof ExpirableBloomFilter) {
                if (((ExpirableBloomFilter) filter).expired()) {
                    final String name = holder.name();
                    manager.remove(name, filter);
                }
            }
        }
    }
}
