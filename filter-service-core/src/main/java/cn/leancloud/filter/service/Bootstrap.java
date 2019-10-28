package cn.leancloud.filter.service;

import cn.leancloud.filter.service.metrics.MetricsService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.linecorp.armeria.common.metric.MeterIdPrefixFunction;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.metric.MetricCollectingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        final var ret = parseCommandLineArgs(args);
        if (ret.isExit()) {
            System.exit(ret.getExitCode());
            return;
        }

        final var opts = ret.getOptions();
        assert opts != null;

        final var scheduledExecutorService = Executors.newScheduledThreadPool(10,
                new ThreadFactoryBuilder().setNameFormat("scheduled-worker-%s").build());
        final var bloomFilterManager = newBloomFilterManager();
        final var purgeFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                bloomFilterManager.purge();
            } catch (Exception ex) {
                logger.error("Purge bloom filter service failed.", ex);
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
        final var metricsService = loadMetricsService();
        final var registry = metricsService.createMeterRegistry();
        metricsService.start();
        final var server = newServer(registry, opts, bloomFilterManager);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            purgeFuture.cancel(false);

            final var shutdownFuture = new CompletableFuture<>();
            scheduledExecutorService.execute(() ->
                    shutdownFuture.complete(null)
            );
            shutdownFuture.join();
            scheduledExecutorService.shutdown();

            metricsService.stop();

            logger.info("Filter server has been stopped.");
        }));

        server.start().join();

        logger.info("Filter server has been started at port {}", opts.getHttpPort());
    }

    private static ParseCommandLineArgsResult parseCommandLineArgs(String[] args) {
        final var opts = new ServerOptions();
        final var cli = new CommandLine(opts);
        try {
            cli.parseArgs(args);

            if (cli.isUsageHelpRequested()) {
                cli.usage(cli.getOut());
                return new ParseCommandLineArgsResult(cli.getCommandSpec().exitCodeOnUsageHelp());
            } else if (cli.isVersionHelpRequested()) {
                cli.printVersionHelp(cli.getOut());
                return new ParseCommandLineArgsResult(cli.getCommandSpec().exitCodeOnVersionHelp());
            }
        } catch (ParameterException ex) {
            cli.getErr().println(ex.getMessage());
            if (!UnmatchedArgumentException.printSuggestions(ex, cli.getErr())) {
                ex.getCommandLine().usage(cli.getErr());
            }
            return new ParseCommandLineArgsResult(cli.getCommandSpec().exitCodeOnInvalidInput());
        } catch (Exception ex) {
            ex.printStackTrace(cli.getErr());
            return new ParseCommandLineArgsResult(cli.getCommandSpec().exitCodeOnExecutionException());
        }
        return new ParseCommandLineArgsResult(opts);
    }

    private static BloomFilterManagerImpl<GuavaBloomFilter> newBloomFilterManager() {
        final GuavaBloomFilterFactory factory = new GuavaBloomFilterFactory();
        final BloomFilterManagerImpl<GuavaBloomFilter> bloomFilterManager = new BloomFilterManagerImpl<>(factory);
        bloomFilterManager.addListener(new BloomFilterManagerListener<>() {
            @Override
            public void onBloomFilterCreated(ExpirableBloomFilterConfig config, GuavaBloomFilter filter) {
                logger.info("Bloom filter with name: {} was created.", filter.name());
            }

            @Override
            public void onBloomFilterRemoved(GuavaBloomFilter filter) {
                if (filter.expired()) {
                    logger.info("Bloom filter with name: {} was purged due to expiration.", filter.name());
                } else {
                    logger.info("Bloom filter with name: {} was removed.", filter.name());
                }
            }
        });
        return bloomFilterManager;
    }

    private static MetricsService loadMetricsService() {
        final var loader = ServiceLoader.load(MetricsService.class);
        final var optService = loader.findFirst();
        return optService.orElseGet(DefaultMetricsService::new);
    }

    private static Server newServer(MeterRegistry registry,
                                    ServerOptions opts,
                                    BloomFilterManager<?, ? super ExpirableBloomFilterConfig> bloomFilterManager) {
        final ServerBuilder sb = Server.builder()
                .channelOption(ChannelOption.SO_BACKLOG, 2048)
                .channelOption(ChannelOption.SO_RCVBUF, 2048)
                .childChannelOption(ChannelOption.SO_SNDBUF, 2048)
                .childChannelOption(ChannelOption.TCP_NODELAY, true)
                .http(opts.getHttpPort())
                .maxNumConnections(1000)
                .maxRequestLength(2048)  // 2KB
                .requestTimeout(Duration.ofSeconds(5))
                .meterRegistry(registry);

        sb.annotatedService("/bloomfilter", new BloomFilterHttpService(bloomFilterManager))
                .decorator(MetricCollectingService.newDecorator(MeterIdPrefixFunction.ofDefault("filter-service")));
        if (opts.isDocServiceEnabled()) {
            sb.serviceUnder("/docs", new DocService());
        }
        return sb.build();
    }

    private static class ParseCommandLineArgsResult {
        private final int exitCode;
        private final boolean exit;
        @Nullable
        private final ServerOptions options;

        ParseCommandLineArgsResult(int exitCode) {
            this.exitCode = exitCode;
            this.exit = true;
            this.options = null;
        }

        ParseCommandLineArgsResult(ServerOptions options) {
            this.exitCode = 0;
            this.exit = false;
            this.options = options;
        }

        int getExitCode() {
            return exitCode;
        }

        boolean isExit() {
            return exit;
        }

        @Nullable
        ServerOptions getOptions() {
            return options;
        }
    }
}
