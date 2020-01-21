package cn.leancloud.filter.service;


import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FilterServiceFileUtilsTest {

    @Test
    public void atomicMoveWithFallbackFailed() throws Exception {
        final String tempDir = System.getProperty("java.io.tmpdir", "/tmp") +
                File.separator + "filter_service_" + System.nanoTime();
        FileUtils.forceMkdir(new File(tempDir));
        Path a = Paths.get(tempDir).resolve("path_a");
        Path b = Paths.get(tempDir).resolve("path_b");
        assertThatThrownBy(() -> FilterServiceFileUtils.atomicMoveWithFallback(a, b))
                .isInstanceOf(NoSuchFileException.class);
        FileUtils.forceDelete(new File(tempDir));
    }
}