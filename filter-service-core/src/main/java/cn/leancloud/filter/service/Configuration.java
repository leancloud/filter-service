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

import static java.util.Objects.requireNonNull;

public final class Configuration {
    private static Configuration instance = new Configuration();

    static void initConfiguration(String configFilePath) throws IOException {
        final File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("configuration file: " + configFilePath + " is not exists");
        }

        try {
            final Configuration configuration = new ObjectMapper(new YAMLFactory()).readValue(configFile, Configuration.class);
            if (configuration != null) {
                instance = configuration;
            }
        } catch (JsonParseException | JsonMappingException ex) {
            throw new IllegalArgumentException("configuration file: " + configFilePath
                    + " is not a legal YAML file or has invalid configurations", ex);
        }
    }

    static void initConfiguration(Configuration configuration) {
        instance = configuration;
    }

    static String metricsPrefix() {
        return instance.metricsPrefix;
    }

    static Duration purgeFilterInterval() {
        return instance.purgeFilterInterval;
    }

    static int maxHttpConnections() {
        return instance.maxHttpConnections;
    }

    static int maxHttpRequestLength() {
        return instance.maxHttpRequestLength;
    }

    static int maxWorkerThreadPoolSize() {
        return instance.maxWorkerThreadPoolSize;
    }

    static Duration requestTimeout() {
        return instance.requestTimeout;
    }

    static long idleTimeoutMillis() {
        return instance.idleTimeoutMillis;
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

    static int channelBufferSizeForFilterPersistence() {
        return instance.channelBufferSizeForFilterPersistence;
    }

    static SupportedChannelOptions channelOptions() {
        return instance.channelOptions;
    }

    static List<TriggerPersistenceCriteria> persistenceCriteria() {
        return instance.persistenceCriteria;
    }

    static long gracefulShutdownQuietPeriodMillis() {
        return instance.gracefulShutdownQuietPeriodMillis;
    }

    static long gracefulShutdownTimeoutMillis() {
        return instance.gracefulShutdownTimeoutMillis;
    }

    static String spec() {
        return "\nmetricsPrefix: " + metricsPrefix() + "\n" +
                "purgeFilterIntervalMillis: " + purgeFilterInterval().toMillis() + "\n" +
                "maxHttpConnections: " + maxHttpConnections() + "\n" +
                "maxHttpRequestLength: " + maxHttpRequestLength() + "B\n" +
                "maxWorkerThreadPoolSize: " + maxWorkerThreadPoolSize() + "\n" +
                "requestTimeoutSeconds: " + requestTimeout().getSeconds() + "\n" +
                "idleTimeoutMillis: " + idleTimeoutMillis() + "\n" +
                "defaultExpectedInsertions: " + defaultExpectedInsertions() + "\n" +
                "defaultFalsePositiveProbability: " + defaultFalsePositiveProbability() + "\n" +
                "defaultValidSecondsAfterCreate: " + defaultValidPeriodAfterCreate().getSeconds() + "\n" +
                "persistentStorageDirectory: " + persistentStorageDirectory() + "\n" +
                "channelBufferSizeForFilterPersistence: " + channelBufferSizeForFilterPersistence() + "B" + "\n" +
                "triggerPersistenceCriteria: " + persistenceCriteria() + "\n" +
                "allowRecoverFromCorruptedPersistentFile: " + allowRecoverFromCorruptedPersistentFile() + "\n" +
                "channelOptions: " + channelOptions() + "\n" +
                "gracefulShutdownQuietPeriodMillis: " + gracefulShutdownQuietPeriodMillis() + "\n" +
                "gracefulShutdownTimeoutMillis: " + gracefulShutdownTimeoutMillis() + "\n";
    }

    private String metricsPrefix;
    private Duration purgeFilterInterval;
    private int maxHttpConnections;
    private int maxHttpRequestLength;
    private int maxWorkerThreadPoolSize;
    private Duration requestTimeout;
    private long idleTimeoutMillis;
    private int defaultExpectedInsertions;
    private double defaultFalsePositiveProbability;
    private Duration defaultValidSecondsAfterCreate;
    private String persistentStorageDirectory;
    private boolean allowRecoverFromCorruptedPersistentFile;
    private int channelBufferSizeForFilterPersistence;
    private SupportedChannelOptions channelOptions;
    private List<TriggerPersistenceCriteria> persistenceCriteria;
    private long gracefulShutdownQuietPeriodMillis;
    private long gracefulShutdownTimeoutMillis;

    // package private for testing
    Configuration() {
        this.metricsPrefix = "filterService";
        this.purgeFilterInterval = Duration.ofMillis(300);
        this.maxHttpConnections = 1000;
        this.maxHttpRequestLength = 10485760;
        this.maxWorkerThreadPoolSize = 10;
        this.requestTimeout = Duration.ofSeconds(5);
        this.idleTimeoutMillis = 10_000;
        this.defaultExpectedInsertions = 1000_000;
        this.defaultFalsePositiveProbability = 0.0001;
        this.defaultValidSecondsAfterCreate = Duration.ofDays(1);
        this.persistentStorageDirectory = System.getProperty("user.dir");
        this.allowRecoverFromCorruptedPersistentFile = true;
        this.channelBufferSizeForFilterPersistence = 102400;
        this.channelOptions = new SupportedChannelOptions();
        this.persistenceCriteria = Collections.singletonList(new TriggerPersistenceCriteria(Duration.ofMinutes(5), 100));
        this.gracefulShutdownQuietPeriodMillis = 0;
        this.gracefulShutdownTimeoutMillis = 0;
    }

    public void setMetricsPrefix(String metricsPrefix) {
        requireNonNull(metricsPrefix, "metricsPrefix");

        this.metricsPrefix = metricsPrefix;
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

    public void setMaxWorkerThreadPoolSize(int maxWorkerThreadPoolSize) {
        if (maxWorkerThreadPoolSize <= 0) {
            throw new IllegalArgumentException("maxWorkerThreadPoolSize: "
                    + maxWorkerThreadPoolSize + " (expected: > 0)");
        }
        this.maxWorkerThreadPoolSize = maxWorkerThreadPoolSize;
    }

    @JsonSetter("requestTimeoutSeconds")
    public void setRequestTimeout(int requestTimeoutSeconds) {
        if (requestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("requestTimeoutSeconds: "
                    + requestTimeoutSeconds + " (expected: > 0)");
        }
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
    }

    public void setIdleTimeoutMillis(int idleTimeoutMillis) {
        if (idleTimeoutMillis <= 0) {
            throw new IllegalArgumentException("idleTimeoutMillis: "
                    + idleTimeoutMillis + " (expected: > 0)");
        }
        this.idleTimeoutMillis = idleTimeoutMillis;
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

    public void setChannelBufferSizeForFilterPersistence(int channelBufferSizeForFilterPersistence) {
        if (channelBufferSizeForFilterPersistence <= 0) {
            throw new IllegalArgumentException("channelBufferSizeForFilterPersistence: "
                    + channelBufferSizeForFilterPersistence + " (expected: > 0)");
        }
        this.channelBufferSizeForFilterPersistence = channelBufferSizeForFilterPersistence;
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
        requireNonNull(channelOptions, "channelOptions");

        this.channelOptions = channelOptions;
    }

    @JsonProperty("triggerPersistenceCriteria")
    public void setPersistenceCriteria(@Nullable List<TriggerPersistenceCriteria> criteriaList) {
        if (criteriaList == null) {
            criteriaList = Collections.emptyList();
        }

        this.persistenceCriteria = criteriaList;
    }

    public void setGracefulShutdownQuietPeriodMillis(long gracefulShutdownQuietPeriodMillis) {
        if (gracefulShutdownQuietPeriodMillis < 0) {
            throw new IllegalArgumentException("gracefulShutdownQuietPeriodMillis: "
                    + gracefulShutdownQuietPeriodMillis + " (expected: >= 0)");
        }
        if (gracefulShutdownQuietPeriodMillis > this.gracefulShutdownTimeoutMillis) {
            this.gracefulShutdownTimeoutMillis = gracefulShutdownQuietPeriodMillis;
        }

        this.gracefulShutdownQuietPeriodMillis = gracefulShutdownQuietPeriodMillis;
    }

    public void setGracefulShutdownTimeoutMillis(long gracefulShutdownTimeoutMillis) {
        if (gracefulShutdownTimeoutMillis < 0) {
            throw new IllegalArgumentException("gracefulShutdownTimeoutMillis: "
                    + gracefulShutdownTimeoutMillis + " (expected: >= 0)");
        }

        if (gracefulShutdownTimeoutMillis < this.gracefulShutdownQuietPeriodMillis) {
            this.gracefulShutdownQuietPeriodMillis = gracefulShutdownTimeoutMillis;
        }

        this.gracefulShutdownTimeoutMillis = gracefulShutdownTimeoutMillis;
    }

    public static class SupportedChannelOptions {
        private int soRcvbuf = 2048;
        private int soSndBuf = 2048;
        private int soBackLog = 2048;
        private boolean tcpNoDelay = true;

        @JsonSetter("SO_RCVBUF")
        public void setSoRcvBuf(int SO_RCVBUF) {
            if (SO_RCVBUF <= 0) {
                throw new IllegalArgumentException("SO_RCVBUF: "
                        + SO_RCVBUF + " (expected: > 0)");
            }
            this.soRcvbuf = SO_RCVBUF;
        }

        @JsonSetter("SO_SNDBUF")
        public void setSoSndbuf(int SO_SNDBUF) {
            if (SO_SNDBUF <= 0) {
                throw new IllegalArgumentException("SO_SNDBUF: "
                        + SO_SNDBUF + " (expected: > 0)");
            }
            this.soSndBuf = SO_SNDBUF;
        }

        @JsonSetter("SO_BACKLOG")
        public void setSoBacklog(int SO_BACKLOG) {
            if (SO_BACKLOG <= 0) {
                throw new IllegalArgumentException("SO_BACKLOG: "
                        + SO_BACKLOG + " (expected: > 0)");
            }
            this.soBackLog = SO_BACKLOG;
        }

        @JsonSetter("TCP_NODELAY")
        public void setTcpNodelay(boolean TCP_NODELAY) {
            this.tcpNoDelay = TCP_NODELAY;
        }

        int SO_RCVBUF() {
            return soRcvbuf;
        }

        int SO_SNDBUF() {
            return soSndBuf;
        }

        int SO_BACKLOG() {
            return soBackLog;
        }

        boolean TCP_NODELAY() {
            return tcpNoDelay;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SupportedChannelOptions that = (SupportedChannelOptions) o;
            return soRcvbuf == that.soRcvbuf &&
                    soSndBuf == that.soSndBuf &&
                    soBackLog == that.soBackLog &&
                    tcpNoDelay == that.tcpNoDelay;
        }

        @Override
        public int hashCode() {
            int ret = Integer.hashCode(soRcvbuf);
            ret = 31 * ret + Integer.hashCode(soSndBuf);
            ret = 31 * ret + Integer.hashCode(soBackLog);
            ret = 31 * ret + Boolean.hashCode(tcpNoDelay);
            return ret;
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

        // used by Jackson to deserialze YMAL to Object
        private TriggerPersistenceCriteria() {}

        TriggerPersistenceCriteria(Duration checkingPeriod, int updatesThreshold) {
            this.checkingPeriod = checkingPeriod;
            this.updatesThreshold = updatesThreshold;
        }

        @JsonSetter("periodInSeconds")
        public void setCheckingPeriod(long persistentCheckingPeriodInSeconds) {
            if (persistentCheckingPeriodInSeconds <= 0) {
                throw new IllegalArgumentException("persistentCheckingPeriodInSeconds: "
                        + persistentCheckingPeriodInSeconds + " (expected: > 0)");
            }
            this.checkingPeriod = Duration.ofSeconds(persistentCheckingPeriodInSeconds);
        }

        @JsonSetter("updatesMoreThan")
        public void setUpdatesThreshold(int persistentUpdatesCriteria) {
            if (persistentUpdatesCriteria <= 0) {
                throw new IllegalArgumentException("persistentUpdatesCriteria: "
                        + persistentUpdatesCriteria + " (expected: > 0)");
            }

            this.updatesThreshold = persistentUpdatesCriteria;
        }

        Duration checkingPeriod() {
            return checkingPeriod;
        }

        int updatesThreshold() {
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
            int ret = checkingPeriod.hashCode();
            ret = 31 * ret + Integer.hashCode(updatesThreshold);
            return ret;
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
