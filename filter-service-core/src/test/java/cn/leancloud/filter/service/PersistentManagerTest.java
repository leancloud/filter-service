package cn.leancloud.filter.service;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static cn.leancloud.filter.service.TestingUtils.generateFilterRecords;
import static cn.leancloud.filter.service.TestingUtils.generateInvalidFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class PersistentManagerTest {
    private BloomFilterFactory factory;
    private Path tempDirPath;
    private BloomFilterManager filterManager;
    private PersistentManager<BloomFilter> manager;

    @Before
    public void setUp() throws Exception {
        final String tempDir = System.getProperty("java.io.tmpdir", "/tmp") +
                File.separator + "filter_service_" + System.nanoTime();
        tempDirPath = Paths.get(tempDir);
        FileUtils.forceMkdir(tempDirPath.toFile());
        filterManager = mock(BloomFilterManager.class);
        factory = mock(GuavaBloomFilterFactory.class);
        when(factory.readFrom(any())).thenCallRealMethod();
        manager = new PersistentManager<>(tempDirPath);
    }

    @After
    public void tearDown() throws Exception {
        manager.close();
    }

    @Test
    public void testPersistentDirIsFile() throws Exception {
        FileChannel.open(tempDirPath.resolve("plain_file"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        assertThatThrownBy(() -> new PersistentManager<>(tempDirPath.resolve("plain_file")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid persistent directory path, it's a regular file");
    }

    @Test
    public void testLockAndReleaseLock() throws Exception {
        final Path lockPath = tempDirPath.resolve("lock_path");
        final PersistentManager<BloomFilter> manager = new PersistentManager<>(lockPath);

        assertThatThrownBy(() -> new PersistentManager<>(lockPath))
                .isInstanceOf(OverlappingFileLockException.class);

        manager.close();

        new PersistentManager<>(lockPath);
    }

    @Test
    public void testMakeBaseDir() throws Exception {
        final Path newPath = tempDirPath.resolve("base_dir");
        assertThat(newPath.toFile().exists()).isFalse();
        new PersistentManager<>(newPath);
        assertThat(newPath.toFile().exists()).isTrue();
    }

    @Test
    public void freezeAllFilters() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters(filterManager);

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(
                manager.persistentFilePath(),
                new GuavaBloomFilterFactory())) {
            for (FilterRecord<BloomFilter> expectRecord : records) {
                assertThat(stream.nextFilterRecord()).isEqualTo(expectRecord);
            }

            assertThat(stream.nextFilterRecord()).isNull();
        }
    }

    @Test
    public void testNoFilesToRecover() throws IOException {
        manager.recoverFilters(factory, true);
        verify(filterManager, never()).addFilters(anyCollection());
    }

    @Test
    public void testRecoverFiltersFromFileNormalCase() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters(filterManager);
        assertThat(manager.recoverFilters(factory, false)).isEqualTo(records);
    }

    @Test
    public void testRecoverOnlyValidFiltersFromFile() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        final BloomFilter invalidFilter = generateInvalidFilter();
        records.add(new FilterRecord("Invalid_Filter", invalidFilter));
        when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters(filterManager);
        assertThat(manager.recoverFilters(factory, false))
                .isEqualTo(records.subList(0, records.size() - 1));
    }

    @Test
    public void testDoNotAllowRecoverFromCorruptedFile() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());
        manager.freezeAllFilters(filterManager);
        try (FileChannel channel = FileChannel.open(manager.persistentFilePath(), StandardOpenOption.WRITE)) {
            channel.truncate(channel.size() - 1);

            assertThatThrownBy(() -> manager.recoverFilters(factory, false))
                    .hasMessageContaining("failed to recover filters from:")
                    .isInstanceOf(PersistentStorageException.class);
        }
    }

    @Test
    public void testAllowRecoverFromCorruptedFile() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());
        manager.freezeAllFilters(filterManager);
        try (FileChannel channel = FileChannel.open(manager.persistentFilePath(), StandardOpenOption.WRITE)) {
            channel.truncate(channel.size() - 1);

            assertThat(manager.recoverFilters(factory, true))
                    .isEqualTo(records.subList(0, records.size() - 1));
        }
    }

    @Test
    public void testRecoverOnlyFromTemporaryFile() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters(filterManager);
        FilterServiceFileUtils.atomicMoveWithFallback(manager.persistentFilePath(), manager.temporaryPersistentFilePath());

        assertThat(manager.recoverFilters(factory, false)).isEqualTo(records);
    }

    @Test
    public void testRecoverFromTemporaryFileAndNormalFile() throws IOException {
        final Path temporaryPath = manager.persistentFilePath().resolveSibling("tmp.bak");
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(20);
        final List<FilterRecord<BloomFilter>> expected = new ArrayList<>();
        expected.addAll(records);
        expected.addAll(records.subList(10, 20));

        // prepare temporary file
        when(filterManager.iterator()).thenReturn(records.subList(10, 20).iterator());
        manager.freezeAllFilters(filterManager);
        FilterServiceFileUtils.atomicMoveWithFallback(manager.persistentFilePath(), temporaryPath);

        // prepare normal file
        when(filterManager.iterator()).thenReturn(records.iterator());
        manager.freezeAllFilters(filterManager);

        FilterServiceFileUtils.atomicMoveWithFallback(temporaryPath, manager.temporaryPersistentFilePath());
        assertThat(manager.recoverFilters(factory, false)).isEqualTo(expected);
    }

    @Test
    public void testRecoverFromTemporaryFileFailed() throws IOException {
        final Path temporaryPath = manager.persistentFilePath().resolveSibling("tmp.bak");
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(20);

        // prepare temporary file
        when(filterManager.iterator()).thenReturn(generateFilterRecords(50, 20).iterator());
        manager.freezeAllFilters(filterManager);
        FilterServiceFileUtils.atomicMoveWithFallback(manager.persistentFilePath(), temporaryPath);

        // prepare normal file
        when(filterManager.iterator()).thenReturn(records.iterator());
        manager.freezeAllFilters(filterManager);

        FilterServiceFileUtils.atomicMoveWithFallback(temporaryPath, manager.temporaryPersistentFilePath());
        try (FileChannel channel = FileChannel.open(manager.temporaryPersistentFilePath(), StandardOpenOption.WRITE)) {
            channel.truncate(channel.size() - 1);
            assertThat(manager.recoverFilters(factory, false)).isEqualTo(records);
        }
    }
}