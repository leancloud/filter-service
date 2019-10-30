package cn.leancloud.filter.service;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ExpirableBloomFilterConfigTest {
    @Test
    public void testGetAndSetValidPeriod() {
        final long expectedValidPeriod = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThat(config.validPeriod()).isEqualTo(ExpirableBloomFilterConfig.DEFAULT_VALID_PERIOD);
        assertThat(config.setValidPeriod(expectedValidPeriod)).isSameAs(config);
        assertThat(config.validPeriod()).isEqualTo(Duration.ofSeconds(expectedValidPeriod));
    }

    @Test
    public void testSetInvalidValidPeriod() {
        final long invalidValidPeriod = -1 * Math.abs(ThreadLocalRandom.current().nextLong());
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final Duration old = config.validPeriod();
        assertThatThrownBy(() -> config.setValidPeriod(invalidValidPeriod))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
        assertThat(config.validPeriod()).isSameAs(old);
    }

    @Test
    public void testSetZeroValidPeriod() {
        final int zeroValidPeriod = 0;
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final Duration old = config.validPeriod();
        config.setValidPeriod(zeroValidPeriod);
        assertThat(config.validPeriod()).isEqualTo(old);
    }

    @Test
    public void testEquals() {
        final long validPeriod = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        final double fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final int expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig filterA = new ExpirableBloomFilterConfig()
                .setValidPeriod(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final ExpirableBloomFilterConfig filterB = new ExpirableBloomFilterConfig()
                .setValidPeriod(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA).isEqualTo(filterB);
    }

    @Test
    public void testHashCode() {
        final long validPeriod = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        final double fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final int expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig filterA = new ExpirableBloomFilterConfig()
                .setValidPeriod(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final ExpirableBloomFilterConfig filterB = new ExpirableBloomFilterConfig()
                .setValidPeriod(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA.hashCode()).isEqualTo(filterB.hashCode());
    }
}