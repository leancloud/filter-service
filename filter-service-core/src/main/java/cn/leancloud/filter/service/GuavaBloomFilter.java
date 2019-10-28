package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class GuavaBloomFilter implements ExpirableBloomFilter {
    static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final String name;
    private final Instant created;
    @JsonIgnore
    private final com.google.common.hash.BloomFilter<CharSequence> filter;
    private final Instant expiration;
    private final double fpp;
    private final int expectedInsertions;

    /**
     * Internal testing usage only.
     * This constructor can set an arbitrary creation time for the constructed {@code GuavaBloomFilter}.
     *
     * @param name               the name for the filter
     * @param expectedInsertions the number of expected insertions to the constructed {@code GuavaBloomFilter};
     *                           must be positive
     * @param fpp                the desired false positive probability (must be positive and less than 1.0)
     * @param validPeriod        the valid duration in second for the constructed {@code GuavaBloomFilter}. When
     *                           time past creation time + validPeriod, the constructed {@code GuavaBloomFilter}
     *                           will be expired and can not be used any more.
     */
    GuavaBloomFilter(String name, int expectedInsertions, double fpp, Duration validPeriod) {
        this(name, expectedInsertions, fpp, Instant.now(), validPeriod);
    }

    /**
     * Internal testing usage only.
     * This constructor can set an arbitrary creation time for the constructed {@code GuavaBloomFilter}.
     *
     * @param name               the name for the filter
     * @param expectedInsertions the number of expected insertions to the constructed {@code GuavaBloomFilter};
     *                           must be positive
     * @param fpp                the desired false positive probability (must be positive and less than 1.0)
     * @param created            the creation time for the constructed {@code GuavaBloomFilter}
     * @param validPeriod        the valid duration in second for the constructed {@code GuavaBloomFilter}. When
     *                           time past creation time + validPeriod, the constructed {@code GuavaBloomFilter}
     *                           will be expired and can not be used any more.
     */
    GuavaBloomFilter(String name, int expectedInsertions, double fpp, Instant created, Duration validPeriod) {
        this(name, expectedInsertions, fpp, created, created.plus(validPeriod));
    }

    @JsonCreator
    private GuavaBloomFilter(@JsonProperty("name") String name,
                             @JsonProperty("expectedInsertions") int expectedInsertions,
                             @JsonProperty("fpp") double fpp,
                             @JsonProperty("created") Instant created,
                             @JsonProperty("expiration") Instant expiration) {
        this.name = name;
        this.fpp = fpp;
        this.expectedInsertions = expectedInsertions;
        this.created = created;
        this.expiration = expiration;
        this.filter = com.google.common.hash.BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8)
                , expectedInsertions,
                fpp);
    }


    @Override
    @JsonGetter("name")
    public String name() {
        return name;
    }

    @Override
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDerializer.class)
    public Instant created() {
        return created;
    }

    @Override
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDerializer.class)
    public Instant expiration() {
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
    public void set(String value) {
        filter.put(value);
    }

    @Override
    public boolean expired() {
        return Instant.now().isAfter(expiration);
    }

    @Override
    public boolean mightContain(String value) {
        return filter.mightContain(value);
    }

    public static class InstantSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {
            String zonedTime = ZonedDateTime.ofInstant(arg0, ZoneOffset.UTC).format(ISO_8601_FORMATTER);
            arg1.writeString(zonedTime);
        }
    }

    public static class InstantDerializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return ZonedDateTime.parse(p.getText(), ISO_8601_FORMATTER).toInstant();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final GuavaBloomFilter that = (GuavaBloomFilter) o;
        return Double.compare(that.fpp, fpp) == 0 &&
                expectedInsertions == that.expectedInsertions &&
                name.equals(that.name) &&
                created.getEpochSecond() == that.created.getEpochSecond() &&
                expiration.getEpochSecond() == that.expiration.getEpochSecond() &&
                filter.equals(that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, created, filter, expiration, fpp, expectedInsertions);
    }

    @Override
    public String toString() {
        return "GuavaBloomFilter{" +
                "name='" + name + '\'' +
                ", created=" + created +
                ", filter=" + filter +
                ", expiration=" + expiration +
                ", fpp=" + fpp +
                ", expectedInsertions=" + expectedInsertions +
                '}';
    }
}
