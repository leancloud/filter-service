package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class BloomFilterManagerImpl<T extends ExpirableBloomFilter>
        implements BloomFilterManager<T, ExpirableBloomFilterConfig>, Purgatory,
        Listenable<BloomFilterManagerListener<T, ExpirableBloomFilterConfig>> {
    private static final FilterNotFoundException FILTER_NOT_FOUND_EXCEPTION = new FilterNotFoundException();

    private final List<BloomFilterManagerListener<T, ExpirableBloomFilterConfig>> listeners;
    private final ConcurrentHashMap<String, T> filterMap;
    private final BloomFilterFactory<? extends T, ? super ExpirableBloomFilterConfig> factory;
    private final Lock filterMapLock = new ReentrantLock();

    BloomFilterManagerImpl(BloomFilterFactory<? extends T, ? super ExpirableBloomFilterConfig> factory) {
        this.filterMap = new ConcurrentHashMap<>();
        this.factory = factory;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public CreateFilterResult<T> createFilter(String name, ExpirableBloomFilterConfig config, boolean overwrite) {
        T filter;
        T prevFilter;
        boolean created = false;
        filterMapLock.lock();
        try {
            prevFilter = filterMap.get(name);
            if (overwrite || prevFilter == null || prevFilter.expired()) {
                filter = factory.createFilter(config);
                filterMap.put(name, filter);
                created = true;
            } else {
                filter = prevFilter;
            }
        } finally {
            filterMapLock.unlock();
        }

        if (created) {
            if (prevFilter != null) {
                notifyBloomFilterRemoved(name, prevFilter);
            }

            notifyBloomFilterCreated(name, config, filter);
        }

        return new CreateFilterResult<>(filter, created);
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
        final T filter = getFilter(name);
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
        final T filter;

        filterMapLock.lock();
        try {
            filter = filterMap.remove(name);
        } finally {
            filterMapLock.unlock();
        }

        if (filter != null) {
            notifyBloomFilterRemoved(name, filter);
        }
    }

    @Override
    public void purge() {
        for (Map.Entry<String, T> entry : filterMap.entrySet()) {
            final T filter = entry.getValue();
            if (filter.expired()) {
                final String name = entry.getKey();
                final boolean removed;
                filterMapLock.lock();
                try {
                    removed = filterMap.remove(name, filter);
                } finally {
                    filterMapLock.unlock();
                }

                if (removed) {
                    notifyBloomFilterRemoved(name, filter);
                }
            }
        }
    }

    private void notifyBloomFilterCreated(String name, ExpirableBloomFilterConfig config, T filter) {
        notifyListeners(l -> l.onBloomFilterCreated(name, config, filter));
    }

    private void notifyBloomFilterRemoved(String name, T filter) {
        notifyListeners(l -> l.onBloomFilterRemoved(name, filter));
    }

    private void notifyListeners(Consumer<BloomFilterManagerListener<T, ExpirableBloomFilterConfig>> consumer) {
        for (BloomFilterManagerListener<T, ExpirableBloomFilterConfig> listener : listeners) {
            consumer.accept(listener);
        }
    }
}
