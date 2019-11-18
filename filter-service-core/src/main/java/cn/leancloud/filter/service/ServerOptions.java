package cn.leancloud.filter.service;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "filter-service",
        sortOptions = false,
        showDefaultValues = true,
        version = "filter-service v1.2",
        description = "filter-service is a daemon network service which is used to expose bloom filters " +
                "and operations by RESTFul API.",
        mixinStandardHelpOptions = true)
final class ServerOptions {
    @Option(names = {"-c", "--configuration-file"},
            description = "The path to a YAML configuration file.")
    private String configFilePath;
    @Option(names = {"-p", "--http-port"},
            defaultValue = "8080",
            description = "The http port on which filter-service is running.")
    private int httpPort;

    @Option(names = {"-d", "--enable-doc-service"},
            defaultValue = "false",
            description = "true when you want to serve the testing document service under path \"/docs\".")
    private boolean docService;

    public int getHttpPort() {
        return httpPort;
    }

    public boolean isDocServiceEnabled() {
        return docService;
    }

    public String configFilePath() {
        return configFilePath;
    }
}
