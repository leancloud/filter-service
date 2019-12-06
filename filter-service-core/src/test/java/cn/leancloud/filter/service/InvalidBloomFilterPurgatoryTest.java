package cn.leancloud.filter.service;

import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class InvalidBloomFilterPurgatoryTest {
    private static final String testingFilterName = "TestingFilterName";
    @Test
    public void testPurge() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final ZonedDateTime creationTime = ZonedDateTime.now(ZoneOffset.UTC).minus(Duration.ofSeconds(10));
        final ZonedDateTime expirationTime = creationTime.plusSeconds(5);
        final GuavaBloomFilterFactory mockedFactory = Mockito.mock(GuavaBloomFilterFactory.class);

        Mockito.when(mockedFactory.createFilter(config))
                .thenReturn(new GuavaBloomFilter(
                        Configuration.defaultExpectedInsertions(),
                        Configuration.defaultFalsePositiveProbability(),
                        creationTime,
                        expirationTime,
                        null));

        final BloomFilterManagerImpl<GuavaBloomFilter, ExpirableBloomFilterConfig> manager = new BloomFilterManagerImpl<>(mockedFactory);
        final GuavaBloomFilter filter = manager.createFilter(testingFilterName, config).getFilter();
        final InvalidBloomFilterPurgatory<GuavaBloomFilter> purgatory =
                new InvalidBloomFilterPurgatory<>(manager);

        assertThat(filter.expired()).isTrue();
        assertThat(manager.getFilter(testingFilterName)).isSameAs(filter);

        purgatory.purge();

        assertThat(manager.getFilter(testingFilterName)).isNull();
    }
}