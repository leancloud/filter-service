package cn.leancloud.filter.service;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterRecordTest {
    private final Duration validPeriodAfterAccess = Duration.ofSeconds(3);
    private final int expectedInsertions = 1000000;
    private final double fpp = 0.0001;
    private final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
    private final ZonedDateTime expiration = creation.plus(Duration.ofSeconds(10));
    private final String testingFilterName = "testing_filter";

    private File tempFile;

    @Before
    public void setUp() throws Exception {
        final String tempDir = System.getProperty("java.io.tmpdir", "/tmp") +
                File.separator + "filter_service_" + System.nanoTime();
        FileUtils.forceMkdir(new File(tempDir));
        tempFile = new File(tempDir + File.separator + "filter_record_test");
    }

    @Test
    public void testReadWriteFilterRecord() throws Exception {
        final GuavaBloomFilter filter = new GuavaBloomFilter(
                expectedInsertions,
                fpp,
                creation,
                expiration,
                validPeriodAfterAccess);
        final FilterRecord<GuavaBloomFilter> record = new FilterRecord<>(testingFilterName, filter);
        final FileChannel channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);

        record.writeFullyTo(channel);

        final FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(tempFile.toPath(), new GuavaBloomFilterFactory());
        assertThat(stream.nextFilterRecord()).isEqualTo(new FilterRecord<>(testingFilterName, filter));
    }
}