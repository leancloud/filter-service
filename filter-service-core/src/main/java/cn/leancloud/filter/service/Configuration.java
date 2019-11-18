package cn.leancloud.filter.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.Duration;

public final class Configuration {
    @Nullable
    private static Configuration instance;

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
        ensureConfigurationInitialized();
        assert instance != null;
        return Duration.ofMillis(instance.purgeFilterIntervalMillis);
    }

    public static int maxHttpConnections() {
        ensureConfigurationInitialized();
        assert instance != null;
        return instance.maxHttpConnections;
    }

    public static int maxHttpRequestLength() {
        ensureConfigurationInitialized();
        assert instance != null;
        return instance.maxHttpRequestLength;
    }

    public static Duration defaultRequestTimeout() {
        ensureConfigurationInitialized();
        assert instance != null;
        return Duration.ofSeconds(instance.defaultRequestTimeoutSeconds);
    }

    public static SupportedChannelOptions channelOptions() {
        ensureConfigurationInitialized();
        assert instance != null;
        return instance.channelOptions;
    }

    public static String spec() {
        return "\npurgeFilterInterval: " + purgeFilterInterval() + "\n" +
                "maxHttpConnections: " + maxHttpConnections() + "\n" +
                "maxHttpRequestLength: " + maxHttpRequestLength() + "B\n" +
                "defaultRequestTimeoutSeconds: " + defaultRequestTimeout() + "\n" +
                "channelOptions: " + channelOptions() + "\n";
    }

    private static void ensureConfigurationInitialized() {
        if (instance == null) {
            throw new IllegalStateException("configuration is not initialized");
        }
    }

    private int purgeFilterIntervalMillis;
    private int maxHttpConnections;
    private int maxHttpRequestLength;
    private int defaultRequestTimeoutSeconds;
    private SupportedChannelOptions channelOptions;

    private Configuration() { this.channelOptions = new SupportedChannelOptions();}

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
