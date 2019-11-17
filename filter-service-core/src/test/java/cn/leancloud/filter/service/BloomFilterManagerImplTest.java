package cn.leancloud.filter.service;

import cn.leancloud.filter.service.BloomFilterManager.CreateFilterResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
        final int validPeriod = 1000;
        final int expectedInsertions = 10000;
        final double fpp = 0.0001;
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig(expectedInsertions, fpp, validPeriod);
        final ZonedDateTime instantBeforeFilterCreate = ZonedDateTime.now(ZoneOffset.UTC);
        final CreateFilterResult<GuavaBloomFilter> result = manager.createFilter(testingFilterName, config);
        final GuavaBloomFilter filter = result.getFilter();

        assertThat(result.isCreated()).isTrue();
        assertThat(filter.fpp()).isEqualTo(fpp);
        assertThat(filter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(filter.expiration()).isEqualTo(filter.created().plus(Duration.ofSeconds(validPeriod)));
        assertThat(filter.created()).isAfter(instantBeforeFilterCreate);
        assertThat(filter.expired()).isFalse();
    }

    @Test
    public void testCreateExistsFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final CreateFilterResult<GuavaBloomFilter> firstCreateFilterResult = manager.createFilter(testingFilterName, config);
        final GuavaBloomFilter existsFilter = firstCreateFilterResult.getFilter();
        assertThat(firstCreateFilterResult.isCreated()).isTrue();

        final CreateFilterResult<GuavaBloomFilter> secondCreateFilterResult = manager.createFilter(testingFilterName, config);
        final GuavaBloomFilter filter = secondCreateFilterResult.getFilter();
        assertThat(secondCreateFilterResult.isCreated()).isFalse();
        assertThat(filter).isSameAs(existsFilter);
    }

    @Test
    public void testOverwriteExistsFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final CreateFilterResult<GuavaBloomFilter> firstCreateFilterResult = manager.createFilter(testingFilterName, config);
        final GuavaBloomFilter existsFilter = firstCreateFilterResult.getFilter();
        assertThat(firstCreateFilterResult.isCreated()).isTrue();

        final CreateFilterResult<GuavaBloomFilter> secondCreateFilterResult = manager.createFilter(testingFilterName, new ExpirableBloomFilterConfig(), true);
        final GuavaBloomFilter filter = secondCreateFilterResult.getFilter();
        assertThat(secondCreateFilterResult.isCreated()).isTrue();
        assertThat(filter).isNotSameAs(existsFilter);
    }

    @Test
    public void testGetExistsFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
    }

    @Test
    public void testGetNonExistsFilter() {
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testSafeGetExistsFilter() throws Exception {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
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
        final int expectSize = ThreadLocalRandom.current().nextInt(1, 100);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        for (int i = 0; i < expectSize; i++) {
            manager.createFilter(numberString(i), config);
        }

        assertThat(manager.size()).isEqualTo(expectSize);
    }

    @Test
    public void getAllFilterNames() {
        final List<String> expectFilterNames = IntStream.range(1, 100).mapToObj(TestingUtils::numberString).collect(Collectors.toList());

        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        for (final String name : expectFilterNames) {
            manager.createFilter(name, config);
        }

        assertThat(manager.getAllFilterNames()).containsExactlyInAnyOrderElementsOf(expectFilterNames);
    }

    @Test
    public void testRemove() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
        manager.remove(testingFilterName);
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testPurge() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final ZonedDateTime creationTime = ZonedDateTime.now(ZoneOffset.UTC).minus(Duration.ofSeconds(10));
        final ZonedDateTime expirationTime = creationTime.plusSeconds(5);
        final GuavaBloomFilterFactory mockedFactory = Mockito.mock(GuavaBloomFilterFactory.class);

        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(
                        BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS,
                        BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        creationTime,
                        expirationTime));

        final BloomFilterManagerImpl<GuavaBloomFilter> manager = new BloomFilterManagerImpl<>(mockedFactory);
        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();

        assertThat(filter.expired()).isTrue();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);

        manager.purge();

        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testListenFilterCreated() {
        final TestingListener listener = new TestingListener();

        manager.addListener(listener);

        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        final List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(1);
        final FilterCreatedEvent receivedEvent = ((FilterCreatedEvent) events.get(0));
        assertThat(receivedEvent.getName()).isSameAs(testingFilterName);
        assertThat(receivedEvent.getConfig()).isSameAs(config);
        assertThat(receivedEvent.getFilter()).isSameAs(filter);
    }

    @Test
    public void testListenFilterOverwrite() {
        final TestingListener listener = new TestingListener();

        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final GuavaBloomFilter prevFilter = manager.createFilter(testingFilterName, config).getFilter();
        manager.addListener(listener);
        final GuavaBloomFilter newFilter = manager.createFilter(testingFilterName, config, true).getFilter();
        final List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        final FilterRemovedEvent prevFilterRemovedEvent = ((FilterRemovedEvent) events.get(0));
        assertThat(prevFilterRemovedEvent.getName()).isSameAs(testingFilterName);
        assertThat(prevFilterRemovedEvent.getFilter()).isSameAs(prevFilter);

        final FilterCreatedEvent newFilterCreatedEvent = ((FilterCreatedEvent) events.get(1));
        assertThat(newFilterCreatedEvent.getName()).isSameAs(testingFilterName);
        assertThat(newFilterCreatedEvent.getConfig()).isSameAs(config);
        assertThat(newFilterCreatedEvent.getFilter()).isSameAs(newFilter);
    }

    @Test
    public void testListenFilterCreatedNotTriggeredWhenNoFilterCreated() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        manager.createFilter(testingFilterName, config);

        final TestingListener listener = new TestingListener();
        manager.addListener(listener);
        manager.createFilter(testingFilterName, config);

        final List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(0);
    }

    @Test
    public void testListenFilterRemoved() {
        final TestingListener listener = new TestingListener();

        manager.addListener(listener);

        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        manager.remove(testingFilterName);

        final List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        assertThat(((FilterRemovedEvent) events.get(1)).getFilter()).isSameAs(filter);
        assertThat((events.get(1)).getName()).isSameAs(testingFilterName);
    }

    @Test
    public void testListenFilterPurged() {
        final TestingListener listener = new TestingListener();
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final ZonedDateTime creationTime = ZonedDateTime.now(ZoneOffset.UTC).minus(Duration.ofSeconds(10));
        final ZonedDateTime expirationTime = creationTime.plusSeconds(5);
        final GuavaBloomFilterFactory mockedFactory = Mockito.mock(GuavaBloomFilterFactory.class);

        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(
                        BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS,
                        BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        creationTime,
                        expirationTime));

        final BloomFilterManagerImpl<GuavaBloomFilter> manager = new BloomFilterManagerImpl<>(mockedFactory);
        manager.addListener(listener);

        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        manager.purge();

        final List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        assertThat(((FilterRemovedEvent) events.get(1)).getFilter()).isSameAs(filter);
        assertThat((events.get(1)).getName()).isSameAs(testingFilterName);
    }

    @Test
    public void removeListener() {
        final TestingListener listener = new TestingListener();

        manager.addListener(listener);
        manager.removeListener(listener);

        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
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