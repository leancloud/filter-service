package cn.leancloud.filter.service;

import net.bytebuddy.dynamic.ClassFileLocator.Resolution.Illegal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.leancloud.filter.service.PersistentManager.PERSISTENT_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;

public class PersistentManagerTest {
    private final Duration validPeriodAfterAccess = Duration.ofSeconds(3);
    private final int expectedInsertions = 1000000;
    private final double fpp = 0.0001;
    private final String testingFilterName = "testing_filter";

    private Path tempDirPath;
    private BloomFilterManager filterManager;
    private BloomFilterFactory factory;
    private PersistentManager<BloomFilter> manager;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final String tempDir = System.getProperty("java.io.tmpdir", "/tmp") +
                File.separator + "filter_service_" + System.nanoTime();
        tempDirPath = Paths.get(tempDir);
        FileUtils.forceMkdir(tempDirPath.toFile());
        filterManager = Mockito.mock(BloomFilterManager.class);
        factory = Mockito.mock(BloomFilterFactory.class);
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
        final List<FilterRecord<GuavaBloomFilter>> records = generateFilterRecords(10);
        Mockito.when(filterManager.iterator()).thenReturn(records.iterator());

        manager.freezeAllFilters();

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(
                tempDirPath.resolve(PERSISTENT_FILE_NAME),
                new GuavaBloomFilterFactory())) {
            for (FilterRecord<GuavaBloomFilter> expectRecord : records) {
                assertThat(stream.nextFilterRecord()).isEqualTo(expectRecord);
            }

            assertThat(stream.nextFilterRecord()).isNull();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testByPassRecovery() throws IOException{
        manager.recoverFiltersFromFile(true);
        Mockito.verify(filterManager, Mockito.never()).addFilters(anyCollection());
    }

    private List<FilterRecord<GuavaBloomFilter>> generateFilterRecords(int size) {
        List<FilterRecord<GuavaBloomFilter>> records = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
            final ZonedDateTime expiration = creation.plus(Duration.ofSeconds(10));
            final GuavaBloomFilter filter = new GuavaBloomFilter(
                    expectedInsertions + i,
                    fpp,
                    creation,
                    expiration,
                    validPeriodAfterAccess);
            final FilterRecord<GuavaBloomFilter> record = new FilterRecord<>(testingFilterName + "_" + i, filter);
            records.add(record);
        }

        return records;
    }
}