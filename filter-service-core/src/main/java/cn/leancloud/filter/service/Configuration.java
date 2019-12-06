package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Configuration {
    private static Configuration instance = new Configuration();

    static void initConfiguration(String configFilePath) throws IOException {
        final File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("configuration file: " + configFilePath + " is not exists");
        }

        try {
            instance = new ObjectMapper(new YAMLFactory()).readValue(configFile, Configuration.class);
        } catch (JsonParseException | JsonMappingException ex) {
            throw new IllegalArgumentException("configuration file: " + configFilePath
                    + " is not a legal YAML file or has invalid configurations", ex);
        }
    }

    static Duration purgeFilterInterval() {
        return instance.purgeFilterInterval;
    }

    static Duration persistentFiltersInterval() {
        return instance.persistentFiltersInterval;
    }

    static int maxHttpConnections() {
        return instance.maxHttpConnections;
    }

    static int maxHttpRequestLength() {
        return instance.maxHttpRequestLength;
    }

    static Duration defaultRequestTimeout() {
        return instance.defaultRequestTimeout;
    }

    static int defaultExpectedInsertions() {
        return instance.defaultExpectedInsertions;
    }

    static double defaultFalsePositiveProbability() {
        return instance.defaultFalsePositiveProbability;
    }

    static Duration defaultValidPeriodAfterCreate() {
        return instance.defaultValidSecondsAfterCreate;
    }

    static String persistentStorageDirectory() {
        return instance.persistentStorageDirectory;
    }

    static boolean allowRecoverFromCorruptedPersistentFile() {
        return instance.allowRecoverFromCorruptedPersistentFile;
    }

    static SupportedChannelOptions channelOptions() {
        return instance.channelOptions;
    }

    static List<TriggerPersistenceCriteria> persistenceCriteria() {
        return instance.persistenceCriteria;
    }

    static String spec() {
        return "\npurgeFilterIntervalMillis: " + purgeFilterInterval().toMillis() + "\n" +
                "persistentFiltersIntervalMillis: " + persistentFiltersInterval().toMillis() + "\n" +
                "maxHttpConnections: " + maxHttpConnections() + "\n" +
                "maxHttpRequestLength: " + maxHttpRequestLength() + "B\n" +
                "defaultRequestTimeoutSeconds: " + defaultRequestTimeout().getSeconds() + "\n" +
                "defaultExpectedInsertions: " + defaultExpectedInsertions() + "\n" +
                "defaultFalsePositiveProbability: " + defaultFalsePositiveProbability() + "\n" +
                "defaultValidSecondsAfterCreate: " + defaultValidPeriodAfterCreate().getSeconds() + "\n" +
                "persistentStorageDirectory: " + persistentStorageDirectory() + "\n" +
                "allowRecoverFromCorruptedPersistentFile: " + allowRecoverFromCorruptedPersistentFile() + "\n" +
                "channelOptions: " + channelOptions() + "\n" +
                "triggerPersistenceCriteria: " + persistenceCriteria() + "\n";
    }

    private Duration persistentFiltersInterval;
    private Duration purgeFilterInterval;
    private int maxHttpConnections;
    private int maxHttpRequestLength;
    private Duration defaultRequestTimeout;
    private int defaultExpectedInsertions;
    private double defaultFalsePositiveProbability;
    private Duration defaultValidSecondsAfterCreate;
    private String persistentStorageDirectory;
    private boolean allowRecoverFromCorruptedPersistentFile;
    private SupportedChannelOptions channelOptions;
    private List<TriggerPersistenceCriteria> persistenceCriteria;

    private Configuration() {
        this.persistentFiltersInterval = Duration.ofSeconds(1);
        this.purgeFilterInterval = Duration.ofMillis(300);
        this.maxHttpConnections = 1000;
        this.maxHttpRequestLength = 10485760;
        this.defaultRequestTimeout = Duration.ofSeconds(5);
        this.defaultExpectedInsertions = 1000_000;
        this.defaultFalsePositiveProbability = 0.0001;
        this.defaultValidSecondsAfterCreate = Duration.ofDays(1);
        this.persistentStorageDirectory = System.getProperty("user.dir");
        this.allowRecoverFromCorruptedPersistentFile = true;
        this.channelOptions = new SupportedChannelOptions();
        this.persistenceCriteria = Collections.emptyList();
    }

    @JsonSetter("persistentFiltersIntervalMillis")
    public void setPersistentFiltersInterval(int persistentFiltersIntervalMillis) {
        if (persistentFiltersIntervalMillis <= 0) {
            throw new IllegalArgumentException("persistentFiltersIntervalMillis: "
                    + persistentFiltersIntervalMillis + " (expected: > 0)");
        }

        this.persistentFiltersInterval = Duration.ofMillis(persistentFiltersIntervalMillis);
    }

    @JsonSetter("purgeFilterIntervalMillis")
    public void setPurgeFilterInterval(int purgeFilterIntervalMillis) {
        if (purgeFilterIntervalMillis <= 0) {
            throw new IllegalArgumentException("purgeFilterIntervalMillis: "
                    + purgeFilterIntervalMillis + " (expected: > 0)");
        }
        this.purgeFilterInterval = Duration.ofMillis(purgeFilterIntervalMillis);
    }

    public void setMaxHttpConnections(int maxHttpConnections) {
        if (maxHttpConnections <= 0) {
            throw new IllegalArgumentException("maxHttpConnections: "
                    + maxHttpConnections + " (expected: > 0)");
        }
        this.maxHttpConnections = maxHttpConnections;
    }

    public void setMaxHttpRequestLength(int maxHttpRequestLength) {
        if (maxHttpRequestLength <= 0) {
            throw new IllegalArgumentException("maxHttpRequestLength: "
                    + maxHttpRequestLength + " (expected: > 0)");
        }
        this.maxHttpRequestLength = maxHttpRequestLength;
    }

    @JsonSetter("defaultRequestTimeoutSeconds")
    public void setDefaultRequestTimeout(int defaultRequestTimeoutSeconds) {
        if (defaultRequestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("defaultRequestTimeoutSeconds: "
                    + defaultRequestTimeoutSeconds + " (expected: > 0)");
        }
        this.defaultRequestTimeout = Duration.ofSeconds(defaultRequestTimeoutSeconds);
    }

    public void setDefaultExpectedInsertions(int defaultExpectedInsertions) {
        if (defaultExpectedInsertions <= 0) {
            throw new IllegalArgumentException("defaultExpectedInsertions: "
                    + defaultExpectedInsertions + " (expected: > 0)");
        }

        this.defaultExpectedInsertions = defaultExpectedInsertions;
    }

    public void setDefaultFalsePositiveProbability(double defaultFalsePositiveProbability) {
        if (defaultFalsePositiveProbability <= 0 || defaultFalsePositiveProbability >= 1) {
            throw new IllegalArgumentException("defaultFalsePositiveProbability: "
                    + defaultFalsePositiveProbability + " (expected between 0 and 1 exclusive)");
        }
        this.defaultFalsePositiveProbability = defaultFalsePositiveProbability;
    }

    @JsonSetter("defaultValidSecondsAfterCreate")
    public void setDefaultValidPeriodAfterCreate(long defaultValidSecondsAfterCreate) {
        if (defaultValidSecondsAfterCreate <= 0) {
            throw new IllegalArgumentException("defaultValidSecondsAfterCreate: "
                    + defaultValidSecondsAfterCreate + " (expected: > 0)");
        }
        this.defaultValidSecondsAfterCreate = Duration.ofSeconds(defaultValidSecondsAfterCreate);
    }

    public void setPersistentStorageDirectory(@Nullable String persistentStorageDirectory) {
        if (persistentStorageDirectory == null || persistentStorageDirectory.isEmpty()) {
            persistentStorageDirectory = System.getProperty("user.dir");
            if (persistentStorageDirectory == null || persistentStorageDirectory.isEmpty()) {
                throw new IllegalArgumentException("empty \"persistentStorageDirectory\" config and \"user.dir\" environment property");
            }
        }
        this.persistentStorageDirectory = persistentStorageDirectory;
    }

    public void setAllowRecoverFromCorruptedPersistentFile(boolean allowRecoverFromCorruptedPersistentFile) {
        this.allowRecoverFromCorruptedPersistentFile = allowRecoverFromCorruptedPersistentFile;
    }

    public void setChannelOptions(SupportedChannelOptions channelOptions) {
        this.channelOptions = channelOptions;
    }

    @JsonProperty("triggerPersistenceCriteria")
    public void setPersistenceCriteria(@Nullable List<TriggerPersistenceCriteria> criteriaList) {
        if (criteriaList == null) {
            criteriaList = Collections.emptyList();
        }

        this.persistenceCriteria = criteriaList;
    }

    public static class SupportedChannelOptions {
        private int SO_RCVBUF = 2048;
        private int SO_SNDBUF = 2048;
        private int SO_BACKLOG = 2048;
        private boolean TCP_NODELAY = true;

        @JsonSetter("SO_RCVBUF")
        public void setSoRcvBuf(int SO_RCVBUF) {
            if (SO_RCVBUF <= 0) {
                throw new IllegalArgumentException("SO_RCVBUF: "
                        + SO_RCVBUF + " (expected: > 0)");
            }
            this.SO_RCVBUF = SO_RCVBUF;
        }

        @JsonSetter("SO_SNDBUF")
        public void setSoSndbuf(int SO_SNDBUF) {
            if (SO_SNDBUF <= 0) {
                throw new IllegalArgumentException("SO_SNDBUF: "
                        + SO_SNDBUF + " (expected: > 0)");
            }
            this.SO_SNDBUF = SO_SNDBUF;
        }

        @JsonSetter("SO_BACKLOG")
        public void setSoBacklog(int SO_BACKLOG) {
            if (SO_BACKLOG <= 0) {
                throw new IllegalArgumentException("SO_BACKLOG: "
                        + SO_BACKLOG + " (expected: > 0)");
            }
            this.SO_BACKLOG = SO_BACKLOG;
        }

        @JsonSetter("TCP_NODELAY")
        public void setTcpNodelay(boolean TCP_NODELAY) {
            this.TCP_NODELAY = TCP_NODELAY;
        }

        public int SO_RCVBUF() {
            return SO_RCVBUF;
        }

        public int SO_SNDBUF() {
            return SO_SNDBUF;
        }

        public int SO_BACKLOG() {
            return SO_BACKLOG;
        }

        public boolean TCP_NODELAY() {
            return TCP_NODELAY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SupportedChannelOptions that = (SupportedChannelOptions) o;
            return SO_RCVBUF == that.SO_RCVBUF &&
                    SO_SNDBUF == that.SO_SNDBUF &&
                    SO_BACKLOG == that.SO_BACKLOG &&
                    TCP_NODELAY == that.TCP_NODELAY;
        }

        @Override
        public int hashCode() {
            return Objects.hash(SO_RCVBUF, SO_SNDBUF, SO_BACKLOG, TCP_NODELAY);
        }

        @Override
        public String toString() {
            return "{" +
                    "SO_RCVBUF=" + SO_RCVBUF() +
                    ", SO_SNDBUF=" + SO_SNDBUF() +
                    ", SO_BACKLOG=" + SO_BACKLOG() +
                    ", TCP_NODELAY=" + TCP_NODELAY() +
                    '}';
        }
    }

    public static class TriggerPersistenceCriteria {
        private Duration checkingPeriod;
        private int updatesThreshold;

        private TriggerPersistenceCriteria() {}

        public TriggerPersistenceCriteria(Duration checkingPeriod, int updatesThreshold) {
            this.checkingPeriod = checkingPeriod;
            this.updatesThreshold = updatesThreshold;
        }

        @JsonSetter("periodInSeconds")
        public void setCheckingPeriod(long periodInSeconds) {
            if (periodInSeconds <= 0) {
                throw new IllegalArgumentException("periodInSeconds: "
                        + periodInSeconds + " (expected: > 0)");
            }
            this.checkingPeriod = Duration.ofSeconds(periodInSeconds);
        }

        @JsonSetter("updatesMoreThan")
        public void setUpdatesThreshold(int updatesMoreThan) {
            if (updatesMoreThan <= 0) {
                throw new IllegalArgumentException("updatesMoreThan: "
                        + updatesMoreThan + " (expected: > 0)");
            }

            this.updatesThreshold = updatesMoreThan;
        }

        public Duration checkingPeriod() {
            return checkingPeriod;
        }

        public int updatesThreshold() {
            return updatesThreshold;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final TriggerPersistenceCriteria that = (TriggerPersistenceCriteria) o;
            return updatesThreshold == that.updatesThreshold &&
                    checkingPeriod.equals(that.checkingPeriod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(checkingPeriod, updatesThreshold);
        }

        @Override
        public String toString() {
            return "{" +
                    "periodInSeconds=" + checkingPeriod.getSeconds() +
                    ", updatesMoreThan=" + updatesThreshold +
                    '}';
        }
    }
}
