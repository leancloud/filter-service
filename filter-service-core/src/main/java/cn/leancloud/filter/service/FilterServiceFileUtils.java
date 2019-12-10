package cn.leancloud.filter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

final class FilterServiceFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FilterServiceFileUtils.class);

    static FileLock lockDirectory(Path baseDir, String lockFileName) throws IOException {
        final Path lockFilePath = baseDir.resolve(lockFileName);
        final FileChannel lockChannel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock fileLock = null;
        try {
            fileLock = lockChannel.tryLock();
            if (fileLock == null) {
                throw new IllegalStateException("failed to lock directory: " + baseDir);
            }
        } finally {
            if (fileLock == null || !fileLock.isValid()) {
                lockChannel.close();
            }
        }

        return fileLock;
    }

    static void releaseDirectoryLock(@Nullable FileLock lock) throws IOException {
        if (lock != null) {
            try (Channel channel = lock.acquiredBy()) {
                if (channel.isOpen()) {
                    lock.release();
                }
            }
        }
    }

    /**
     * Attempts to move source to target atomically and falls back to a non-atomic move if it fails.
     *
     * @throws IOException if both atomic and non-atomic moves fail
     */
    static void atomicMoveWithFallback(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException outer) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Non-atomic move of {} to {} succeeded after atomic move failed due to {}", source, target,
                        outer.getMessage());
            } catch (IOException inner) {
                inner.addSuppressed(outer);
                throw inner;
            }
        }
    }
}
