package cn.leancloud.filter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class PersistentManager<F extends BloomFilter> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(PersistentManager.class);
    private static final String LOCK_FILE_NAME = "lock";
    private static final String PERSISTENT_FILE_NAME = "dump.db";

    private final BloomFilterManager<F, ?> manager;
    private final BloomFilterFactory<F, ?> factory;
    private final Path basePath;
    private final FileLock fileLock;

    public PersistentManager(BloomFilterManager<F, ?> manager, BloomFilterFactory<F, ?> factory) throws IOException {
        final Path basePath = Paths.get(Configuration.persistentStorageDirectory());
        final File dir = basePath.toFile();
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalStateException("invalid persistent directory path, it's a regular file: " + basePath);
        }

        FileUtils.forceMkdir(basePath.toFile());

        this.fileLock = FileUtils.lockDirectory(basePath, LOCK_FILE_NAME);
        this.basePath = basePath;
        this.manager = manager;
        this.factory = factory;
    }

    public void freezeAllFilters() throws IOException {
        final Path tempPath = temporaryPersistentFilePath();
        try (FileChannel channel = FileChannel.open(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            for (FilterRecord<F> record : manager) {
                record.writeFullyTo(channel);
            }
        }

        FileUtils.atomicMoveWithFallback(tempPath, persistentFilePath());
    }

    public void recoverFiltersFromFile() throws IOException {
        if (persistentFilePath().toFile().exists()) {
            final List<FilterRecord<? extends F>> records = new ArrayList<>();
            try {
                try (FilterRecordInputStream<F> filterStream = new FilterRecordInputStream<>(persistentFilePath(), factory)) {
                    readFiltersFromFile(filterStream).forEach(records::add);
                }
                manager.addFilters(records);
            } catch (InvalidFilterException ex) {
                if (Configuration.allowRecoverFromCorruptedPersistentFile()) {
                    logger.warn("Recover " + records.size() + " filters from corrupted file:" + persistentFilePath() +
                            ". The exception captured as follows:", ex);
                    manager.addFilters(records);
                } else {
                    throw new PersistentStorageException("failed to recover filters from: " + persistentFilePath(), ex);
                }
            }
        } else {
            logger.info("By pass recover filters from persistent file due to no persistent file exists under path: " + persistentFilePath());
        }
    }

    public Iterable<FilterRecord<? extends F>> readFiltersFromFile(FilterRecordInputStream<F> filterStream) {
        return () -> new AbstractIterator<FilterRecord<? extends F>>() {
            @Nullable
            @Override
            protected FilterRecord<? extends F> makeNext() {
                try {
                    final FilterRecord<F> record = filterStream.nextFilterRecord();
                    if (record == null) {
                        allDone();
                    }
                    return record;
                } catch (IOException ex) {
                    throw new InvalidFilterException("read filter from file:" + persistentFilePath() + " failed", ex);
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        FileUtils.releaseDirectoryLock(fileLock);
    }

    private Path temporaryPersistentFilePath() {
        return basePath.resolve(PERSISTENT_FILE_NAME).resolve(".tmp");
    }

    private Path persistentFilePath() {
        return basePath.resolve(PERSISTENT_FILE_NAME);
    }
}
