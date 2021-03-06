package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class BloomFilterManagerImpl<T extends BloomFilter, C extends BloomFilterConfig<? extends C>>
        implements BloomFilterManager<T, C>,
        Listenable<BloomFilterManagerListener<? super T, C>> {
    private static final FilterNotFoundException FILTER_NOT_FOUND_EXCEPTION = new FilterNotFoundException();

    private final List<BloomFilterManagerListener<? super T, C>> listeners;
    private final ConcurrentHashMap<String, T> filterMap;
    private final BloomFilterFactory<? extends T, ? super C> factory;
    private final Lock filterMapLock = new ReentrantLock();

    BloomFilterManagerImpl(BloomFilterFactory<? extends T, ? super C> factory) {
        this.filterMap = new ConcurrentHashMap<>();
        this.factory = factory;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public CreateFilterResult<T> createFilter(String name, C config, boolean overwrite) {
        T filter;
        T prevFilter;
        boolean created = false;
        filterMapLock.lock();
        try {
            prevFilter = filterMap.get(name);
            if (overwrite || prevFilter == null || !prevFilter.valid()) {
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
    public void addFilters(Iterable<FilterRecord<? extends T>> filters) {
        filterMapLock.lock();
        try {
            for (FilterRecord<? extends T> holder : filters) {
                filterMap.put(holder.name(), holder.filter());
            }
        } finally {
            filterMapLock.unlock();
        }
    }

    @Nullable
    @Override
    public T getFilter(String name) {
        return filterMap.get(name);
    }

    @Override
    public T ensureGetValidFilter(String name) throws FilterNotFoundException {
        final T filter = getFilter(name);
        if (filter == null || !filter.valid()) {
            throw FILTER_NOT_FOUND_EXCEPTION;
        }
        return filter;
    }

    @Override
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
    public void remove(String name, T filter) {
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

    @Override
    public Iterator<FilterRecord<T>> iterator() {
        return filterMap.entrySet().stream().map(e -> new FilterRecord<>(e.getKey(), e.getValue())).iterator();
    }

    @Override
    public void addListener(BloomFilterManagerListener<? super T, C> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(BloomFilterManagerListener<? super T, C> listener) {
        return listeners.remove(listener);
    }

    private void notifyBloomFilterCreated(String name, C config, T filter) {
        notifyListeners(l -> l.onBloomFilterCreated(name, config, filter));
    }

    private void notifyBloomFilterRemoved(String name, T filter) {
        notifyListeners(l -> l.onBloomFilterRemoved(name, filter));
    }

    private void notifyListeners(Consumer<BloomFilterManagerListener<? super T, C>> consumer) {
        for (BloomFilterManagerListener<? super T, C> listener : listeners) {
            consumer.accept(listener);
        }
    }
}
