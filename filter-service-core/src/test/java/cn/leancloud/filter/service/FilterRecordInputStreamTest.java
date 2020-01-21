package cn.leancloud.filter.service;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static cn.leancloud.filter.service.FilterRecord.*;
import static cn.leancloud.filter.service.TestingUtils.generateFilterRecords;
import static cn.leancloud.filter.service.TestingUtils.generateSingleFilterRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FilterRecordInputStreamTest {
    private final GuavaBloomFilterFactory factory = new GuavaBloomFilterFactory();

    private FileChannel writeRecordChannel;
    private File tempFile;
    private String tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = System.getProperty("java.io.tmpdir", "/tmp") +
                File.separator + "filter_service_" + System.nanoTime();
        FileUtils.forceMkdir(new File(tempDir));
        tempFile = new File(tempDir + File.separator + "filter_record_test");
        writeRecordChannel = FileChannel.open(tempFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
    }

    @After
    public void tearDown() throws Exception {
        writeRecordChannel.close();
        FileUtils.forceDelete(new File(tempDir));
    }

    @Test
    public void testReadWriteMultiFilterRecord() throws Exception {
        final List<FilterRecord<BloomFilter>> records = generateFilterRecords(100);
        for (FilterRecord<BloomFilter> record : records) {
            record.writeFullyTo(writeRecordChannel);
        }

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(tempFile.toPath(), factory)) {
            for (FilterRecord<BloomFilter> expectRecord : records) {
                assertThat(stream.nextFilterRecord()).isEqualTo(expectRecord);
            }

            assertThat(stream.nextFilterRecord()).isNull();
        }
    }

    @Test
    public void testShortReadFilterHeader() throws Exception {
        final FilterRecord<BloomFilter> record = generateSingleFilterRecord();
        record.writeFullyTo(writeRecordChannel);
        writeRecordChannel.truncate(HEADER_OVERHEAD - 1);

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(tempFile.toPath(), factory)) {
            assertThatThrownBy(stream::nextFilterRecord).hasMessage(UnfinishedFilterException.shortReadFilterHeader(tempFile.toString(), 1).getMessage());
        }
    }

    @Test
    public void testShortReadFilterBody() throws Exception {
        final FilterRecord<BloomFilter> record = generateSingleFilterRecord();
        record.writeFullyTo(writeRecordChannel);
        writeRecordChannel.truncate(writeRecordChannel.size() - 1);

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(tempFile.toPath(), factory)) {
            assertThatThrownBy(stream::nextFilterRecord).hasMessage(UnfinishedFilterException.shortReadFilterBody(tempFile.toString(), 1).getMessage());
        }
    }

    @Test
    public void testBadMagic() throws Exception {
        final FilterRecord<BloomFilter> record = generateSingleFilterRecord();
        record.writeFullyTo(writeRecordChannel);

        overwriteFirstFilterRecordMagic((byte) 101);

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(tempFile.toPath(), factory)) {
            assertThatThrownBy(stream::nextFilterRecord)
                    .isInstanceOf(InvalidFilterException.class)
                    .hasMessageContaining("read unknown Magic: 101 from position");
        }
    }

    @Test
    public void testBadCrc() throws Exception {
        final FilterRecord<BloomFilter> record = generateSingleFilterRecord();
        record.writeFullyTo(writeRecordChannel);

        overwriteFirstFilterRecordCrc(101);

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(tempFile.toPath(), factory)) {
            assertThatThrownBy(stream::nextFilterRecord)
                    .isInstanceOf(InvalidFilterException.class)
                    .hasMessageContaining("got unmatched crc when read filter from position");
        }
    }

    @Test
    public void testEOF() throws Exception {
        final FilterRecord<BloomFilter> record = generateSingleFilterRecord();
        record.writeFullyTo(writeRecordChannel);

        try (FilterRecordInputStream<GuavaBloomFilter> stream = new FilterRecordInputStream<>(tempFile.toPath(), factory)) {
            writeRecordChannel.truncate(writeRecordChannel.size() - 1);
            assertThatThrownBy(stream::nextFilterRecord)
                    .isInstanceOf(EOFException.class)
                    .hasMessageContaining("Failed to read `FilterRecord` from file channel");
        }
    }

    private void overwriteFirstFilterRecordMagic(byte magic) throws IOException {
        ByteBuffer buffer = readFirstHeader();
        buffer.flip();
        buffer.put(MAGIC_OFFSET, magic);
        writeRecordChannel.position(0);
        writeFullyTo(writeRecordChannel, buffer);
    }

    private void overwriteFirstFilterRecordCrc(int crc) throws IOException {
        ByteBuffer buffer = readFirstHeader();
        buffer.flip();
        buffer.putInt(CRC_OFFSET, crc);
        writeRecordChannel.position(0);
        writeFullyTo(writeRecordChannel, buffer);
    }

    private int writeFullyTo(GatheringByteChannel channel, ByteBuffer buffer) throws IOException {
        buffer.mark();
        int written = 0;
        while (written < buffer.limit()) {
            written = channel.write(buffer);
        }
        buffer.reset();
        return written;
    }

    private ByteBuffer readFirstHeader() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_OVERHEAD);
        FilterRecordInputStream.readFullyOrFail(writeRecordChannel, buffer, 0);
        return buffer;
    }
}