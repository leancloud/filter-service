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
    public void testCreateFilter() {
        final var validPeriod = 1000;
        final var expectedInsertions = 1000000;
        final var fpp = 0.0001;
        final var config = new ExpirableBloomFilterConfig(testingFilterName)
                .setValidPeriod(validPeriod)
                .setExpectedInsertions(expectedInsertions)
                .setFpp(fpp);
        final var instantBeforeFilterCreate = Instant.now();
        final var filter = manager.createFilter(config);

        assertThat(filter.fpp()).isEqualTo(fpp);
        assertThat(filter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(filter.name()).isEqualTo(testingFilterName);
        assertThat(filter.expiration()).isEqualTo(filter.created().plus(Duration.ofSeconds(validPeriod)));
        assertThat(filter.created()).isAfter(instantBeforeFilterCreate);
        assertThat(filter.expired()).isFalse();
    }

    @Test
    public void testGetExistsFilter() {
        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        final var filter = manager.createFilter(config);
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
    }

    @Test
    public void testGetNonExistsFilter() {
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testSafeGetExistsFilter() throws Exception {
        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        final var filter = manager.createFilter(config);
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
        for (int i = 0; i < expectSize; i++) {
            final var config = new ExpirableBloomFilterConfig(numberString(i));
            manager.createFilter(config);
        }

        assertThat(manager.size()).isEqualTo(expectSize);
    }

    @Test
    public void getAllFilterNames() {
        final var expectFilterNames = IntStream.range(1, 1000).mapToObj(TestingUtils::numberString).collect(Collectors.toList());

        for (final var name : expectFilterNames) {
            final var config = new ExpirableBloomFilterConfig(name);
            manager.createFilter(config);
        }

        assertThat(manager.getAllFilterNames()).containsExactlyInAnyOrderElementsOf(expectFilterNames);
    }

    @Test
    public void testRemove() {
        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        final var filter = manager.createFilter(config);
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
        manager.remove(testingFilterName);
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testPurge() {
        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        final var creationTime = Instant.now().minus(Duration.ofSeconds(10));
        final var mockedFactory = Mockito.mock(GuavaBloomFilterFactory.class);

        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(testingFilterName,
                        BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS,
                        BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        creationTime,
                        Duration.ofSeconds(5)));

        final var manager = new BloomFilterManagerImpl<>(mockedFactory);
        final var filter = manager.createFilter(config);

        assertThat(filter.expired()).isTrue();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);

        manager.purge();

        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testListenFilterCreated() {
        final var listener = new TestingListener();

        manager.addListener(listener);

        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        final var filter = manager.createFilter(config);
        List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(1);
        assertThat(((FilterCreatedEvent) events.get(0)).getConfig()).isSameAs(config);
        assertThat(((FilterCreatedEvent) events.get(0)).getFilter()).isSameAs(filter);
    }

    @Test
    public void testListenFilterRemoved() {
        final var listener = new TestingListener();

        manager.addListener(listener);

        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        final var filter = manager.createFilter(config);
        manager.remove(testingFilterName);

        List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        assertThat(((FilterRemovedEvent) events.get(1)).getFilter()).isSameAs(filter);
    }

    @Test
    public void testListenFilterPurged() {
        final var listener = new TestingListener();
        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        final var creationTime = Instant.now().minus(Duration.ofSeconds(10));
        final var mockedFactory = Mockito.mock(GuavaBloomFilterFactory.class);

        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(testingFilterName,
                        BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS,
                        BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        creationTime,
                        Duration.ofSeconds(5)));

        final var manager = new BloomFilterManagerImpl<>(mockedFactory);
        manager.addListener(listener);

        final var filter = manager.createFilter(config);
        manager.purge();

        List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        assertThat(((FilterRemovedEvent) events.get(1)).getFilter()).isSameAs(filter);
    }

    @Test
    public void removeListener() {
        final var listener = new TestingListener();

        manager.addListener(listener);
        manager.removeListener(listener);

        final var config = new ExpirableBloomFilterConfig(testingFilterName);
        manager.createFilter(config);

        List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isZero();
    }

    private static class TestingListener implements BloomFilterManagerListener<GuavaBloomFilter, ExpirableBloomFilterConfig> {
        private final AtomicInteger onFilterCreatedCalled = new AtomicInteger();
        private final AtomicInteger onFilterRemovedCalled = new AtomicInteger();
        private final List<FilterEvent> receivedEvents = new ArrayList<>();


        @Override

        public void onBloomFilterCreated(ExpirableBloomFilterConfig config, GuavaBloomFilter filter) {
            onFilterCreatedCalled.incrementAndGet();
            receivedEvents.add(new FilterCreatedEvent(config, filter));
        }

        @Override
        public void onBloomFilterRemoved(GuavaBloomFilter filter) {
            onFilterRemovedCalled.incrementAndGet();
            receivedEvents.add(new FilterRemovedEvent(filter));
        }

        List<FilterEvent> getReceivedEvents() {
            return receivedEvents;
        }
    }

    private interface FilterEvent {}

    private static class FilterCreatedEvent implements FilterEvent {
        private final ExpirableBloomFilterConfig config;
        private final GuavaBloomFilter filter;

        FilterCreatedEvent(ExpirableBloomFilterConfig config, GuavaBloomFilter filter) {
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

    private static class FilterRemovedEvent implements FilterEvent {
        private final GuavaBloomFilter filter;

        FilterRemovedEvent(GuavaBloomFilter filter) {
            this.filter = filter;
        }

        GuavaBloomFilter getFilter() {
            return filter;
        }
    }
}