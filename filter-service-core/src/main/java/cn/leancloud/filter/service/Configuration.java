package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class Configuration {
    private static Configuration instance = new Configuration();

    public static void initConfiguration(String configFilePath) throws IOException {
        final File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("configuration file: " + configFilePath + " is not exists");
        }

        try {
            instance = new ObjectMapper(new YAMLFactory()).readValue(configFile, Configuration.class);
        } catch (JsonParseException | JsonMappingException ex) {
            throw new IllegalArgumentException("configuration file: " + configFilePath
                    + " is not a legal YAML file", ex);
        }
    }

    public static Duration purgeFilterInterval() {
        return Duration.ofMillis(instance.purgeFilterIntervalMillis);
    }

    public static Duration persistentFiltersInterval() {
        return Duration.ofMillis(instance.persistentFiltersIntervalMillis);
    }

    public static int maxHttpConnections() {
        return instance.maxHttpConnections;
    }

    public static int maxHttpRequestLength() {
        return instance.maxHttpRequestLength;
    }

    public static Duration defaultRequestTimeout() {
        return Duration.ofSeconds(instance.defaultRequestTimeoutSeconds);
    }

    public static int defaultExpectedInsertions() {
        return instance.defaultExpectedInsertions;
    }

    public static double defaultFalsePositiveProbability() {
        return instance.defaultFalsePositiveProbability;
    }

    public static Duration defaultValidPeriodAfterCreate() {
        return Duration.ofSeconds(instance.defaultValidSecondsAfterCreate);
    }

    public static String persistentStorageDirectory() {
        return instance.persistentStorageDirectory;
    }

    public static boolean allowRecoverFromCorruptedPersistentFile() {
        return instance.allowRecoverFromCorruptedPersistentFile;
    }

    public static SupportedChannelOptions channelOptions() {
        return instance.channelOptions;
    }

    public static String spec() {
        return "\npurgeFilterInterval: " + purgeFilterInterval() + "\n" +
                "persistentFiltersInterval: " + persistentFiltersInterval() + "\n" +
                "maxHttpConnections: " + maxHttpConnections() + "\n" +
                "maxHttpRequestLength: " + maxHttpRequestLength() + "B\n" +
                "defaultRequestTimeoutSeconds: " + defaultRequestTimeout() + "\n" +
                "defaultExpectedInsertions: " + defaultExpectedInsertions() + "\n" +
                "defaultFalsePositiveProbability: " + defaultFalsePositiveProbability() + "\n" +
                "defaultValidPeriodAfterCreate: " + defaultValidPeriodAfterCreate() + "\n" +
                "persistentStorageDirectory: " + persistentStorageDirectory() + "\n" +
                "allowRecoverFromCorruptedPersistentFile: " + allowRecoverFromCorruptedPersistentFile() + "\n" +
                "channelOptions: " + channelOptions() + "\n";
    }

    private int persistentFiltersIntervalMillis;
    private int purgeFilterIntervalMillis;
    private int maxHttpConnections;
    private int maxHttpRequestLength;
    private int defaultRequestTimeoutSeconds;
    private int defaultExpectedInsertions;
    private double defaultFalsePositiveProbability;
    private long defaultValidSecondsAfterCreate;
    private String persistentStorageDirectory;
    private boolean allowRecoverFromCorruptedPersistentFile;
    private SupportedChannelOptions channelOptions;

    private Configuration() {
        this.persistentFiltersIntervalMillis = 1000;
        this.purgeFilterIntervalMillis = 300;
        this.maxHttpConnections = 1000;
        this.maxHttpRequestLength = 10485760;
        this.defaultRequestTimeoutSeconds = 5;
        this.defaultExpectedInsertions = 1000_000;
        this.defaultFalsePositiveProbability = 0.0001;
        this.defaultValidSecondsAfterCreate = TimeUnit.DAYS.toSeconds(1);
        this.persistentStorageDirectory = System.getProperty("user.dir");
        this.allowRecoverFromCorruptedPersistentFile = true;
        this.channelOptions = new SupportedChannelOptions();
    }

    public void setPersistentFiltersIntervalMillis(int persistentFiltersIntervalMillis) {
        this.persistentFiltersIntervalMillis = persistentFiltersIntervalMillis;
    }

    public void setPurgeFilterIntervalMillis(int purgeFilterIntervalMillis) {
        this.purgeFilterIntervalMillis = purgeFilterIntervalMillis;
    }

    public void setMaxHttpConnections(int maxHttpConnections) {
        this.maxHttpConnections = maxHttpConnections;
    }

    public void setMaxHttpRequestLength(int maxHttpRequestLength) {
        this.maxHttpRequestLength = maxHttpRequestLength;
    }

    public void setDefaultRequestTimeoutSeconds(int defaultRequestTimeoutSeconds) {
        this.defaultRequestTimeoutSeconds = defaultRequestTimeoutSeconds;
    }

    public void setDefaultExpectedInsertions(int defaultExpectedInsertions) {
        this.defaultExpectedInsertions = defaultExpectedInsertions;
    }

    public void setDefaultFalsePositiveProbability(double defaultFalsePositiveProbability) {
        this.defaultFalsePositiveProbability = defaultFalsePositiveProbability;
    }

    public void setDefaultValidSecondsAfterCreate(long defaultValidSecondsAfterCreate) {
        this.defaultValidSecondsAfterCreate = defaultValidSecondsAfterCreate;
    }

    public void setPersistentStorageDirectory(String persistentStorageDirectory) {
        this.persistentStorageDirectory = persistentStorageDirectory;
    }

    public void setAllowRecoverFromCorruptedPersistentFile(boolean allowRecoverFromCorruptedPersistentFile) {
        this.allowRecoverFromCorruptedPersistentFile = allowRecoverFromCorruptedPersistentFile;
    }

    public void setChannelOptions(SupportedChannelOptions channelOptions) {
        this.channelOptions = channelOptions;
    }

    public static class SupportedChannelOptions {
        private int SO_RCVBUF = 2048;
        private int SO_SNDBUF = 2048;
        private int SO_BACKLOG = 2048;
        private boolean TCP_NODELAY = true;

        @JsonProperty("SO_RCVBUF")
        public void setSoRcvBuf(int SO_RCVBUF) {
            this.SO_RCVBUF = SO_RCVBUF;
        }

        @JsonProperty("SO_SNDBUF")
        public void setSoSndbuf(int SO_SNDBUF) {
            this.SO_SNDBUF = SO_SNDBUF;
        }

        @JsonProperty("SO_BACKLOG")
        public void setSoBacklog(int SO_BACKLOG) {
            this.SO_BACKLOG = SO_BACKLOG;
        }

        @JsonProperty("TCP_NODELAY")
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
        public String toString() {
            return "{" +
                    "SO_RCVBUF=" + SO_RCVBUF() +
                    ", SO_SNDBUF=" + SO_SNDBUF() +
                    ", SO_BACKLOG=" + SO_BACKLOG() +
                    ", TCP_NODELAY=" + TCP_NODELAY() +
                    '}';
        }
    }
}
