package cn.leancloud.filter.service;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ExpirableBloomFilterConfigTest {
    @Test
    public void testGetAndSetValidPeriodAfterCreate() {
        final int expectedValidPeriod = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThat(config.validPeriodAfterCreate()).isEqualTo(ExpirableBloomFilterConfig.DEFAULT_VALID_PERIOD);
        assertThat(config.setValidPeriodAfterCreate(Duration.ofSeconds(expectedValidPeriod))).isSameAs(config);
        assertThat(config.validPeriodAfterCreate()).isEqualTo(Duration.ofSeconds(expectedValidPeriod));
    }

    @Test
    public void testSetNegativeValidPeriodAfterCreate() {
        final int invalidValidPeriod = -1 * Math.abs(ThreadLocalRandom.current().nextInt());
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final Duration old = config.validPeriodAfterCreate();
        assertThatThrownBy(() -> config.setValidPeriodAfterCreate(Duration.ofSeconds(invalidValidPeriod)))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
        assertThat(config.validPeriodAfterCreate()).isSameAs(old);
    }

    @Test
    public void testSetZeroValidPeriodAfterCreate() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final Duration old = config.validPeriodAfterCreate();
        assertThatThrownBy(() -> config.setValidPeriodAfterCreate(Duration.ofSeconds(0)))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");

        assertThat(config.validPeriodAfterCreate()).isEqualTo(old);
    }

    @Test
    public void testDefaultValidPeriodAfterAccess() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThat(config.validPeriodAfterCreate()).isEqualTo(ExpirableBloomFilterConfig.DEFAULT_VALID_PERIOD);
        assertThat(config.extendValidPeriodAfterAccess()).isNull();
    }

    @Test
    public void testGetAndSetValidPeriodAfterAccess() {
        final int expectedValidPeriodAfterAccess = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();

        assertThat(config.setExtendValidPeriodAfterAccess(Duration.ofSeconds(expectedValidPeriodAfterAccess))).isSameAs(config);
        assertThat(config.extendValidPeriodAfterAccess()).isEqualTo(Duration.ofSeconds(expectedValidPeriodAfterAccess));
    }

    @Test
    public void testSetNegativeValidPeriodAfterAccess() {
        final int invalidValidPeriod = -1 * Math.abs(ThreadLocalRandom.current().nextInt());
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThatThrownBy(() -> config.setExtendValidPeriodAfterAccess(Duration.ofSeconds(invalidValidPeriod)))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
        assertThat(config.extendValidPeriodAfterAccess()).isNull();
    }

    @Test
    public void testSetZeroValidPeriodAfterAccess() {
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        assertThatThrownBy(() -> config.setExtendValidPeriodAfterAccess(Duration.ofSeconds(0)))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");

        assertThat(config.extendValidPeriodAfterAccess()).isNull();
    }

    @Test
    public void testEquals() {
        final int validPeriod = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        final double fpp = ThreadLocalRandom.current().nextDouble(0.0001, 1);
        final int expectedInsertions = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        final ExpirableBloomFilterConfig filterA = new ExpirableBloomFilterConfig()
                .setValidPeriodAfterCreate(Duration.ofSeconds(validPeriod))
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final ExpirableBloomFilterConfig filterB = new ExpirableBloomFilterConfig()
                .setValidPeriodAfterCreate(Duration.ofSeconds(validPeriod))
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
                .setValidPeriodAfterCreate(Duration.ofSeconds(validPeriod))
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        final ExpirableBloomFilterConfig filterB = new ExpirableBloomFilterConfig()
                .setValidPeriodAfterCreate(Duration.ofSeconds(validPeriod))
                .setFpp(fpp)
                .setExpectedInsertions(expectedInsertions);

        assertThat(filterA.hashCode()).isEqualTo(filterB.hashCode());
    }
}