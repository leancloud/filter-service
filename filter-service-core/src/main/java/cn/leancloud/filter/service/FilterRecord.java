package cn.leancloud.filter.service;

import cn.leancloud.filter.service.utils.ChecksumedBufferedOutputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * The schema is:
 * BodyLength: Int32
 * MAGIC: Byte
 * CRC: Uint32
 * NameLength: Int32
 * Name: Bytes
 * Filter: Bytes
 */
public final class FilterRecord<F extends BloomFilter> {
    static final int BODY_LENGTH_OFFSET = 0;
    static final int BODY_LENGTH_LENGTH = 4;
    static final int MAGIC_OFFSET = BODY_LENGTH_OFFSET + BODY_LENGTH_LENGTH;
    static final int MAGIC_LENGTH = 1;
    static final int CRC_OFFSET = MAGIC_OFFSET + MAGIC_LENGTH;
    static final int CRC_LENGTH = 4;
    static final int HEADER_OVERHEAD = CRC_OFFSET + CRC_LENGTH;

    static final byte DEFAULT_MAGIC = (byte) 0;

    private final String name;
    private final F filter;

    public FilterRecord(String name, F filter) {
        this.name = name;
        this.filter = filter;
    }

    public String name() {
        return name;
    }

    public F filter() {
        return filter;
    }

    public int writeFullyTo(FileChannel channel) throws IOException {
        final long startPos = channel.position();
        // write body first then we can know how large the body is
        channel.position(startPos + HEADER_OVERHEAD);

        // we don't need to close this stream. it'll be effectively closed when the underlying channel closed
        final ChecksumedBufferedOutputStream stream = new ChecksumedBufferedOutputStream(
                Channels.newOutputStream(channel),
                Configuration.channelBufferSizeForFilterPersistence());
        writeBody(stream);
        stream.flush();

        // write header
        int bodyLen = (int) (channel.position() - startPos - HEADER_OVERHEAD);
        channel.position(startPos);

        final ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_OVERHEAD);
        headerBuffer.putInt(BODY_LENGTH_OFFSET, bodyLen);
        headerBuffer.put(MAGIC_OFFSET, DEFAULT_MAGIC);
        headerBuffer.putInt(CRC_OFFSET, (int) stream.checksum());
        writeBufferTo(channel, headerBuffer);

        // move position forward to the end of this record
        channel.position(startPos + HEADER_OVERHEAD + bodyLen);
        return HEADER_OVERHEAD + bodyLen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FilterRecord<?> that = (FilterRecord<?>) o;
        return name.equals(that.name) &&
                filter.equals(that.filter);
    }

    @Override
    public int hashCode() {
        int ret = name.hashCode();
        ret = 31 * ret + filter.hashCode();
        return ret;
    }

    @Override
    public String toString() {
        return "FilterRecord{" +
                "name='" + name + '\'' +
                ", filter=" + filter +
                '}';
    }

    private void writeBody(OutputStream out) throws IOException {
        final DataOutputStream dout = new DataOutputStream(out);
        final byte[] nameInBytes = name.getBytes(StandardCharsets.UTF_8);
        dout.writeInt(nameInBytes.length);
        dout.write(nameInBytes);
        filter.writeTo(out);
    }

    private int writeBufferTo(GatheringByteChannel channel, ByteBuffer buffer) throws IOException {
        buffer.mark();
        int written = 0;
        while (written < buffer.limit()) {
            written = channel.write(buffer);
        }
        buffer.reset();
        return written;
    }
}

