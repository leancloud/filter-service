package cn.leancloud.filter.service;

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
        manager = new PersistentManager<>(
                filterManager,
                factory,
                tempDirPath
        );
    }

    @After
    public void tearDown() throws Exception {
        manager.close();
    }

    @Test
    public void testPersistentDirIsFile() throws Exception {
        FileChannel.open(tempDirPath.resolve("plain_file"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        assertThatThrownBy(() -> new PersistentManager<>(
                filterManager,
                factory,
                tempDirPath.resolve("plain_file")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid persistent directory path, it's a regular file");
    }

    @Test
    public void testLockAndReleaseLock() throws Exception {
        final Path lockPath = tempDirPath.resolve("lock_path");
        final PersistentManager<BloomFilter> manager = new PersistentManager<>(
                filterManager,
                factory,
                lockPath
        );

        assertThatThrownBy(() -> new PersistentManager<>(
                filterManager,
                factory,
                lockPath
        )).isInstanceOf(OverlappingFileLockException.class);

        manager.close();

        new PersistentManager<>(
                filterManager,
                factory,
                lockPath
        );
    }

    @Test
    public void testMakeBaseDir() throws Exception {
        final Path newPath = tempDirPath.resolve("base_dir");
        assertThat(newPath.toFile().exists()).isFalse();
        new PersistentManager<>(
                filterManager,
                factory,
                newPath
        );
        assertThat(newPath.toFile().exists()).isTrue();
    }

    @Test
    public void freezeAllFilters() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters();

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
    public void testByPassRecovery() throws IOException {
        manager.recoverFiltersFromFile(true);
        verify(filterManager, never()).addFilters(anyCollection());
    }

    @Test
    public void testRecoverFiltersFromFileNormalCase() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters();
        manager.recoverFiltersFromFile(false);

        verify(filterManager, times(1)).addFilters(records);
    }

    @Test
    public void testRecoverOnlyValidFiltersFromFile() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        final BloomFilter invalidFilter = generateInvalidFilter();
        records.add(new FilterRecord("Invalid_Filter", invalidFilter));
        when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters();
        manager.recoverFiltersFromFile(false);

        verify(filterManager, times(1)).addFilters(records.subList(0, records.size() - 1));
    }

    @Test
    public void testDoNotAllowRecoverFromCorruptedFile() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());
        manager.freezeAllFilters();
        try (FileChannel channel = FileChannel.open(manager.persistentFilePath(), StandardOpenOption.WRITE)) {
            channel.truncate(channel.size() - 1);

            assertThatThrownBy(() -> manager.recoverFiltersFromFile(false))
                    .isInstanceOf(PersistentStorageException.class)
                    .hasMessageContaining("failed to recover filters from:");
        }
    }

    @Test
    public void testAllowRecoverFromCorruptedFile() throws IOException {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(10);
        when(filterManager.iterator()).thenReturn(records.iterator());
        manager.freezeAllFilters();
        try (FileChannel channel = FileChannel.open(manager.persistentFilePath(), StandardOpenOption.WRITE)) {
            channel.truncate(channel.size() - 1);

            manager.recoverFiltersFromFile(true);

            verify(filterManager, times(1)).addFilters(records.subList(0, records.size() - 1));
        }
    }
}