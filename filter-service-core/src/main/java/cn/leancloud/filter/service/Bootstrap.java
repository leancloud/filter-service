package cn.leancloud.filter.service;

import cn.leancloud.filter.service.Configuration.TriggerPersistenceCriteria;
import cn.leancloud.filter.service.metrics.MetricsService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.metric.MeterIdPrefixFunction;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.metric.MetricCollectingService;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

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

    static ParseCommandLineArgsResult parseCommandLineArgs(String[] args) {
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

    static class ParseCommandLineArgsResult {
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

    private final MetricsService metricsService;
    private final BackgroundJobScheduler scheduler;
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private final CountUpdateBloomFilterFactory<ExpirableBloomFilterConfig> factory;
    private final BloomFilterManagerImpl<BloomFilter, ExpirableBloomFilterConfig> bloomFilterManager;
    private final PersistentManager<BloomFilter> persistentManager;
    private final Server server;

    public Bootstrap(ServerOptions opts) throws Exception {
        this.metricsService = loadMetricsService();
        final MeterRegistry registry = metricsService.createMeterRegistry();
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                Configuration.maxWorkerThreadPoolSize(),
                new ThreadFactoryBuilder()
                        .setNameFormat("filter-service-worker-%s")
                        .setUncaughtExceptionHandler((t, e) ->
                                logger.error("Worker thread: " + t.getName() + " got uncaught exception.", e))
                        .build());
        scheduledThreadPoolExecutor.setKeepAliveTime(300, TimeUnit.SECONDS);
        scheduledThreadPoolExecutor.allowCoreThreadTimeOut(true);

        this.scheduler = new BackgroundJobScheduler(registry, scheduledThreadPoolExecutor);
        this.factory = new CountUpdateBloomFilterFactory<>(new GuavaBloomFilterFactory(), new LongAdder());
        this.persistentManager = new PersistentManager<>(Paths.get(Configuration.persistentStorageDirectory()));
        this.bloomFilterManager = newBloomFilterManager();
        this.server = newServer(registry, opts, scheduledThreadPoolExecutor);
    }

    void start(boolean forTesting) throws Exception {
        recoverPreviousBloomFilters();

        scheduler.scheduleFixedIntervalJob(
                new PurgeFiltersJob(new InvalidBloomFilterPurgatory<>(bloomFilterManager)),
                "purgeExpiredFilters",
                Configuration.purgeFilterInterval());

        for (TriggerPersistenceCriteria criteria : Configuration.persistenceCriteria()) {
            scheduler.scheduleFixedIntervalJob(
                    new PersistentFiltersJob<>(bloomFilterManager, persistentManager, factory.filterUpdateTimesCounter(), criteria),
                    "persistentFilters",
                    criteria.checkingPeriod()
            );
        }

        metricsService.start();

        if (!forTesting) {
            server.start().join();
        }
        logger.info("Filter server has been started with configurations: {}", Configuration.spec());
    }

    void stop() {
        try {
            server.stop().join();

            scheduler.stop();
            scheduledThreadPoolExecutor.shutdown();
            scheduledThreadPoolExecutor.awaitTermination(1, TimeUnit.DAYS);

            metricsService.stop();
            persistentManager.close();
            logger.info("Filter service has been stopped.");
        } catch (Exception ex) {
            logger.info("Got unexpected exception during shutdown filter service, exit anyway.", ex);
        } finally {
            LogManager.shutdown();
        }
    }

    private void start() throws Exception {
        start(false);
    }

    private MetricsService loadMetricsService() {
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
        final List<FilterRecord<? extends BloomFilter>> records =
                persistentManager.recoverFilters(factory, Configuration.allowRecoverFromCorruptedPersistentFile());
        bloomFilterManager.addFilters(records);
    }

    private Server newServer(MeterRegistry registry, ServerOptions opts, ScheduledExecutorService scheduledExecutorService) {
        final ServerBuilder sb = Server.builder()
                .channelOption(ChannelOption.SO_BACKLOG, Configuration.channelOptions().SO_BACKLOG())
                .channelOption(ChannelOption.SO_RCVBUF, Configuration.channelOptions().SO_RCVBUF())
                .childChannelOption(ChannelOption.SO_SNDBUF, Configuration.channelOptions().SO_SNDBUF())
                .childChannelOption(ChannelOption.TCP_NODELAY, Configuration.channelOptions().TCP_NODELAY())
                .http(opts.port())
                .maxNumConnections(Configuration.maxHttpConnections())
                .maxRequestLength(Configuration.maxHttpRequestLength())
                .requestTimeout(Configuration.requestTimeout())
                .disableDateHeader()
                .disableServerHeader()
                .blockingTaskExecutor(scheduledExecutorService, false)
                .gracefulShutdownTimeoutMillis(Configuration.gracefulShutdownQuietPeriodMillis(), Configuration.gracefulShutdownTimeoutMillis())
                .idleTimeoutMillis(Configuration.idleTimeoutMillis())
                .meterRegistry(registry);

        sb.service("/v1/ping", (ctx, req) -> HttpResponse.of("pong"));
        sb.annotatedService("/v1/bloomfilter", new BloomFilterHttpService(bloomFilterManager))
                .decorator(MetricCollectingService.newDecorator(MeterIdPrefixFunction.ofDefault(Configuration.metricsPrefix())));
        if (opts.docServiceEnabled()) {
            sb.serviceUnder("/v1/docs", new DocService());
        }
        return sb.build();
    }

    private BloomFilterManagerImpl<BloomFilter, ExpirableBloomFilterConfig> newBloomFilterManager() {
        final BloomFilterManagerImpl<BloomFilter, ExpirableBloomFilterConfig> bloomFilterManager = new BloomFilterManagerImpl<>(factory);
        bloomFilterManager.addListener(new BloomFilterManagerListener<BloomFilter, ExpirableBloomFilterConfig>() {
            @Override
            public void onBloomFilterCreated(String name, ExpirableBloomFilterConfig config, BloomFilter filter) {
                logger.info("Bloom filter with name: {} was created.", name);
            }

            @Override
            public void onBloomFilterRemoved(String name, BloomFilter filter) {
                if (filter.valid()) {
                    logger.info("Bloom filter with name: {} was purged due to expiration.", name);
                } else {
                    logger.info("Bloom filter with name: {} was removed.", name);
                }
            }
        });
        return bloomFilterManager;
    }
}
