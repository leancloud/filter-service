package cn.leancloud.filter.service;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AbstractBloomFilterConfigTest {
    @Test
    public void testGetAndSetExpectedInsertions() {
        final int expectedEInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final TestingBloomFilterConfig config = new TestingBloomFilterConfig();
        assertThat(config.expectedInsertions()).isEqualTo(BloomFilterConfig.DEFAULT_EXPECTED_INSERTIONS);
        assertThat(config.setExpectedInsertions(expectedEInsertions)).isSameAs(config);
        assertThat(config.expectedInsertions()).isEqualTo(expectedEInsertions);
    }

    @Test
    public void testGetAndSetFpp() {
        final double expectedFpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final TestingBloomFilterConfig config = new TestingBloomFilterConfig();
        assertThat(config.fpp()).isEqualTo(BloomFilterConfig.DEFAULT_FALSE_POSITIVE_PROBABILITY);
        assertThat(config.setFpp(expectedFpp)).isSameAs(config);
        assertThat(config.fpp()).isEqualTo(expectedFpp);
    }

    @Test
    public void testGetAndSetInvalidExpectedInsertions() {
        final int invalidExpectedInsertions = -1 * Math.abs(ThreadLocalRandom.current().nextInt());
        final TestingBloomFilterConfig config = new TestingBloomFilterConfig();

        assertThatThrownBy(() -> config.setExpectedInsertions(invalidExpectedInsertions))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testGetAndSetInvalidFpp() {
        final double invalidFpp = ThreadLocalRandom.current().nextDouble(1, Long.MAX_VALUE);
        final TestingBloomFilterConfig config = new TestingBloomFilterConfig();

        assertThatThrownBy(() -> config.setFpp(invalidFpp))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testEquals() {
        final double fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final int expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final TestingBloomFilterConfig filterA = new TestingBloomFilterConfig()
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final TestingBloomFilterConfig filterB = new TestingBloomFilterConfig()
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA).isEqualTo(filterB);
    }

    @Test
    public void testHashCode() {
        final double fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final int expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final TestingBloomFilterConfig filterA = new TestingBloomFilterConfig()
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final TestingBloomFilterConfig filterB = new TestingBloomFilterConfig()
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA.hashCode()).isEqualTo(filterB.hashCode());
    }

    private static class TestingBloomFilterConfig extends AbstractBloomFilterConfig<TestingBloomFilterConfig> {

    }
}