package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
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
    private final String name;
    private final Instant created;
    @JsonIgnore
    private final com.google.common.hash.BloomFilter<CharSequence> filter;
    private final Instant expiration;
    private final double fpp;
    private final int expectedInsertions;

    GuavaBloomFilter(String name, int expectedInsertions, double fpp, Duration duration) {
        this.name = name;
        this.fpp = fpp;
        this.expectedInsertions = expectedInsertions;
        this.created = Instant.now();
        this.expiration = this.created.plus(duration);
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
    public Instant created() {
        return created;
    }

    @Override
    @JsonSerialize(using = InstantSerializer.class)
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
        private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        @Override
        public void serialize(Instant arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {
            String zonedTime = ZonedDateTime.ofInstant(arg0, ZoneOffset.UTC).format(ISO_8601_FORMATTER);
            arg1.writeString(zonedTime);
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
                created.equals(that.created) &&
                filter.equals(that.filter) &&
                expiration.equals(that.expiration);
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
