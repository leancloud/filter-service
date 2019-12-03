package cn.leancloud.filter.service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class PersistentManager<F extends BloomFilter> {
    private BloomFilterManager<F, ?> manager;
    private BloomFilterFactory<F, ?> factory;
    private Path basePath;

    public PersistentManager(BloomFilterManager<F, ?> manager, BloomFilterFactory<F, ?> factory) throws IOException {
        final String tempDir = System.getProperty("java.io.tmpdir", "/tmp") +
                File.separator + "filter_service_" + System.nanoTime();
        FileUtils.forceMkdir(new File(tempDir));
        this.basePath = Paths.get(tempDir);


        this.manager = manager;
        this.factory = factory;
    }


    public void freezeAllFilters() throws Exception {
        final Path tempPath = basePath.resolve("serialization_manager.tmp");
        final FileChannel channel = FileChannel.open(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
        for (FilterRecord<F> record : manager) {
            record.writeFullyTo(channel);
        }
    }

    public void recoverFiltersFromFile() {
        Iterable<FilterRecord<? extends F>> filters = readFiltersFromFile();
        manager.addFilters(filters);
    }

    public Iterable<FilterRecord<? extends F>> readFiltersFromFile() {
        return () -> new AbstractIterator<FilterRecord<? extends F>>() {
            @Nullable
            @Override
            protected FilterRecord<? extends F> makeNext() {
                try {

                    F filter = factory.readFrom(null);
                    return new FilterRecord<>("name", filter);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        };
    }



}
