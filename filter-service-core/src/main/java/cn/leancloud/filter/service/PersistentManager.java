package cn.leancloud.filter.service;

import cn.leancloud.filter.service.utils.AbstractIterator;
import org.apache.commons.io.FileUtils;
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
import java.util.List;

public class PersistentManager<F extends BloomFilter> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(PersistentManager.class);
    private static final String LOCK_FILE_NAME = "lock";
    private static final String TEMPORARY_PERSISTENT_FILE_SUFFIX = ".tmp";
    private static final String PERSISTENT_FILE_SUFFIX = ".db";
    private static final String PERSISTENT_FILE_NAME = "snapshot";

    private final Path basePath;
    private final FileLock fileLock;

    PersistentManager(Path persistentPath)
            throws IOException {
        final File dir = persistentPath.toFile();
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalStateException("invalid persistent directory path, it's a regular file: " + persistentPath);
        }

        FileUtils.forceMkdir(persistentPath.toFile());

        this.fileLock = FilterServiceFileUtils.lockDirectory(persistentPath, LOCK_FILE_NAME);
        this.basePath = persistentPath;
    }

    synchronized void freezeAllFilters(Iterable<FilterRecord<F>> records) throws IOException {
        final Path tempPath = temporaryPersistentFilePath();
        int counter = 0;
        try (FileChannel channel = FileChannel.open(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            for (FilterRecord<F> record : records) {
                record.writeFullyTo(channel);
                counter++;
            }

            channel.force(true);
        }

        FilterServiceFileUtils.atomicMoveWithFallback(tempPath, persistentFilePath());
        logger.debug("Persistent " + counter + " filters.");
    }

    synchronized List<FilterRecord<? extends F>> recoverFilters(BloomFilterFactory<? extends F, ?> factory,
                                                                boolean allowRecoverFromCorruptedFile)
            throws IOException {
        final List<FilterRecord<? extends F>> records = new ArrayList<>();
        if (persistentFilePath().toFile().exists()) {
            records.addAll(recoverFiltersFromFile(factory, allowRecoverFromCorruptedFile, persistentFilePath()));
        }

        if (temporaryPersistentFilePath().toFile().exists()) {
            try {
                records.addAll(recoverFiltersFromFile(factory, allowRecoverFromCorruptedFile, temporaryPersistentFilePath()));
            } catch (IOException | PersistentStorageException ex) {
                logger.warn("Failed to recover from the file: \"{}\" and got error msg: \"{}\". We just ignore it and carry on.",
                        temporaryPersistentFilePath(), ex.getMessage());
            }
        }

        return records;
    }

    @Override
    public synchronized void close() throws IOException {
        FilterServiceFileUtils.releaseDirectoryLock(fileLock);
    }

    // Package private for testing
    Path temporaryPersistentFilePath() {
        return basePath.resolve(PERSISTENT_FILE_NAME + TEMPORARY_PERSISTENT_FILE_SUFFIX);
    }

    // Package private for testing
    Path persistentFilePath() {
        return basePath.resolve(PERSISTENT_FILE_NAME + PERSISTENT_FILE_SUFFIX);
    }

    private List<FilterRecord<? extends F>> recoverFiltersFromFile(BloomFilterFactory<? extends F, ?> factory,
                                                                   boolean allowRecoverFromCorruptedFile,
                                                                   Path filePath) throws IOException {
        final List<FilterRecord<? extends F>> records = new ArrayList<>();
        try {
            try (FilterRecordInputStream<? extends F> filterStream = new FilterRecordInputStream<>(filePath, factory)) {
                readFiltersFromFile(filterStream)
                        .forEach(r -> {
                            if (r.filter().valid()) {
                                records.add(r);
                            }
                        });
            }
            logger.info("Recovered " + records.size() + " filters from: " + filePath);
            return records;
        } catch (InvalidFilterException ex) {
            if (allowRecoverFromCorruptedFile) {
                logger.warn("Recover " + records.size() + " filters from corrupted file:" + filePath +
                        ". The exception captured as follows:", ex);
                return records;
            } else {
                throw new PersistentStorageException("failed to recover filters from: " + filePath, ex);
            }
        }
    }

    private Iterable<FilterRecord<? extends F>> readFiltersFromFile(FilterRecordInputStream<? extends F> filterStream) {
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
