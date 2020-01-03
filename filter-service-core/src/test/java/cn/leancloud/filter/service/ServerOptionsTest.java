package cn.leancloud.filter.service;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerOptionsTest {
    @Test
    public void testServerOptions() {
        final String configFilePath = "/mnt/avos/logs";
        final int port = 10101;
        final boolean docService = true;
        ServerOptions options = new ServerOptions(configFilePath, port, docService);
        assertThat(options.configFilePath()).isEqualTo(configFilePath);
        assertThat(options.port()).isEqualTo(port);
        assertThat(options.docServiceEnabled()).isTrue();
    }

}