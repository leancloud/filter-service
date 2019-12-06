package cn.leancloud.filter.service;

public final class InvalidBloomFilterPurgatory<F extends BloomFilter> implements Purgatory {
    private BloomFilterManager<F, ?> manager;

    public InvalidBloomFilterPurgatory(BloomFilterManager<F, ?> manager) {
        this.manager = manager;
    }

    @Override
    public void purge() {
        for (FilterRecord<F> holder : manager) {
            final F filter = holder.filter();
            if (!filter.valid()) {
                final String name = holder.name();
                manager.remove(name, filter);
            }
        }
    }
}
