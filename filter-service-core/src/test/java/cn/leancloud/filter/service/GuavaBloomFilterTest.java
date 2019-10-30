package cn.leancloud.filter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class GuavaBloomFilterTest {
    private static final GuavaBloomFilterFactory testingFactory = new GuavaBloomFilterFactory();
    private static final ExpirableBloomFilterConfig defaultTestingConfig = new ExpirableBloomFilterConfig();

    @Test
    public void testGetters() {
        final int validPeriod = 1000;
        final int expectedInsertions = 1000000;
        final double fpp = 0.0001;
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig()
                .setValidPeriod(validPeriod)
                .setExpectedInsertions(expectedInsertions)
                .setFpp(fpp);
        final Instant instantBeforeFilterCreate = Instant.now();
        final GuavaBloomFilter filter = testingFactory.createFilter(config);

        assertThat(filter.fpp()).isEqualTo(fpp);
        assertThat(filter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(filter.expiration()).isEqualTo(filter.created().plus(Duration.ofSeconds(validPeriod)));
        assertThat(filter.created()).isAfter(instantBeforeFilterCreate);
        assertThat(filter.expired()).isFalse();
    }

    @Test
    public void testMightContain() {
        final String testingValue = "SomeValue";
        final GuavaBloomFilter filter = testingFactory.createFilter(defaultTestingConfig);
        assertThat(filter.mightContain(testingValue)).isFalse();
        assertThat(filter.set(testingValue)).isTrue();
        assertThat(filter.mightContain(testingValue)).isTrue();
    }

    @Test
    public void testToJson() throws Exception {
        final GuavaBloomFilter expectedFilter = testingFactory.createFilter(defaultTestingConfig);
        final ObjectMapper mapper = new ObjectMapper();

        final String json = mapper.valueToTree(expectedFilter).toString();
        final GuavaBloomFilter filter = new ObjectMapper().readerFor(GuavaBloomFilter.class).readValue(json);
        assertThat(filter).isEqualTo(expectedFilter);
    }
}