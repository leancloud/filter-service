package cn.leancloud.filter.service;

import cn.leancloud.filter.service.Bootstrap.ParseCommandLineArgsResult;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import picocli.CommandLine.ExitCode;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class BootstrapTest {

    @Test
    public void testStartStop() throws Exception {
        final String configFilePath = "src/test/resources/testing-configuration.yaml";
        final ServerOptions opts = new ServerOptions(configFilePath, 8080, false);
        final Bootstrap bootstrap = new Bootstrap(opts);
        bootstrap.start(true);

        bootstrap.stop();
        FileUtils.forceDelete(Paths.get("lock").toFile());
    }

    @Test
    public void testHelp() {
        final String[] args = new String[]{"-h"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.OK);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testHelp2() {
        final String[] args = new String[]{"--help"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.OK);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testVersion() {
        final String[] args = new String[]{"-V"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.OK);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testVersion2() {
        final String[] args = new String[]{"--version"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.OK);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testUnknownArgument() {
        final String[] args = new String[]{"-unknown"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.USAGE);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testInvalidPort() {
        String[] args = new String[]{"-p", "wahaha"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.USAGE);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testInvalidPort2() {
        String[] args = new String[]{"--port", "wahaha"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.USAGE);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testInvalidEnableDocService() {
        String[] args = new String[]{"-d", "wahaha"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isTrue();
        assertThat(ret.getExitCode()).isEqualTo(ExitCode.USAGE);
        assertThat(ret.getOptions()).isNull();
    }

    @Test
    public void testArgsInAbbreviationForm() {
        String[] args = new String[]{"-d", "-c", "path/to/config", "-p", "8080"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isFalse();

        assertThat(ret.getExitCode()).isEqualTo(ExitCode.OK);
        ServerOptions options = ret.getOptions();
        assertThat(options).isNotNull();
        assertThat(options.docServiceEnabled()).isTrue();
        assertThat(options.port()).isEqualTo(8080);
        assertThat(options.configFilePath()).isEqualTo("path/to/config");
    }

    @Test
    public void testArgsInFullForm() {
        String[] args = new String[]{"--enable-doc-service", "--configuration-file", "path/to/config", "--port", "8080"};
        ParseCommandLineArgsResult ret = Bootstrap.parseCommandLineArgs(args);
        assertThat(ret.isExit()).isFalse();

        assertThat(ret.getExitCode()).isEqualTo(ExitCode.OK);
        ServerOptions options = ret.getOptions();
        assertThat(options).isNotNull();
        assertThat(options.docServiceEnabled()).isTrue();
        assertThat(options.port()).isEqualTo(8080);
        assertThat(options.configFilePath()).isEqualTo("path/to/config");
    }
}