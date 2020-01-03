package cn.leancloud.filter.service;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.annotation.Nullable;

@Command(name = "filter-service",
        sortOptions = false,
        showDefaultValues = true,
        version = "filter-service v1.5-SNAPSHOT",
        description = "filter-service is a daemon network service which is used to expose bloom filters " +
                "and operations by RESTFul API.",
        mixinStandardHelpOptions = true)
final class ServerOptions {
    @Option(names = {"-c", "--configuration-file"},
            description = "The path to a YAML configuration file.")
    @Nullable
    private String configFilePath;
    @Option(names = {"-p", "--port"},
            defaultValue = "8080",
            description = "The http/https port on which filter-service is running.")
    private int port;

    @Option(names = {"-d", "--enable-doc-service"},
            defaultValue = "false",
            description = "true when you want to serve the testing document service under path \"/docs\".")
    private boolean docService;

    int getPort() {
        return port;
    }

    boolean isDocServiceEnabled() {
        return docService;
    }

    @Nullable
    String configFilePath() {
        return configFilePath;
    }

    ServerOptions() {

    }

    ServerOptions(@Nullable String configFilePath, int port, boolean docService) {
        this.configFilePath = configFilePath;
        this.port = port;
        this.docService = docService;
    }
}
