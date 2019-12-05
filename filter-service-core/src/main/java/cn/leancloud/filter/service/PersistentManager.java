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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PersistentManager<F extends BloomFilter> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(PersistentManager.class);
    private static final String LOCK_FILE_NAME = "lock";
    private static final String TEMPORARY_PERSISTENT_FILE_SUFFIX = ".tmp";
    private static final String PERSISTENT_FILE_SUFFIX = ".db";
    private static final String PERSISTENT_FILE_NAME = "snapshot";

    private final Path basePath;
    private final FileLock fileLock;

    public PersistentManager(Path persistentPath)
            throws IOException {
        final File dir = persistentPath.toFile();
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalStateException("invalid persistent directory path, it's a regular file: " + persistentPath);
        }

        FileUtils.forceMkdir(persistentPath.toFile());

        this.fileLock = FileUtils.lockDirectory(persistentPath, LOCK_FILE_NAME);
        this.basePath = persistentPath;
    }

    public synchronized void freezeAllFilters(BloomFilterManager<F, ?> bloomFilterManager) throws IOException {
        final Path tempPath = temporaryPersistentFilePath();
        int counter = 0;
        try (FileChannel channel = FileChannel.open(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            for (FilterRecord<F> record : bloomFilterManager) {
                record.writeFullyTo(channel);
                counter++;
            }

            channel.force(true);
        }

        FileUtils.atomicMoveWithFallback(tempPath, persistentFilePath());
        logger.debug("Persistent " + counter + " filters.");
    }

    public synchronized List<FilterRecord<? extends F>> recoverFiltersFromFile(BloomFilterFactory<F, ?> factory,
                                                                               boolean allowRecoverFromCorruptedFile)
            throws IOException {
        if (persistentFilePath().toFile().exists()) {
            final List<FilterRecord<? extends F>> records = new ArrayList<>();
            try {
                try (FilterRecordInputStream<F> filterStream = new FilterRecordInputStream<>(persistentFilePath(), factory)) {
                    readFiltersFromFile(filterStream)
                            .forEach(r -> {
                                if (r.filter().valid()) {
                                    records.add(r);
                                }
                            });
                }
                logger.info("Recovered " + records.size() + " filters from: " + persistentFilePath());
                return records;
            } catch (InvalidFilterException ex) {
                if (allowRecoverFromCorruptedFile) {
                    logger.warn("Recover " + records.size() + " filters from corrupted file:" + persistentFilePath() +
                            ". The exception captured as follows:", ex);
                    return records;
                } else {
                    throw new PersistentStorageException("failed to recover filters from: " + persistentFilePath(), ex);
                }
            }
        } else {
            logger.info("By pass recover filters from persistent file due to no persistent file exists under path: " + basePath);
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        FileUtils.releaseDirectoryLock(fileLock);
    }

    private Path temporaryPersistentFilePath() {
        return basePath.resolve(PERSISTENT_FILE_NAME + TEMPORARY_PERSISTENT_FILE_SUFFIX);
    }

    // Package private for testing
    Path persistentFilePath() {
        return basePath.resolve(PERSISTENT_FILE_NAME + PERSISTENT_FILE_SUFFIX);
    }

    private Iterable<FilterRecord<? extends F>> readFiltersFromFile(FilterRecordInputStream<F> filterStream) {
        return () -> new AbstractIterator<FilterRecord<? extends F>>() {
            @Nullable
            @Override
            protected FilterRecord<? extends F> makeNext() {
                try {
                    final FilterRecord<? extends F> record = filterStream.nextFilterRecord();
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
}
