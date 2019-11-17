package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.hash.Funnels;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class GuavaBloomFilter implements ExpirableBloomFilter {
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final ZonedDateTime created;
    @JsonIgnore
    private final com.google.common.hash.BloomFilter<CharSequence> filter;
    private final ZonedDateTime expiration;
    private final double fpp;
    private final int expectedInsertions;

    /**
     * Constructor used by {@link GuavaBloomFilterFactory}.
     *
     * @param expectedInsertions the number of expected insertions to the constructed {@code GuavaBloomFilter};
     *                           must be positive
     * @param fpp                the desired false positive probability (must be positive and less than 1.0)
     * @param validPeriod        the valid duration in second for the constructed {@code GuavaBloomFilter}. When
     *                           time past creation time + validPeriod, the constructed {@code GuavaBloomFilter}
     *                           will be expired and can not be used any more.
     */
    GuavaBloomFilter(int expectedInsertions, double fpp, Duration validPeriod) {
        this(expectedInsertions, fpp, ZonedDateTime.now(ZoneOffset.UTC), validPeriod);
    }

    private GuavaBloomFilter(int expectedInsertions, double fpp, ZonedDateTime created, Duration validPeriod) {
        this(expectedInsertions, fpp, created, created.plus(validPeriod));
    }

    /**
     * Constructor used by testing usage and JSON serialization.
     * This constructor can set arbitrary creation and expiration time for the constructed {@code GuavaBloomFilter}.
     *
     * @param expectedInsertions the number of expected insertions to the constructed {@code GuavaBloomFilter};
     *                           must be positive
     * @param fpp                the desired false positive probability (must be positive and less than 1.0)
     * @param created            the creation time for the constructed {@code GuavaBloomFilter}
     * @param expiration         the expiration time of the constructed {@code GuavaBloomFilter}. When
     *                           time past this expiration time, the constructed {@code GuavaBloomFilter}
     *                           will be expired and can not be used any more.
     */
    @JsonCreator
    GuavaBloomFilter(@JsonProperty("expectedInsertions") int expectedInsertions,
                     @JsonProperty("fpp") double fpp,
                     @JsonProperty("created") ZonedDateTime created,
                     @JsonProperty("expiration") ZonedDateTime expiration) {
        this.fpp = fpp;
        this.expectedInsertions = expectedInsertions;
        this.created = created;
        this.expiration = expiration;
        this.filter = com.google.common.hash.BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8)
                , expectedInsertions,
                fpp);
    }

    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDerializer.class)
    public ZonedDateTime created() {
        return created;
    }

    @Override
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDerializer.class)
    public ZonedDateTime expiration() {
        return expiration;
    }

    @Override
    @JsonGetter("fpp")
    public double fpp() {
        return fpp;
    }

    @Override
    @JsonGetter("expectedInsertions")
    public int expectedInsertions() {
        return expectedInsertions;
    }

    @Override
    public boolean set(String value) {
        return filter.put(value);
    }

    @Override
    public boolean expired() {
        return ZonedDateTime.now(ZoneOffset.UTC).isAfter(expiration);
    }

    @Override
    public boolean mightContain(String value) {
        return filter.mightContain(value);
    }

    public static class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {
        @Override
        public void serialize(ZonedDateTime arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {
            final String zonedTime = arg0.format(ISO_8601_FORMATTER);
            arg1.writeString(zonedTime);
        }
    }

    public static class ZonedDateTimeDerializer extends JsonDeserializer<ZonedDateTime> {
        @Override
        public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return ZonedDateTime.parse(p.getText(), ISO_8601_FORMATTER);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final GuavaBloomFilter that = (GuavaBloomFilter) o;
        return Double.compare(that.fpp, fpp) == 0 &&
                expectedInsertions == that.expectedInsertions &&
                created.toInstant().getEpochSecond() == that.created.toInstant().getEpochSecond() &&
                created.getOffset() == that.created.getOffset() &&
                expiration.toInstant().getEpochSecond() == that.expiration.toInstant().getEpochSecond() &&
                expiration.getOffset() == that.expiration.getOffset() &&
                filter.equals(that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(created, filter, expiration, fpp, expectedInsertions);
    }

    @Override
    public String toString() {
        return "GuavaBloomFilter{" +
                ", created=" + created +
                ", filter=" + filter +
                ", expiration=" + expiration +
                ", fpp=" + fpp +
                ", expectedInsertions=" + expectedInsertions +
                '}';
    }
}
