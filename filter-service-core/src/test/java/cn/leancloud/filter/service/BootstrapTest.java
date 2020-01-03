package cn.leancloud.filter.service;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class BootstrapTest {

    @After
    public void tearDown() throws Exception {
        FileUtils.forceDelete(Paths.get("lock").toFile());
    }

    @Test
    public void testStartStop() throws Exception {
        final String configFilePath = "src/test/resources/testing-configuration.yaml";
        final ServerOptions opts = new ServerOptions(configFilePath, 8080, false);
        final Bootstrap bootstrap = new Bootstrap(opts);
        bootstrap.start();

        bootstrap.stop();
    }
}