package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.hash.Funnels;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class GuavaBloomFilter implements ExpirableBloomFilter {
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");


    public static class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {
        @Override
        public void serialize(ZonedDateTime arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {
            final String zonedTime = arg0.format(ISO_8601_FORMATTER);
            arg1.writeString(zonedTime);
        }
    }

    public static class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {
        @Override
        public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return ZonedDateTime.parse(p.getText(), ISO_8601_FORMATTER);
        }
    }

    public static class DurationSerializer extends JsonSerializer<Duration> {
        @Override
        public void serialize(Duration arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {
            arg1.writeNumber(arg0.getSeconds());
        }
    }

    public static class DurationDeserializer extends JsonDeserializer<Duration> {
        @Nullable
        @Override
        public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.getNumberValue() != null) {
                return Duration.ofSeconds(p.getNumberValue().intValue());
            }
            return null;
        }
    }

    private final ZonedDateTime created;
    @JsonIgnore
    private final com.google.common.hash.BloomFilter<CharSequence> filter;
    @Nullable
    private final Duration validPeriodAfterAccess;
    private final double fpp;
    private final int expectedInsertions;
    private final Timer timer;
    private ZonedDateTime expiration;

    /**
     * Constructor with default timer used by {@link GuavaBloomFilterFactory} and JSON serialization.
     */
    @JsonCreator
    GuavaBloomFilter(@JsonProperty("expectedInsertions") int expectedInsertions,
                     @JsonProperty("fpp") double fpp,
                     @JsonProperty("created") ZonedDateTime created,
                     @JsonProperty("expiration") ZonedDateTime expiration,
                     @Nullable @JsonProperty("validPeriodAfterAccess") Duration validPeriodAfterAccess) {
        this(expectedInsertions, fpp, created, expiration, validPeriodAfterAccess, Timer.DEFAULT_TIMER);
    }

    /**
     * Constructor for {@link GuavaBloomFilter}.
     * This constructor can set arbitrary creation and expiration time for the constructed {@code GuavaBloomFilter}.
     *
     * @param expectedInsertions           the number of expected insertions to the constructed {@code GuavaBloomFilter};
     *                                     must be positive
     * @param fpp                          the desired false positive probability (must be positive and less than 1.0)
     * @param created                      the creation time for the constructed {@code GuavaBloomFilter}
     * @param expiration                   the expiration time of the constructed {@code GuavaBloomFilter}. When
     *                                     time past this expiration time, the constructed {@code GuavaBloomFilter}
     *                                     will be expired and can not be used any more.
     * @param validPeriodAfterAccess the time duration to push the expiration of the filter forward after calling
     *                                     {@link GuavaBloomFilter#set(String)} or {@link GuavaBloomFilter#mightContain(String)}
     *                                     each time
     * @param timer                        the {@link Timer} used to get current {@link ZonedDateTime} with UTC offset. This
     *                                     argument is used under test to set an arbitrary current time.
     */
    GuavaBloomFilter(int expectedInsertions,
                     double fpp,
                     ZonedDateTime created,
                     ZonedDateTime expiration,
                     @Nullable Duration validPeriodAfterAccess,
                     Timer timer) {
        this.fpp = fpp;
        this.expectedInsertions = expectedInsertions;
        this.created = created;
        this.expiration = expiration;
        this.filter = com.google.common.hash.BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                fpp);
        this.validPeriodAfterAccess = validPeriodAfterAccess;
        this.timer = timer;
    }

    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    public ZonedDateTime created() {
        return created;
    }

    @Override
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    public synchronized ZonedDateTime expiration() {
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
        final boolean result = filter.put(value);
        tryExtendExpiration();
        return result;
    }

    @Override
    public boolean expired() {
        if (validPeriodAfterAccess == null) {
            return timer.utcNow().isAfter(expiration);
        } else {
            synchronized (this) {
                return timer.utcNow().isAfter(expiration);
            }
        }
    }

    @Override
    public boolean mightContain(String value) {
        final boolean result = filter.mightContain(value);
        tryExtendExpiration();
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final GuavaBloomFilter that = (GuavaBloomFilter) o;
        if ((validPeriodAfterAccess == null && that.validPeriodAfterAccess == null) ||
                validPeriodAfterAccess != null && that.validPeriodAfterAccess != null) {
            return Double.compare(that.fpp, fpp) == 0 &&
                    expectedInsertions == that.expectedInsertions &&
                    created.toInstant().getEpochSecond() == that.created.toInstant().getEpochSecond() &&
                    created.getOffset() == that.created.getOffset() &&
                    expiration.toInstant().getEpochSecond() == that.expiration.toInstant().getEpochSecond() &&
                    expiration.getOffset() == that.expiration.getOffset() &&
                    filter.equals(that.filter) &&
                    (validPeriodAfterAccess == null ||
                            validPeriodAfterAccess.getSeconds() ==
                                    that.validPeriodAfterAccess.getSeconds()) &&
                    timer.equals(that.timer);

        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(created, filter, expiration, fpp, expectedInsertions, validPeriodAfterAccess);
    }

    @Override
    public String toString() {
        return "GuavaBloomFilter{" +
                ", created=" + created +
                ", filter=" + filter +
                ", expiration=" + expiration +
                ", fpp=" + fpp +
                ", expectedInsertions=" + expectedInsertions +
                ", validPeriodAfterAccess" + validPeriodAfterAccess +
                '}';
    }

    @Nullable
    @JsonInclude(Include.NON_NULL)
    @JsonGetter("validPeriodAfterAccess")
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    Duration validPeriodAfterAccess() {
        return validPeriodAfterAccess;
    }

    private void tryExtendExpiration() {
        if (validPeriodAfterAccess != null) {
            synchronized (this) {
                expiration = timer.utcNow().plus(validPeriodAfterAccess);
            }
        }
    }
}
