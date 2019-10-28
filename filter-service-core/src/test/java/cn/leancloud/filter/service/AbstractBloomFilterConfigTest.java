package cn.leancloud.filter.service;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AbstractBloomFilterConfigTest {
    private static final String testingFilterName = "TestingFilter";

    @Test
    public void testGetAndSetExpectedInsertions() {
        final var expectedEInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final var config = new TestingBloomFilterConfig(testingFilterName);
        assertThat(config.expectedInsertions()).isEqualTo(BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS);
        assertThat(config.setExpectedInsertions(expectedEInsertions)).isSameAs(config);
        assertThat(config.expectedInsertions()).isEqualTo(expectedEInsertions);
    }

    @Test
    public void testGetAndSetFpp() {
        final var expectedFpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        ;
        final var config = new TestingBloomFilterConfig(testingFilterName);
        assertThat(config.fpp()).isEqualTo(BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY);
        assertThat(config.setFpp(expectedFpp)).isSameAs(config);
        assertThat(config.fpp()).isEqualTo(expectedFpp);
    }

    @Test
    public void testGetAndSetInvalidExpectedInsertions() {
        final var invalidExpectedInsertions = -1 * Math.abs(ThreadLocalRandom.current().nextInt());
        final var config = new TestingBloomFilterConfig(testingFilterName);

        assertThatThrownBy(() -> config.setExpectedInsertions(invalidExpectedInsertions))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testGetAndSetInvalidFpp() {
        final var invalidFpp = ThreadLocalRandom.current().nextDouble(1, Long.MAX_VALUE);
        final var config = new TestingBloomFilterConfig(testingFilterName);

        assertThatThrownBy(() -> config.setFpp(invalidFpp))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testEquals() {
        final var fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final var expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final var filterA = new TestingBloomFilterConfig(testingFilterName)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final var filterB = new TestingBloomFilterConfig(testingFilterName)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA).isEqualTo(filterB);
    }

    @Test
    public void testHashCode() {
        final var fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final var expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final var filterA = new TestingBloomFilterConfig(testingFilterName)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final var filterB = new TestingBloomFilterConfig(testingFilterName)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA.hashCode()).isEqualTo(filterB.hashCode());
    }

    private static class TestingBloomFilterConfig extends AbstractBloomFilterConfig<TestingBloomFilterConfig> {
        TestingBloomFilterConfig(String name) {
            super(name);
        }
    }
}