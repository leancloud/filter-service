package cn.leancloud.filter.service;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest {
    @Test
    public void testDefaultValues() {
        assertThat(Configuration.purgeFilterInterval()).isEqualTo(Duration.ofMillis(300));
        assertThat(Configuration.maxHttpConnections()).isEqualTo(1000);
        assertThat(Configuration.maxHttpRequestLength()).isEqualTo(10 * 1024 * 1024);
    }

    @Test
    public void testNormalConfigurationFile() throws Exception {
        Configuration.initConfiguration("src/test/resources/testing-configuration.yaml");
        assertThat(Configuration.purgeFilterInterval()).isEqualTo(Duration.ofMillis(300));
        assertThat(Configuration.maxHttpConnections()).isEqualTo(1000);
        assertThat(Configuration.maxHttpRequestLength()).isEqualTo(10 * 1024 * 1024);
    }
}