package cn.leancloud.filter.service;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ExpirableBloomFilterConfigTest {
    @Test
    public void testGetAndSetValidPeriodAfterWrite() {
        final int expectedValidPeriod = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThat(config.validPeriodAfterWrite()).isEqualTo(ExpirableBloomFilterConfig.DEFAULT_VALID_PERIOD);
        assertThat(config.setValidPeriodAfterWrite(expectedValidPeriod)).isSameAs(config);
        assertThat(config.validPeriodAfterWrite()).isEqualTo(Duration.ofSeconds(expectedValidPeriod));
    }

    @Test
    public void testSetInvalidValidPeriodAfterWrite() {
        final int invalidValidPeriod = -1 * Math.abs(ThreadLocalRandom.current().nextInt());
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final Duration old = config.validPeriodAfterWrite();
        assertThatThrownBy(() -> config.setValidPeriodAfterWrite(invalidValidPeriod))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
        assertThat(config.validPeriodAfterWrite()).isSameAs(old);
    }

    @Test
    public void testDefaultValidPeriodAfterAccess() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThat(config.validPeriodAfterWrite()).isEqualTo(ExpirableBloomFilterConfig.DEFAULT_VALID_PERIOD);
        assertThat(config.validPeriodAfterAccess()).isNull();
    }

    @Test
    public void testGetAndSetValidPeriodAfterAccess() {
        final int expectedValidPeriodAfterAccess = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();

        assertThat(config.setValidPeriodAfterAccess(expectedValidPeriodAfterAccess)).isSameAs(config);
        assertThat(config.validPeriodAfterAccess()).isEqualTo(Duration.ofSeconds(expectedValidPeriodAfterAccess));
    }

    @Test
    public void testSetInvalidValidPeriodAfterAccess() {
        final int invalidValidPeriod = -1 * Math.abs(ThreadLocalRandom.current().nextInt());
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThatThrownBy(() -> config.setValidPeriodAfterAccess(invalidValidPeriod))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
        assertThat(config.validPeriodAfterAccess()).isNull();
    }

    @Test
    public void testSetZeroValidPeriod() {
        final int zeroValidPeriod = 0;
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final Duration old = config.validPeriodAfterWrite();
        config.setValidPeriodAfterWrite(zeroValidPeriod);
        assertThat(config.validPeriodAfterWrite()).isEqualTo(old);
    }

    @Test
    public void testEquals() {
        final int validPeriod = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        final double fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final int expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig filterA = new ExpirableBloomFilterConfig()
                .setValidPeriodAfterWrite(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final ExpirableBloomFilterConfig filterB = new ExpirableBloomFilterConfig()
                .setValidPeriodAfterWrite(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA).isEqualTo(filterB);
    }

    @Test
    public void testHashCode() {
        final int validPeriod = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        final double fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final int expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig filterA = new ExpirableBloomFilterConfig()
                .setValidPeriodAfterWrite(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final ExpirableBloomFilterConfig filterB = new ExpirableBloomFilterConfig()
                .setValidPeriodAfterWrite(validPeriod)
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA.hashCode()).isEqualTo(filterB.hashCode());
    }
}