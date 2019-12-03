package cn.leancloud.filter.service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * The schema is:
 * Length => Int32
 * MAGIC => Byte
 * CRC => Uint32
 * NameLength => Int32
 * Name => Bytes
 * Filter => Bytes
 */
public final class FilterRecord<F extends BloomFilter> {
    static final int LENGTH_OFFSET = 0;
    static final int LENGTH_LENGTH = 4;
    static final int MAGIC_OFFSET = LENGTH_OFFSET + LENGTH_LENGTH;
    static final int MAGIC_LENGTH = 1;
    static final int CRC_OFFSET = MAGIC_OFFSET + MAGIC_LENGTH;
    static final int CRC_LENGTH = 4;
    static final int HEADER_OVERHEAD = CRC_OFFSET + CRC_LENGTH;

    private String name;
    private F filter;

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
        buffer.putInt(HEADER_OVERHEAD + body.length);
        buffer.put((byte) 0);
        final long crc = Crc32C.compute(buffer, HEADER_OVERHEAD, body.length);
        buffer.putInt((int)crc);
        return writeFullyTo(channel, buffer);
    }

    private byte[] serializeBody() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DataOutputStream dout = new DataOutputStream(out);
        dout.writeInt(name.length());
        dout.writeChars(name);
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

