package cn.leancloud.filter.service;

import cn.leancloud.filter.service.utils.ChecksumedBufferedOutputStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(value = 1)
public class PersistentFilterByOutputStreamBenchmark {
    GuavaBloomFilter filter;
    FileChannel fileChannel;

    @Setup
    public void setup() throws Exception {
        final Duration validPeriodAfterAccess = Duration.ofSeconds(3);
        final int expectedInsertions = 10000000;
        final double fpp = 0.001;
        final ZonedDateTime creation = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime expiration = creation.plus(Duration.ofSeconds(10));
        filter = new GuavaBloomFilter(
                expectedInsertions,
                fpp,
                creation,
                expiration,
                validPeriodAfterAccess);
        fileChannel = FileChannel.open(Paths.get("/dev/null"), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.READ);
    }

    @TearDown
    public void teardown() throws Exception {
        fileChannel.close();
    }

    @Benchmark
    public int testByteArrayOutputStream() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        filter.writeTo(out);
        ByteBuffer buffer = ByteBuffer.wrap(out.toByteArray());
        return writeFullyTo(fileChannel, buffer);
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

    @Benchmark
    public int testBufferedOutputStream() throws Exception {
        final long pos = fileChannel.position();
        final BufferedOutputStream bout = new BufferedOutputStream(Channels.newOutputStream(fileChannel), 102400);
        filter.writeTo(bout);
        bout.flush();
        return (int) (fileChannel.position() - pos);
    }

    @Benchmark
    public int testChecksumedBufferedOutputStream() throws Exception {
        final long pos = fileChannel.position();
        final ChecksumedBufferedOutputStream bout = new ChecksumedBufferedOutputStream(Channels.newOutputStream(fileChannel), 102400);
        filter.writeTo(bout);
        bout.flush();
        return (int) (fileChannel.position() - pos);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(PersistentFilterByOutputStreamBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(10))
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
