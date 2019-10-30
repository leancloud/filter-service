package cn.leancloud.filter.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.leancloud.filter.service.TestingUtils.numberString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BloomFilterManagerImplTest {
    private static final GuavaBloomFilterFactory factory = new GuavaBloomFilterFactory();
    private static final String testingFilterName = "TestingFilterName";

    private BloomFilterManagerImpl<GuavaBloomFilter> manager;

    @Before
    public void setUp() {
        manager = new BloomFilterManagerImpl<>(factory);
    }

    @Test
    public void testCreateNonExistsFilter() {
        final var validPeriod = 1000;
        final var expectedInsertions = 10000;
        final var fpp = 0.0001;
        final var config = new ExpirableBloomFilterConfig(expectedInsertions, fpp, validPeriod);
        final var instantBeforeFilterCreate = Instant.now();
        final var result = manager.createFilter(testingFilterName, config);
        final var filter = result.getFilter();

        assertThat(result.isCreated()).isTrue();
        assertThat(filter.fpp()).isEqualTo(fpp);
        assertThat(filter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(filter.expiration()).isEqualTo(filter.created().plus(Duration.ofSeconds(validPeriod)));
        assertThat(filter.created()).isAfter(instantBeforeFilterCreate);
        assertThat(filter.expired()).isFalse();
    }

    @Test
    public void testCreateExistsFilter() {
        final var config = new ExpirableBloomFilterConfig();
        final var firstCreateFilterResult = manager.createFilter(testingFilterName, config);
        final var existsFilter = firstCreateFilterResult.getFilter();
        assertThat(firstCreateFilterResult.isCreated()).isTrue();

        final var secondCreateFilterResult = manager.createFilter(testingFilterName, config);
        final var filter = secondCreateFilterResult.getFilter();
        assertThat(secondCreateFilterResult.isCreated()).isFalse();
        assertThat(filter).isSameAs(existsFilter);
    }

    @Test
    public void testOverwriteExistsFilter() {
        final var config = new ExpirableBloomFilterConfig();
        final var firstCreateFilterResult = manager.createFilter(testingFilterName, config);
        final var existsFilter = firstCreateFilterResult.getFilter();
        assertThat(firstCreateFilterResult.isCreated()).isTrue();

        final var secondCreateFilterResult = manager.createFilter(testingFilterName, new ExpirableBloomFilterConfig(), true);
        final var filter = secondCreateFilterResult.getFilter();
        assertThat(secondCreateFilterResult.isCreated()).isTrue();
        assertThat(filter).isNotSameAs(existsFilter);
    }

    @Test
    public void testGetExistsFilter() {
        final var config = new ExpirableBloomFilterConfig();
        final var filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
    }

    @Test
    public void testGetNonExistsFilter() {
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testSafeGetExistsFilter() throws Exception {
        final var config = new ExpirableBloomFilterConfig();
        final var filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.safeGetFilter(testingFilterName)).isSameAs(filter);
    }

    @Test
    public void testSafeGetNonExistsFilter() {
        assertThatThrownBy(() -> manager.safeGetFilter(testingFilterName))
                .isInstanceOf(FilterNotFoundException.class);
    }

    @Test
    public void testZeroSizeManager() {
        assertThat(manager.size()).isZero();
    }

    @Test
    public void testSize() {
        final var expectSize = ThreadLocalRandom.current().nextInt(1, 1000);
        final var config = new ExpirableBloomFilterConfig();
        for (int i = 0; i < expectSize; i++) {
            manager.createFilter(numberString(i), config);
        }

        assertThat(manager.size()).isEqualTo(expectSize);
    }

    @Test
    public void getAllFilterNames() {
        final var expectFilterNames = IntStream.range(1, 100).mapToObj(TestingUtils::numberString).collect(Collectors.toList());

        for (final var name : expectFilterNames) {
            final var config = new ExpirableBloomFilterConfig();
            manager.createFilter(name, config);
        }

        assertThat(manager.getAllFilterNames()).containsExactlyInAnyOrderElementsOf(expectFilterNames);
    }

    @Test
    public void testRemove() {
        final var config = new ExpirableBloomFilterConfig();
        final var filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
        manager.remove(testingFilterName);
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testPurge() {
        final var config = new ExpirableBloomFilterConfig();
        final var creationTime = Instant.now().minus(Duration.ofSeconds(10));
        final var mockedFactory = Mockito.mock(GuavaBloomFilterFactory.class);

        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(
                        BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS,
                        BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        creationTime,
                        Duration.ofSeconds(5)));

        final var manager = new BloomFilterManagerImpl<>(mockedFactory);
        final var filter = manager.createFilter(testingFilterName, config).getFilter();

        assertThat(filter.expired()).isTrue();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);

        manager.purge();

        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testListenFilterCreated() {
        final var listener = new TestingListener();

        manager.addListener(listener);

        final var config = new ExpirableBloomFilterConfig();
        final var filter = manager.createFilter(testingFilterName, config).getFilter();
        final var events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(1);
        final var receivedEvent = ((FilterCreatedEvent) events.get(0));
        assertThat(receivedEvent.getName()).isSameAs(testingFilterName);
        assertThat(receivedEvent.getConfig()).isSameAs(config);
        assertThat(receivedEvent.getFilter()).isSameAs(filter);
    }

    @Test
    public void testListenFilterCreatedNotTriggeredWhenNoFilterCreated() {
        final var config = new ExpirableBloomFilterConfig();
        manager.createFilter(testingFilterName, config);

        final var listener = new TestingListener();
        manager.addListener(listener);
        manager.createFilter(testingFilterName, config);

        final var events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(0);
    }

    @Test
    public void testListenFilterRemoved() {
        final var listener = new TestingListener();

        manager.addListener(listener);

        final var config = new ExpirableBloomFilterConfig();
        final var filter = manager.createFilter(testingFilterName, config).getFilter();
        manager.remove(testingFilterName);

        final var events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        assertThat(((FilterRemovedEvent) events.get(1)).getFilter()).isSameAs(filter);
        assertThat((events.get(1)).getName()).isSameAs(testingFilterName);
    }

    @Test
    public void testListenFilterPurged() {
        final var listener = new TestingListener();
        final var config = new ExpirableBloomFilterConfig();
        final var creationTime = Instant.now().minus(Duration.ofSeconds(10));
        final var mockedFactory = Mockito.mock(GuavaBloomFilterFactory.class);

        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(
                        BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS,
                        BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        creationTime,
                        Duration.ofSeconds(5)));

        final var manager = new BloomFilterManagerImpl<>(mockedFactory);
        manager.addListener(listener);

        final var filter = manager.createFilter(testingFilterName, config).getFilter();
        manager.purge();

        final var events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        assertThat(((FilterRemovedEvent) events.get(1)).getFilter()).isSameAs(filter);
        assertThat((events.get(1)).getName()).isSameAs(testingFilterName);
    }

    @Test
    public void removeListener() {
        final var listener = new TestingListener();

        manager.addListener(listener);
        manager.removeListener(listener);

        final var config = new ExpirableBloomFilterConfig();
        manager.createFilter(testingFilterName, config);

        List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isZero();
    }

    private static class TestingListener implements BloomFilterManagerListener<GuavaBloomFilter, ExpirableBloomFilterConfig> {
        private final AtomicInteger onFilterCreatedCalled = new AtomicInteger();
        private final AtomicInteger onFilterRemovedCalled = new AtomicInteger();
        private final List<FilterEvent> receivedEvents = new ArrayList<>();


        @Override
        public void onBloomFilterCreated(String name, ExpirableBloomFilterConfig config, GuavaBloomFilter filter) {
            onFilterCreatedCalled.incrementAndGet();
            receivedEvents.add(new FilterCreatedEvent(name, config, filter));
        }

        @Override
        public void onBloomFilterRemoved(String name, GuavaBloomFilter filter) {
            onFilterRemovedCalled.incrementAndGet();
            receivedEvents.add(new FilterRemovedEvent(name, filter));
        }

        List<FilterEvent> getReceivedEvents() {
            return receivedEvents;
        }
    }

    private static abstract class FilterEvent {
        private final String name;

        FilterEvent(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }

    private static class FilterCreatedEvent extends FilterEvent {

        private final ExpirableBloomFilterConfig config;
        private final GuavaBloomFilter filter;

        FilterCreatedEvent(String name, ExpirableBloomFilterConfig config, GuavaBloomFilter filter) {
            super(name);
            this.config = config;
            this.filter = filter;
        }

        ExpirableBloomFilterConfig getConfig() {
            return config;
        }

        GuavaBloomFilter getFilter() {
            return filter;
        }
    }

    private static class FilterRemovedEvent extends FilterEvent {
        private final GuavaBloomFilter filter;

        FilterRemovedEvent(String name, GuavaBloomFilter filter) {
            super(name);
            this.filter = filter;
        }

        GuavaBloomFilter getFilter() {
            return filter;
        }
    }
}