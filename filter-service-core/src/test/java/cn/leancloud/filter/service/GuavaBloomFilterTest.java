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
        final var validPeriod = 1000;
        final var expectedInsertions = 1000000;
        final var fpp = 0.0001;
        final var config = new ExpirableBloomFilterConfig()
                .setValidPeriod(validPeriod)
                .setExpectedInsertions(expectedInsertions)
                .setFpp(fpp);
        final var instantBeforeFilterCreate = Instant.now();
        final var filter = testingFactory.createFilter(config);

        assertThat(filter.fpp()).isEqualTo(fpp);
        assertThat(filter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(filter.expiration()).isEqualTo(filter.created().plus(Duration.ofSeconds(validPeriod)));
        assertThat(filter.created()).isAfter(instantBeforeFilterCreate);
        assertThat(filter.expired()).isFalse();
    }

    @Test
    public void testMightContain() {
        final var testingValue = "SomeValue";
        final var filter = testingFactory.createFilter(defaultTestingConfig);
        assertThat(filter.mightContain(testingValue)).isFalse();
        assertThat(filter.set(testingValue)).isTrue();
        assertThat(filter.mightContain(testingValue)).isTrue();
    }

    @Test
    public void testToJson() throws Exception {
        final var expectedFilter = testingFactory.createFilter(defaultTestingConfig);
        final var mapper = new ObjectMapper();

        final var json = mapper.valueToTree(expectedFilter).toString();
        final var filter = new ObjectMapper().readerFor(GuavaBloomFilter.class).readValue(json);
        assertThat(filter).isEqualTo(expectedFilter);
    }
}