package cn.leancloud.filter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class GuavaBloomFilterTest {
    private static final GuavaBloomFilterFactory testingFactory = new GuavaBloomFilterFactory();
    private static final ExpirableBloomFilterConfig defaultTestingConfig = new ExpirableBloomFilterConfig();

    @Test
    public void testGetters() {
        final Duration validPeriodAfterAccess = Duration.ofSeconds(3);
        final int expectedInsertions = 1000000;
        final double fpp = 0.0001;
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.plus(Duration.ofSeconds(10));
        final GuavaBloomFilter filter = new GuavaBloomFilter(
                expectedInsertions,
                fpp,
                creation,
                expiration,
                validPeriodAfterAccess);

        assertThat(filter.fpp()).isEqualTo(fpp);
        assertThat(filter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(filter.expiration()).isEqualTo(expiration);
        assertThat(filter.validPeriodAfterAccess()).isEqualTo(validPeriodAfterAccess);
        assertThat(filter.created()).isEqualTo(creation);
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
    public void testExpiredFilter() {
        final AdjustableTimer timer = new AdjustableTimer();
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.plusSeconds(10);
        final GuavaBloomFilter filter = new GuavaBloomFilter(
                1000,
                0.001,
                creation,
                expiration,
                null,
                timer);
        timer.setNow(expiration);
        assertThat(filter.expired()).isFalse();
        timer.setNow(expiration.plusSeconds(1));
        assertThat(filter.expired()).isTrue();
    }

    @Test
    public void testExtendExpirationOnSet() {
        final String testingValue = "SomeValue";
        final AdjustableTimer timer = new AdjustableTimer();
        final Duration validPeriodAfterAccess = Duration.ofSeconds(5);
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.plus(validPeriodAfterAccess);
        final GuavaBloomFilter filter = new GuavaBloomFilter(
                1000,
                0.001,
                creation,
                expiration,
                validPeriodAfterAccess,
                timer);
        timer.setNow(expiration);
        assertThat(filter.expired()).isFalse();
        filter.set(testingValue);
        timer.setNow(expiration.plus(Duration.ofSeconds(1)));
        assertThat(filter.expired()).isFalse();
    }

    @Test
    public void testExtendExpirationOnMightContain() {
        final String testingValue = "SomeValue";
        final AdjustableTimer timer = new AdjustableTimer();
        final Duration validPeriodAfterAccess = Duration.ofSeconds(5);
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.plus(validPeriodAfterAccess);
        final GuavaBloomFilter filter = new GuavaBloomFilter(
                1000,
                0.001,
                creation,
                expiration,
                validPeriodAfterAccess,
                timer);
        timer.setNow(expiration);
        assertThat(filter.expired()).isFalse();
        filter.mightContain(testingValue);
        timer.setNow(expiration.plus(Duration.ofSeconds(1)));
        assertThat(filter.expired()).isFalse();
    }

    @Test
    public void testToJson() throws Exception {
        final Duration validPeriodAfterAccess = Duration.ofSeconds(10);
        final ExpirableBloomFilterConfig config = (ExpirableBloomFilterConfig) defaultTestingConfig.clone();
        config.setValidPeriodAfterAccess(validPeriodAfterAccess);

        final GuavaBloomFilter expectedFilter = testingFactory.createFilter(config);
        final ObjectMapper mapper = new ObjectMapper();

        final String json = mapper.valueToTree(expectedFilter).toString();
        final GuavaBloomFilter filter = new ObjectMapper().readerFor(GuavaBloomFilter.class).readValue(json);
        assertThat(filter).isEqualTo(expectedFilter);
    }

    @Test
    public void testSerializationWithoutValidPeriodAfterAccess() throws Exception {
        final int expectedInsertions = 1000000;
        final double fpp = 0.0001;
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.plus(Duration.ofSeconds(10));
        final GuavaBloomFilter expect = new GuavaBloomFilter(
                expectedInsertions,
                fpp,
                creation,
                expiration,
                null);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        expect.writeTo(out);

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final GuavaBloomFilter actualFilter = GuavaBloomFilter.readFrom(in);

        assertThat(actualFilter).isEqualTo(expect);
        assertThat(actualFilter.fpp()).isEqualTo(fpp);
        assertThat(actualFilter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(actualFilter.expiration().toEpochSecond()).isEqualTo(expiration.toEpochSecond());
        assertThat(actualFilter.validPeriodAfterAccess()).isNull();
        assertThat(actualFilter.created().toEpochSecond()).isEqualTo(creation.toEpochSecond());
        assertThat(actualFilter.expired()).isFalse();
    }

    @Test
    public void testSerializationWithValidPeriodAfterAccess() throws Exception {
        final Duration validPeriodAfterAccess = Duration.ofSeconds(3);
        final int expectedInsertions = 1000000;
        final double fpp = 0.0001;
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.plus(Duration.ofSeconds(10));
        final GuavaBloomFilter expect = new GuavaBloomFilter(
                expectedInsertions,
                fpp,
                creation,
                expiration,
                validPeriodAfterAccess);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        expect.writeTo(out);

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final GuavaBloomFilter actualFilter = testingFactory.readFrom(in);

        assertThat(actualFilter).isEqualTo(expect);
        assertThat(actualFilter.fpp()).isEqualTo(fpp);
        assertThat(actualFilter.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(actualFilter.expiration().toEpochSecond()).isEqualTo(expiration.toEpochSecond());
        assertThat(actualFilter.validPeriodAfterAccess()).isEqualTo(validPeriodAfterAccess);
        assertThat(actualFilter.created().toEpochSecond()).isEqualTo(creation.toEpochSecond());
        assertThat(actualFilter.expired()).isFalse();
    }
}