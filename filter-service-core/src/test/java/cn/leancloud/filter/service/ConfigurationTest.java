package cn.leancloud.filter.service;

import cn.leancloud.filter.service.Configuration.SupportedChannelOptions;
import cn.leancloud.filter.service.Configuration.TriggerPersistenceCriteria;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigurationTest {
    @Test
    public void testSetEmptyPersistenceCriteria() throws Exception{
        Configuration.initConfiguration("src/test/resources/empty-configuration.yaml");
        assertThat(Configuration.persistenceCriteria())
                .hasSize(1)
                .contains(new TriggerPersistenceCriteria(Duration.ofSeconds(300), 100));
    }

    @Test
    public void testSetEmptyChannelOptions() throws Exception{
        Configuration.initConfiguration("src/test/resources/empty-configuration.yaml");
        assertThat(Configuration.channelOptions().SO_BACKLOG()).isEqualTo(2048);
        assertThat(Configuration.channelOptions().SO_RCVBUF()).isEqualTo(2048);
        assertThat(Configuration.channelOptions().SO_SNDBUF()).isEqualTo(2048);
        assertThat(Configuration.channelOptions().TCP_NODELAY()).isTrue();
    }

    @Test
    public void testDefaultValues() {
        assertThat(Configuration.metricsPrefix()).isEqualTo("filterService");
        assertThat(Configuration.purgeFilterInterval()).isEqualTo(Duration.ofMillis(300));
        assertThat(Configuration.maxHttpConnections()).isEqualTo(1000);
        assertThat(Configuration.maxHttpRequestLength()).isEqualTo(10 * 1024 * 1024);
        assertThat(Configuration.maxWorkerThreadPoolSize()).isEqualTo(10);
        assertThat(Configuration.requestTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(Configuration.idleTimeoutMillis()).isEqualTo(10_000);
        assertThat(Configuration.defaultExpectedInsertions()).isEqualTo(1000_000);
        assertThat(Configuration.defaultFalsePositiveProbability()).isEqualTo(0.0001);
        assertThat(Configuration.defaultValidPeriodAfterCreate()).isEqualTo(Duration.ofDays(1));
        assertThat(Configuration.persistentStorageDirectory()).isEqualTo(System.getProperty("user.dir"));
        assertThat(Configuration.channelBufferSizeForFilterPersistence()).isEqualTo(102400);
        assertThat(Configuration.allowRecoverFromCorruptedPersistentFile()).isTrue();
        assertThat(Configuration.channelOptions().SO_BACKLOG()).isEqualTo(2048);
        assertThat(Configuration.channelOptions().SO_RCVBUF()).isEqualTo(2048);
        assertThat(Configuration.channelOptions().SO_SNDBUF()).isEqualTo(2048);
        assertThat(Configuration.channelOptions().TCP_NODELAY()).isTrue();
        assertThat(Configuration.gracefulShutdownQuietPeriodMillis()).isZero();
        assertThat(Configuration.gracefulShutdownTimeoutMillis()).isZero();
    }

    @Test
    public void testConfigurationFileNotExists() {
        assertThatThrownBy(() -> Configuration.initConfiguration("not-exists.yaml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("configuration file: .* is not exists");
    }

    @Test
    public void testConfigurationFileIllegal() {
        assertThatThrownBy(() -> Configuration.initConfiguration("src/test/resources/illegal-configuration.yaml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("configuration file: .* is not a legal YAML file or has invalid configurations");
    }

    @Test
    public void testNormalConfigurationFile() throws Exception {
        Configuration.initConfiguration("src/test/resources/testing-configuration.yaml");
        System.out.println(Configuration.spec());
        assertThat(Configuration.metricsPrefix()).isEqualTo("filterServiceTest");
        assertThat(Configuration.purgeFilterInterval()).isEqualTo(Duration.ofMillis(200));
        assertThat(Configuration.maxHttpConnections()).isEqualTo(2000);
        assertThat(Configuration.maxHttpRequestLength()).isEqualTo(5 * 1024 * 1024);
        assertThat(Configuration.maxWorkerThreadPoolSize()).isEqualTo(11);
        assertThat(Configuration.requestTimeout()).isEqualTo(Duration.ofSeconds(6));
        assertThat(Configuration.idleTimeoutMillis()).isEqualTo(20_000);
        assertThat(Configuration.defaultExpectedInsertions()).isEqualTo(2000_000);
        assertThat(Configuration.defaultFalsePositiveProbability()).isEqualTo(0.0002);
        assertThat(Configuration.defaultValidPeriodAfterCreate()).isEqualTo(Duration.ofDays(2));
        assertThat(Configuration.persistentStorageDirectory()).isEqualTo("./log/storage");
        assertThat(Configuration.channelBufferSizeForFilterPersistence()).isEqualTo(102401);
        assertThat(Configuration.allowRecoverFromCorruptedPersistentFile()).isTrue();
        assertThat(Configuration.channelOptions().SO_BACKLOG()).isEqualTo(1024);
        assertThat(Configuration.channelOptions().SO_RCVBUF()).isEqualTo(1024);
        assertThat(Configuration.channelOptions().SO_SNDBUF()).isEqualTo(1024);
        assertThat(Configuration.channelOptions().TCP_NODELAY()).isFalse();
        assertThat(Configuration.persistenceCriteria())
                .hasSize(3)
                .contains(new TriggerPersistenceCriteria(Duration.ofSeconds(901), 2))
                .contains(new TriggerPersistenceCriteria(Duration.ofSeconds(301), 11))
                .contains(new TriggerPersistenceCriteria(Duration.ofSeconds(61), 10001));
        assertThat(Configuration.gracefulShutdownQuietPeriodMillis()).isEqualTo(1);
        assertThat(Configuration.gracefulShutdownTimeoutMillis()).isEqualTo(1);
    }

    @Test
    public void testSetNullMetricsPrefix() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setMetricsPrefix(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("metricsPrefix");
    }

    @Test
    public void testSetInvalidPurgeFilterInterval() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setPurgeFilterInterval(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("purgeFilterIntervalMillis: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setPurgeFilterInterval(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("purgeFilterIntervalMillis: 0 (expected: > 0)");
    }

    @Test
    public void testSetMaxHttpConnections() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setMaxHttpConnections(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxHttpConnections: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setMaxHttpConnections(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxHttpConnections: 0 (expected: > 0)");
    }

    @Test
    public void testSetMaxHttpRequestLength() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setMaxHttpRequestLength(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxHttpRequestLength: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setMaxHttpRequestLength(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxHttpRequestLength: 0 (expected: > 0)");
    }

    @Test
    public void testSetMaxWorkerThreadPoolSize() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setMaxWorkerThreadPoolSize(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxWorkerThreadPoolSize: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setMaxWorkerThreadPoolSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxWorkerThreadPoolSize: 0 (expected: > 0)");
    }

    @Test
    public void testSetRequestTimeout() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setRequestTimeout(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestTimeoutSeconds: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setRequestTimeout(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestTimeoutSeconds: 0 (expected: > 0)");
    }

    @Test
    public void testSetIdleTimeoutMillis() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setIdleTimeoutMillis(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idleTimeoutMillis: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setIdleTimeoutMillis(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idleTimeoutMillis: 0 (expected: > 0)");
    }

    @Test
    public void testSetDefaultExpectedInsertions() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setDefaultExpectedInsertions(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultExpectedInsertions: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setDefaultExpectedInsertions(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultExpectedInsertions: 0 (expected: > 0)");
    }

    @Test
    public void testSetDefaultFalsePositiveProbability() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setDefaultFalsePositiveProbability(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultFalsePositiveProbability: -1.0 (expected between 0 and 1 exclusive)");

        assertThatThrownBy(() -> c.setDefaultFalsePositiveProbability(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultFalsePositiveProbability: 0.0 (expected between 0 and 1 exclusive)");

        assertThatThrownBy(() -> c.setDefaultFalsePositiveProbability(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultFalsePositiveProbability: 1.0 (expected between 0 and 1 exclusive)");

        assertThatThrownBy(() -> c.setDefaultFalsePositiveProbability(1.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultFalsePositiveProbability: 1.1 (expected between 0 and 1 exclusive)");
    }

    @Test
    public void testSetDefaultValidPeriodAfterCreate() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setDefaultValidPeriodAfterCreate(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultValidSecondsAfterCreate: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setDefaultValidPeriodAfterCreate(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultValidSecondsAfterCreate: 0 (expected: > 0)");
    }

    @Test
    public void testSetChannelBufferSizeForFilterPersistence() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setChannelBufferSizeForFilterPersistence(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channelBufferSizeForFilterPersistence: -1 (expected: > 0)");

        assertThatThrownBy(() -> c.setChannelBufferSizeForFilterPersistence(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channelBufferSizeForFilterPersistence: 0 (expected: > 0)");
    }

    @Test
    public void testSetPersistentStorageDirectory() {
        Configuration c = new Configuration();
        c.setPersistentStorageDirectory(null);
        assertThat(Configuration.persistentStorageDirectory()).isEqualTo(System.getProperty("user.dir"));

        c = new Configuration();
        c.setPersistentStorageDirectory("");
        assertThat(Configuration.persistentStorageDirectory()).isEqualTo(System.getProperty("user.dir"));
    }

    @Test
    public void testSetPersistentStorageDirectoryFailed() {
        final Configuration c = new Configuration();
        String userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", "");
        assertThatThrownBy(() -> c.setPersistentStorageDirectory(""))
                .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("empty \"persistentStorageDirectory\" config and \"user.dir\" environment property");
        System.setProperty("user.dir", userDir);
    }

    @Test
    public void testSetNullChannelOptions() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setChannelOptions(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("channelOptions");
    }

    @Test
    public void testNegativeGracefulShutdownMillis() {
        final Configuration c = new Configuration();
        assertThatThrownBy(() -> c.setGracefulShutdownQuietPeriodMillis(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.setGracefulShutdownTimeoutMillis(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSetGracefulShutdownQuietGreaterThanTimeoutPeriod() {
        final Configuration c = new Configuration();
        c.setGracefulShutdownQuietPeriodMillis(100);
        Configuration.initConfiguration(c);
        assertThat(Configuration.gracefulShutdownQuietPeriodMillis()).isEqualTo(100);
        assertThat(Configuration.gracefulShutdownTimeoutMillis()).isEqualTo(100);
    }

    @Test
    public void testSetGracefulShutdownTimeoutLowerThanQuietPeriod() {
        final Configuration c = new Configuration();
        c.setGracefulShutdownTimeoutMillis(100);
        c.setGracefulShutdownQuietPeriodMillis(50);
        c.setGracefulShutdownTimeoutMillis(49);
        Configuration.initConfiguration(c);
        assertThat(Configuration.gracefulShutdownQuietPeriodMillis()).isEqualTo(49);
        assertThat(Configuration.gracefulShutdownTimeoutMillis()).isEqualTo(49);
    }

    @Test
    public void testSetSoRcvBuf() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        assertThatThrownBy(() -> options.setSoRcvBuf(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SO_RCVBUF: -1 (expected: > 0)");

        assertThatThrownBy(() -> options.setSoRcvBuf(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SO_RCVBUF: 0 (expected: > 0)");
    }

    @Test
    public void testSetSoSndBuf() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        assertThatThrownBy(() -> options.setSoSndbuf(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SO_SNDBUF: -1 (expected: > 0)");

        assertThatThrownBy(() -> options.setSoSndbuf(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SO_SNDBUF: 0 (expected: > 0)");
    }

    @Test
    public void testSetSoBackLog() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        assertThatThrownBy(() -> options.setSoBacklog(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SO_BACKLOG: -1 (expected: > 0)");

        assertThatThrownBy(() -> options.setSoBacklog(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SO_BACKLOG: 0 (expected: > 0)");
    }

    @Test
    public void testHashAndEqualsInSupportedChannelOptions() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        options.setSoBacklog(1024);
        options.setSoSndbuf(1024);
        options.setSoRcvBuf(1024);
        options.setTcpNodelay(true);

        final SupportedChannelOptions options2 = new SupportedChannelOptions();
        options2.setSoBacklog(1024);
        options2.setSoSndbuf(1024);
        options2.setSoRcvBuf(1024);
        options2.setTcpNodelay(true);
        assertThat(options.hashCode()).isEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isTrue();
    }

    @Test
    public void testHashAndEqualsInSupportedChannelOptions2() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        options.setSoBacklog(1024);
        options.setSoSndbuf(1024);
        options.setSoRcvBuf(1024);
        options.setTcpNodelay(true);

        final SupportedChannelOptions options2 = new SupportedChannelOptions();
        options2.setSoBacklog(1025);
        options2.setSoSndbuf(1024);
        options2.setSoRcvBuf(1024);
        options2.setTcpNodelay(true);
        assertThat(options.hashCode()).isNotEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isFalse();
    }

    @Test
    public void testHashAndEqualsInSupportedChannelOptions3() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        options.setSoBacklog(1024);
        options.setSoSndbuf(1024);
        options.setSoRcvBuf(1024);
        options.setTcpNodelay(true);

        final SupportedChannelOptions options2 = new SupportedChannelOptions();
        options2.setSoBacklog(1024);
        options2.setSoSndbuf(1025);
        options2.setSoRcvBuf(1024);
        options2.setTcpNodelay(true);
        assertThat(options.hashCode()).isNotEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isFalse();
    }

    @Test
    public void testHashAndEqualsInSupportedChannelOptions4() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        options.setSoBacklog(1024);
        options.setSoSndbuf(1024);
        options.setSoRcvBuf(1024);
        options.setTcpNodelay(true);

        final SupportedChannelOptions options2 = new SupportedChannelOptions();
        options2.setSoBacklog(1024);
        options2.setSoSndbuf(1024);
        options2.setSoRcvBuf(1025);
        options2.setTcpNodelay(true);
        assertThat(options.hashCode()).isNotEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isFalse();
    }

    @Test
    public void testHashAndEqualsInSupportedChannelOptions5() {
        final SupportedChannelOptions options = new SupportedChannelOptions();
        options.setSoBacklog(1024);
        options.setSoSndbuf(1024);
        options.setSoRcvBuf(1024);
        options.setTcpNodelay(true);

        final SupportedChannelOptions options2 = new SupportedChannelOptions();
        options2.setSoBacklog(1024);
        options2.setSoSndbuf(1024);
        options2.setSoRcvBuf(1024);
        options2.setTcpNodelay(false);
        assertThat(options.hashCode()).isNotEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isFalse();
    }

    @Test
    public void testCreateCheckingPeriod() {
        final TriggerPersistenceCriteria options = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 10);
        assertThat(options.updatesThreshold()).isEqualTo(10);
        assertThat(options.checkingPeriod()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    public void testSetCheckingPeriodFailed() {
        final TriggerPersistenceCriteria options = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 10);

        assertThatThrownBy(() -> options.setCheckingPeriod(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("persistentCheckingPeriodInSeconds: -1 (expected: > 0)");

        assertThatThrownBy(() -> options.setCheckingPeriod(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("persistentCheckingPeriodInSeconds: 0 (expected: > 0)");
    }

    @Test
    public void testSetUpdatesThresholdFailed() {
        final TriggerPersistenceCriteria options = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 10);

        assertThatThrownBy(() -> options.setUpdatesThreshold(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("persistentUpdatesCriteria: -1 (expected: > 0)");

        assertThatThrownBy(() -> options.setUpdatesThreshold(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("persistentUpdatesCriteria: 0 (expected: > 0)");
    }

    @Test
    public void testHashAndEqualsInTriggerPersistenceCriteria() {
        final TriggerPersistenceCriteria options = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 10);
        final TriggerPersistenceCriteria options2 = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 10);
        assertThat(options.hashCode()).isEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isTrue();
    }

    @Test
    public void testHashAndEqualsInTriggerPersistenceCriteria2() {
        final TriggerPersistenceCriteria options = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 10);
        final TriggerPersistenceCriteria options2 = new TriggerPersistenceCriteria(Duration.ofSeconds(11), 10);
        assertThat(options.hashCode()).isNotEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isFalse();
    }

    @Test
    public void testHashAndEqualsInTriggerPersistenceCriteria3() {
        final TriggerPersistenceCriteria options = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 10);
        final TriggerPersistenceCriteria options2 = new TriggerPersistenceCriteria(Duration.ofSeconds(10), 11);
        assertThat(options.hashCode()).isNotEqualTo(options2.hashCode());
        assertThat(options.equals(options2)).isFalse();
    }
}