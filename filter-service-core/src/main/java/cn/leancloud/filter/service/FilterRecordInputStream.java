package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static cn.leancloud.filter.service.FilterRecord.*;

public final class FilterRecordInputStream<F extends BloomFilter> {
    private static void readFully(FileChannel channel, ByteBuffer destinationBuffer, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("position:" + position + " (expected: >=0)");
        }
        long currentPosition = position;
        int bytesRead;
        do {
            bytesRead = channel.read(destinationBuffer, currentPosition);
            currentPosition += bytesRead;
        } while (bytesRead != -1 && destinationBuffer.hasRemaining());
    }

    private static void readFullyOrFail(FileChannel channel, ByteBuffer destinationBuffer, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("position:" + position + " (expected: >=0)");
        }
        final int expectedReadBytes = destinationBuffer.remaining();
        readFully(channel, destinationBuffer, position);
        if (destinationBuffer.hasRemaining()) {
            throw new EOFException(String.format("Failed to read `FilterRecord` from file channel `%s`. Expected to read %d bytes, " +
                            "but reached end of file after reading %d bytes. Started read from position %d.",
                    channel, expectedReadBytes, expectedReadBytes - destinationBuffer.remaining(), position));
        }
    }

    private final FileChannel channel;
    private final long end;
    private final ByteBuffer headerBuffer;
    private final BloomFilterFactory<F, ?> factory;
    private long position;

    public FilterRecordInputStream(Path recordFilePath, BloomFilterFactory<F, ?> factory) throws IOException {
        this.channel = FileChannel.open(recordFilePath, StandardOpenOption.READ);
        this.position = channel.position();
        this.headerBuffer = ByteBuffer.allocate(HEADER_OVERHEAD);
        this.factory = factory;
        this.end = channel.size();
    }

    @Nullable
    public FilterRecord<F> nextFilterRecord() throws IOException {
        if (end - position <= HEADER_OVERHEAD) {
            return null;
        }

        headerBuffer.rewind();
        readFullyOrFail(channel, headerBuffer, position);
        headerBuffer.rewind();

        int bodyLen = headerBuffer.getInt(BODY_LENGTH_OFFSET);
        if (end - position < HEADER_OVERHEAD + bodyLen) {
            return null;
        }

        checkMagic(headerBuffer);

        final ByteBuffer bodyBuffer = ByteBuffer.allocate(bodyLen);
        readFullyOrFail(channel, bodyBuffer, position + HEADER_OVERHEAD);
        bodyBuffer.flip();

        checkCrc(headerBuffer, bodyBuffer);

        final ByteBuffer filterNameBuffer = readFilterNameBuffer(bodyBuffer);
        final String name = StandardCharsets.UTF_8.decode(filterNameBuffer).toString();
        final F filter = factory.readFrom(new ByteArrayInputStream(bodyBuffer.array(),
                bodyBuffer.position() + filterNameBuffer.limit(), bodyBuffer.remaining()));

        // every thing is fine, we move position forward
        position += bodyLen + HEADER_OVERHEAD;
        return new FilterRecord<>(name, filter);
    }

    private void checkMagic(ByteBuffer headerBuffer) {
        byte magic = headerBuffer.get(MAGIC_OFFSET);
        if (magic != DEFAULT_MAGIC) {
            throw new InvalidFilterException("read unknown Magic: " + magic + " from position: "
                    + position + " from file channel: " + channel);
        }
    }

    private void checkCrc(ByteBuffer headerBuffer, ByteBuffer bodyBuffer) {
        final long expectCrc = readCrc(headerBuffer);
        long actualCrc = Crc32C.compute(bodyBuffer, 0, bodyBuffer.limit());
        if (actualCrc != expectCrc) {
            throw new InvalidFilterException("got unmatched crc when read filter from position: "
                    + position + " from file channel: " + channel + ". expect: " + expectCrc + ", actual: " + actualCrc);
        }
    }

    private ByteBuffer readFilterNameBuffer(ByteBuffer bodyBuffer) {
        final int nameLength = bodyBuffer.getInt();
        final ByteBuffer filterNameBuffer = bodyBuffer.slice();
        filterNameBuffer.limit(nameLength);
        return filterNameBuffer;
    }

    private long readCrc(ByteBuffer headerBuffer) {
        // read unsigned int
        return headerBuffer.getInt(CRC_OFFSET) & 0xffffffffL;
    }

}