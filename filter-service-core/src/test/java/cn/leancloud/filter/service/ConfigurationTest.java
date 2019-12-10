package cn.leancloud.filter.service;

import cn.leancloud.filter.service.Configuration.TriggerPersistenceCriteria;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigurationTest {
    @Test
    public void testDefaultValues() {
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
        assertThat(Configuration.allowRecoverFromCorruptedPersistentFile()).isFalse();
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
}