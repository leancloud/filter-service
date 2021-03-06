package cn.leancloud.filter.service;

import cn.leancloud.filter.service.BloomFilterManager.CreateFilterResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    private BloomFilterManagerImpl<BloomFilter, ExpirableBloomFilterConfig> manager;

    @Before
    public void setUp() {
        manager = new BloomFilterManagerImpl<>(factory);
    }

    @Test
    public void testCreateNonExistsFilter() {
        final int validPeriod = 1000;
        final int expectedInsertions = 10000;
        final double fpp = 0.0001;
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig(expectedInsertions, fpp);
        config.setValidPeriodAfterCreate(Duration.ofSeconds(validPeriod));
        final CreateFilterResult<BloomFilter> result = manager.createFilter(testingFilterName, config);
        final BloomFilter filter = result.getFilter();

        assertThat(result.isCreated()).isTrue();
        assertThat(filter.fpp()).isEqualTo(fpp);
        assertThat(filter.expectedInsertions()).isEqualTo(expectedInsertions);
    }

    @Test
    public void testCreateExistsFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final CreateFilterResult<BloomFilter> firstCreateFilterResult = manager.createFilter(testingFilterName, config);
        final BloomFilter existsFilter = firstCreateFilterResult.getFilter();
        assertThat(firstCreateFilterResult.isCreated()).isTrue();

        final CreateFilterResult<BloomFilter> secondCreateFilterResult = manager.createFilter(testingFilterName, config);
        final BloomFilter filter = secondCreateFilterResult.getFilter();
        assertThat(secondCreateFilterResult.isCreated()).isFalse();
        assertThat(filter).isSameAs(existsFilter);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOverwriteExpiredFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final ZonedDateTime created = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = created.minus(Duration.ofSeconds(10));
        // a filter factory only to produce expired bloom filters
        final BloomFilterFactory<GuavaBloomFilter, ExpirableBloomFilterConfig> mockedFactory = Mockito.mock(BloomFilterFactory.class);
        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(
                        config.expectedInsertions(),
                        config.fpp(),
                        created,
                        expiration,
                        null));

        final BloomFilterManagerImpl<GuavaBloomFilter, ExpirableBloomFilterConfig> manager = new BloomFilterManagerImpl<>(mockedFactory);
        final CreateFilterResult<GuavaBloomFilter> firstCreateFilterResult = manager.createFilter(testingFilterName, config);
        assertThat(firstCreateFilterResult.isCreated()).isTrue();

        // overwrite mark is not set, but the exists filter still was overwritten
        final CreateFilterResult<GuavaBloomFilter> secondCreateFilterResult = manager.createFilter(testingFilterName, config);
        assertThat(secondCreateFilterResult.isCreated()).isTrue();
        Mockito.verify(mockedFactory, Mockito.times(2)).createFilter(config);
    }

    @Test
    public void testOverwriteExistsFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final CreateFilterResult<BloomFilter> firstCreateFilterResult = manager.createFilter(testingFilterName, config);
        final BloomFilter existsFilter = firstCreateFilterResult.getFilter();
        assertThat(firstCreateFilterResult.isCreated()).isTrue();

        final CreateFilterResult<BloomFilter> secondCreateFilterResult = manager.createFilter(testingFilterName, config, true);
        final BloomFilter filter = secondCreateFilterResult.getFilter();
        assertThat(secondCreateFilterResult.isCreated()).isTrue();
        assertThat(filter).isNotSameAs(existsFilter);
    }

    @Test
    public void testGetExistsFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final BloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
    }

    @Test
    public void testGetNonExistsFilter() {
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testGetInvalidFilter() {
        final BloomFilter filter = TestingUtils.generateInvalidFilter();
        manager.addFilters(Collections.singletonList(new FilterRecord<>(testingFilterName, filter)));
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
    }

    @Test
    public void testSafeGetExistsFilter() throws Exception {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final BloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.ensureGetValidFilter(testingFilterName)).isSameAs(filter);
    }

    @Test
    public void testSafeGetNonExistsFilter() {
        assertThatThrownBy(() -> manager.ensureGetValidFilter(testingFilterName))
                .isInstanceOf(FilterNotFoundException.class);
    }

    @Test
    public void testSafeGetInvalidFilter() {
        final BloomFilter filter = TestingUtils.generateInvalidFilter();
        manager.addFilters(Collections.singletonList(new FilterRecord<>(testingFilterName, filter)));
        assertThatThrownBy(() -> manager.ensureGetValidFilter(testingFilterName))
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
        final BloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);
        manager.remove(testingFilterName);
        assertThat(manager.getFilter(testingFilterName)).isNull();
    }

    @Test
    public void testListenFilterCreated() {
        final TestingListener listener = new TestingListener();

        manager.addListener(listener);

        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final BloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        final List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(1);
        final FilterCreatedEvent receivedEvent = ((FilterCreatedEvent) events.get(0));
        assertThat(receivedEvent.getName()).isSameAs(testingFilterName);
        assertThat(receivedEvent.getConfig()).isSameAs(config);
        assertThat(receivedEvent.getFilter()).isSameAs(filter);
    }

    @Test
    public void testListenFilterOverwrited() {
        final TestingListener listener = new TestingListener();

        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final BloomFilter prevFilter = manager.createFilter(testingFilterName, config).getFilter();
        manager.addListener(listener);
        final BloomFilter newFilter = manager.createFilter(testingFilterName, config, true).getFilter();
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

    @SuppressWarnings("unchecked")
    @Test
    public void testListenOverwriteExpiredFilter() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final TestingListener listener = new TestingListener();
        final ZonedDateTime created = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = created.minus(Duration.ofSeconds(10));
        // a filter factory only to produce expired bloom filters
        final BloomFilterFactory<GuavaBloomFilter, ExpirableBloomFilterConfig> expiredFilterFactory = Mockito.mock(BloomFilterFactory.class);
        Mockito.when(expiredFilterFactory.createFilter(config))
                .thenAnswer(invocation ->
                        new GuavaBloomFilter(
                                config.expectedInsertions(),
                                config.fpp(),
                                created,
                                expiration,
                                null));

        final BloomFilterManagerImpl<GuavaBloomFilter, ExpirableBloomFilterConfig> manager = new BloomFilterManagerImpl<>(expiredFilterFactory);
        final GuavaBloomFilter prevFilter = manager.createFilter(testingFilterName, config).getFilter();

        manager.addListener(listener);

        // overwrite mark is not set, but the exists filter still was overwritten
        final GuavaBloomFilter newFilter = manager.createFilter(testingFilterName, config).getFilter();

        final List<FilterEvent> events = listener.getReceivedEvents();
        assertThat(events.size()).isEqualTo(2);
        final FilterRemovedEvent prevFilterRemovedEvent = ((FilterRemovedEvent) events.get(0));
        assertThat(prevFilterRemovedEvent.getName()).isSameAs(testingFilterName);
        assertThat(prevFilterRemovedEvent.getFilter()).isSameAs(prevFilter);

        final FilterCreatedEvent newFilterCreatedEvent = ((FilterCreatedEvent) events.get(1));
        assertThat(newFilterCreatedEvent.getName()).isSameAs(testingFilterName);
        assertThat(newFilterCreatedEvent.getConfig()).isSameAs(config);
        assertThat(newFilterCreatedEvent.getFilter()).isSameAs(newFilter);
        Mockito.verify(expiredFilterFactory, Mockito.times(2)).createFilter(config);
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
        final BloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        manager.remove(testingFilterName);

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

    private static class TestingListener implements BloomFilterManagerListener<BloomFilter, ExpirableBloomFilterConfig> {
        private final AtomicInteger onFilterCreatedCalled = new AtomicInteger();
        private final AtomicInteger onFilterRemovedCalled = new AtomicInteger();
        private final List<FilterEvent> receivedEvents = new ArrayList<>();


        @Override
        public void onBloomFilterCreated(String name, ExpirableBloomFilterConfig config, BloomFilter filter) {
            onFilterCreatedCalled.incrementAndGet();
            receivedEvents.add(new FilterCreatedEvent(name, config, filter));
        }

        @Override
        public void onBloomFilterRemoved(String name, BloomFilter filter) {
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
        private final BloomFilter filter;

        FilterCreatedEvent(String name, ExpirableBloomFilterConfig config, BloomFilter filter) {
            super(name);
            this.config = config;
            this.filter = filter;
        }

        ExpirableBloomFilterConfig getConfig() {
            return config;
        }

        BloomFilter getFilter() {
            return filter;
        }
    }

    private static class FilterRemovedEvent extends FilterEvent {
        private final BloomFilter filter;

        FilterRemovedEvent(String name, BloomFilter filter) {
            super(name);
            this.filter = filter;
        }

        BloomFilter getFilter() {
            return filter;
        }
    }
}