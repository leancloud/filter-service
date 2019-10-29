package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class BloomFilterManagerImpl<T extends ExpirableBloomFilter>
        implements BloomFilterManager<T, ExpirableBloomFilterConfig>, Purgatory,
        Listenable<BloomFilterManagerListener<T, ExpirableBloomFilterConfig>> {
    private static final FilterNotFoundException FILTER_NOT_FOUND_EXCEPTION = new FilterNotFoundException();

    private final List<BloomFilterManagerListener<T, ExpirableBloomFilterConfig>> listeners;
    private final ConcurrentHashMap<String, T> filterMap;
    private final BloomFilterFactory<? extends T, ? super ExpirableBloomFilterConfig> factory;

    BloomFilterManagerImpl(BloomFilterFactory<? extends T, ? super ExpirableBloomFilterConfig> factory) {
        this.filterMap = new ConcurrentHashMap<>();
        this.factory = factory;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public T createFilter(ExpirableBloomFilterConfig config) {
        final var filter = factory.createFilter(config);
        filterMap.put(config.name(), filter);

        notifyBloomFilterCreated(config, filter);
        return filter;
    }

    @Override
    public void addListener(BloomFilterManagerListener<T, ExpirableBloomFilterConfig> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(BloomFilterManagerListener<T, ExpirableBloomFilterConfig> listener) {
        return listeners.remove(listener);
    }

    @Nullable
    @Override
    public T getFilter(String name) {
        return filterMap.get(name);
    }

    @Override
    public T safeGetFilter(String name) throws FilterNotFoundException {
        final var filter = getFilter(name);
        if (filter == null) {
            throw FILTER_NOT_FOUND_EXCEPTION;
        }
        return filter;
    }

    public List<String> getAllFilterNames() {
        return new ArrayList<>(filterMap.keySet());
    }

    @Override
    public int size() {
        return filterMap.size();
    }

    @Override
    public void remove(String name) {
        final var filter = filterMap.remove(name);
        if (filter != null) {
            notifyBloomFilterRemoved(filter);
        }
    }

    @Override
    public void purge() {
        for (Map.Entry<String, T> entry : filterMap.entrySet()) {
            final var filter = entry.getValue();
            if (filter.expired()) {
                if (filterMap.remove(entry.getKey(), filter)) {
                    notifyBloomFilterRemoved(filter);
                }
            }
        }
    }

    private void notifyBloomFilterCreated(ExpirableBloomFilterConfig config, T filter) {
        notifyListeners(l -> l.onBloomFilterCreated(config, filter));
    }

    private void notifyBloomFilterRemoved(T filter) {
        notifyListeners(l -> l.onBloomFilterRemoved(filter));
    }

    private void notifyListeners(Consumer<BloomFilterManagerListener<T, ExpirableBloomFilterConfig>> consumer) {
        for (BloomFilterManagerListener<T, ExpirableBloomFilterConfig> listener : listeners) {
            consumer.accept(listener);
        }
    }
}
