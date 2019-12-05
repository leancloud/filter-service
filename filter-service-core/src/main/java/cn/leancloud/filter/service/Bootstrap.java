package cn.leancloud.filter.service;

import cn.leancloud.filter.service.metrics.MetricsService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.linecorp.armeria.common.metric.MeterIdPrefixFunction;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.metric.MetricCollectingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelOption;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.*;

public final class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) throws Exception {
        final ParseCommandLineArgsResult ret = parseCommandLineArgs(args);
        if (ret.isExit()) {
            System.exit(ret.getExitCode());
            return;
        }

        final ServerOptions opts = ret.getOptions();
        assert opts != null;

        if (opts.configFilePath() != null) {
            Configuration.initConfiguration(opts.configFilePath());
        }

        final Bootstrap bootstrap = new Bootstrap(opts);
        bootstrap.start();

        Runtime.getRuntime().addShutdownHook(new Thread(bootstrap::stop));
    }

    private static ParseCommandLineArgsResult parseCommandLineArgs(String[] args) {
        final ServerOptions opts = new ServerOptions();
        final CommandLine cli = new CommandLine(opts);
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

    private final ServerOptions opts;
    private final MetricsService metricsService;
    private final MeterRegistry registry;
    private final ScheduledExecutorService scheduledExecutorService;
    private final GuavaBloomFilterFactory factory;
    private final BloomFilterManagerImpl<GuavaBloomFilter, ExpirableBloomFilterConfig> bloomFilterManager;
    private final PersistentManager<GuavaBloomFilter> persistentManager;
    private final Server server;
    private List<ScheduledFuture<?>> scheduledFutures;

    public Bootstrap(ServerOptions opts) throws Exception {
        this.opts = opts;
        this.metricsService = loadMetricsService();
        this.scheduledExecutorService = Executors.newScheduledThreadPool(10,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-worker-%s")
                        .setUncaughtExceptionHandler((t, e) ->
                                logger.error("Scheduled worker thread: " + t.getName() + " got uncaught exception.", e))
                        .build());
        this.registry = metricsService.createMeterRegistry();
        this.factory = new GuavaBloomFilterFactory();
        this.bloomFilterManager = newBloomFilterManager(factory);
        this.persistentManager = new PersistentManager<>(Paths.get(Configuration.persistentStorageDirectory()));
        this.server = newServer(registry, opts, bloomFilterManager);
    }

    private void start() throws Exception {
        recoverPreviousBloomFilters();

        scheduledFutures = schedulePeriodJobs();
        metricsService.start();
        server.start().join();
        logger.info("Filter server has been started at port {} with configurations: {}",
                opts.getPort(), Configuration.spec());
    }

    private void stop() {
        try {
            server.stop().join();

            for (ScheduledFuture<?> future : scheduledFutures) {
                future.cancel(false);
            }

            final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
            scheduledExecutorService.execute(() ->
                    shutdownFuture.complete(null)
            );
            shutdownFuture.join();
            scheduledExecutorService.shutdown();

            metricsService.stop();
            persistentManager.close();
            logger.info("Filter service has been stopped.");
        } catch (Exception ex) {
            logger.info("Got unexpected exception during shutdown filter service, exit anyway.", ex);
        } finally {
            LogManager.shutdown();
        }
    }

    private static MetricsService loadMetricsService() {
        final ServiceLoader<MetricsService> loader = ServiceLoader.load(MetricsService.class);
        final Iterator<MetricsService> iterator = loader.iterator();
        if (iterator.hasNext()) {
            final MetricsService service = iterator.next();
            logger.info("Load {} as implementation for {}.",
                    service.getClass().getName(), MetricsService.class.getName());
            return service;
        } else {
            logger.info("Using {} to record metrics", DefaultMetricsService.class.getName());
            return new DefaultMetricsService();
        }
    }

    private void recoverPreviousBloomFilters() throws IOException {
        final List<FilterRecord<? extends GuavaBloomFilter>> records =
                persistentManager.recoverFiltersFromFile(factory, Configuration.allowRecoverFromCorruptedPersistentFile());
        bloomFilterManager.addFilters(records);
    }

    private List<ScheduledFuture<?>> schedulePeriodJobs() {
        final Timer persistentFiltersTimer = registry.timer("filter-service.persistentFilters");
        final Timer purgeExpiredFiltersTimer = registry.timer("filter-service.purgeExpiredFilters");
        final List<ScheduledFuture<?>> futures = new ArrayList<>();
        final InvalidBloomFilterPurgatory<GuavaBloomFilter> purgatory
                = new InvalidBloomFilterPurgatory<>(bloomFilterManager);
        futures.add(scheduledExecutorService.scheduleWithFixedDelay(purgeExpiredFiltersTimer.wrap(() -> {
            try {
                purgatory.purge();
            } catch (Throwable ex) {
                logger.error("Purge bloom filter service failed.", ex);
                throw ex;
            }
        }), 0, Configuration.purgeFilterInterval().toMillis(), TimeUnit.MILLISECONDS));

        futures.add(scheduledExecutorService.scheduleWithFixedDelay(persistentFiltersTimer.wrap(() -> {
            try {
                persistentManager.freezeAllFilters(bloomFilterManager);
            } catch (IOException ex) {
                logger.error("Persistent bloom filters failed.", ex);
            } catch (Throwable t) {
                // sorry for the duplication, but currently I don't figure out another way
                // to catch the direct buffer OOM when freeze filters to file
                logger.error("Persistent bloom filters failed.", t);
                throw t;
            }

        }), 0, Configuration.persistentFiltersInterval().toMillis(), TimeUnit.MILLISECONDS));

        return futures;
    }

    private Server newServer(MeterRegistry registry,
                             ServerOptions opts,
                             BloomFilterManager<?, ? super ExpirableBloomFilterConfig> bloomFilterManager) {
        final ServerBuilder sb = Server.builder()
                .channelOption(ChannelOption.SO_BACKLOG, Configuration.channelOptions().SO_BACKLOG())
                .channelOption(ChannelOption.SO_RCVBUF, Configuration.channelOptions().SO_RCVBUF())
                .childChannelOption(ChannelOption.SO_SNDBUF, Configuration.channelOptions().SO_SNDBUF())
                .childChannelOption(ChannelOption.TCP_NODELAY, Configuration.channelOptions().TCP_NODELAY())
                .http(opts.getPort())
                .maxNumConnections(Configuration.maxHttpConnections())
                .maxRequestLength(Configuration.maxHttpRequestLength())
                .requestTimeout(Configuration.defaultRequestTimeout())
                .meterRegistry(registry);

        sb.annotatedService("/v1/bloomfilter", new BloomFilterHttpService(bloomFilterManager))
                .decorator(MetricCollectingService.newDecorator(MeterIdPrefixFunction.ofDefault("filter-service")));
        if (opts.isDocServiceEnabled()) {
            sb.serviceUnder("/docs", new DocService());
        }
        return sb.build();
    }

    private BloomFilterManagerImpl<GuavaBloomFilter, ExpirableBloomFilterConfig> newBloomFilterManager(GuavaBloomFilterFactory factory) {
        final BloomFilterManagerImpl<GuavaBloomFilter, ExpirableBloomFilterConfig> bloomFilterManager = new BloomFilterManagerImpl<>(factory);
        bloomFilterManager.addListener(new BloomFilterManagerListener<GuavaBloomFilter, ExpirableBloomFilterConfig>() {
            @Override
            public void onBloomFilterCreated(String name, ExpirableBloomFilterConfig config, GuavaBloomFilter filter) {
                logger.info("Bloom filter with name: {} was created.", name);
            }

            @Override
            public void onBloomFilterRemoved(String name, GuavaBloomFilter filter) {
                if (filter.expired()) {
                    logger.info("Bloom filter with name: {} was purged due to expiration.", name);
                } else {
                    logger.info("Bloom filter with name: {} was removed.", name);
                }
            }
        });
        return bloomFilterManager;
    }
}
