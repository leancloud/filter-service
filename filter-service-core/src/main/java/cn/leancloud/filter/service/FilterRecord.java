package cn.leancloud.filter.service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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

    public int writeFullyTo(GatheringByteChannel channel) throws IOException {
        final byte[] body = serializeBody();
        final ByteBuffer buffer = ByteBuffer.allocate(HEADER_OVERHEAD + body.length);
        // write body first
        buffer.mark();
        buffer.position(HEADER_OVERHEAD);
        buffer.put(body);
        buffer.reset();

        // write header
        buffer.putInt(BODY_LENGTH_OFFSET, body.length);
        buffer.put(MAGIC_OFFSET, DEFAULT_MAGIC);
        final long crc = Crc32C.compute(buffer, HEADER_OVERHEAD, body.length);
        buffer.putInt(CRC_OFFSET, (int) crc);
        return writeFullyTo(channel, buffer);
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
        return Objects.hash(name, filter);
    }

    @Override
    public String toString() {
        return "FilterRecord{" +
                "name='" + name + '\'' +
                ", filter=" + filter +
                '}';
    }

    private byte[] serializeBody() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DataOutputStream dout = new DataOutputStream(out);
        final byte[] nameInBytes = name.getBytes(StandardCharsets.UTF_8);
        dout.writeInt(nameInBytes.length);
        dout.write(nameInBytes);
        filter.writeTo(out);
        return out.toByteArray();
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
}

