package cn.leancloud.filter.service;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigurationTest {

    @Test
    public void testEnsureInitialization() {
        testEnsureInitializationForMethod(Configuration::purgeFilterInterval);
        testEnsureInitializationForMethod(Configuration::maxHttpConnections);
        testEnsureInitializationForMethod(Configuration::maxHttpRequestLength);
        testEnsureInitializationForMethod(Configuration::defaultRequestTimeout);
        testEnsureInitializationForMethod(Configuration::channelOptions);
        testEnsureInitializationForMethod(Configuration::spec);
    }

    private void testEnsureInitializationForMethod(ThrowingCallable shouldRaiseThrowable){
        assertThatThrownBy(shouldRaiseThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configuration is not initialized");
    }

    @Test
    public void testNormalConfigurationFile() throws Exception {
        Configuration.initConfiguration("src/test/resources/testing-configuration.yaml");
        assertThat(Configuration.purgeFilterInterval()).isEqualTo(Duration.ofMillis(300));
        assertThat(Configuration.maxHttpConnections()).isEqualTo(1000);
        assertThat(Configuration.maxHttpRequestLength()).isEqualTo(10 * 1024 * 1024);
    }
}