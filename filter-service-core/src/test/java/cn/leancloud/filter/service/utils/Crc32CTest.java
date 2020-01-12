package cn.leancloud.filter.service.utils;

import org.junit.Test;

import java.util.zip.Checksum;

import static org.assertj.core.api.Assertions.assertThat;

public class Crc32CTest {
    @Test
    public void testUpdate() {
        final byte[] bytes = "Hello world".getBytes();
        final int len = bytes.length;

        Checksum crc1 = Crc32C.create();
        Checksum crc2 = Crc32C.create();
        Checksum crc3 = Crc32C.create();

        crc1.update(bytes, 0, len);
        for (int i = 0; i < len; i++)
            crc2.update(bytes[i]);
        crc3.update(bytes, 0, len / 2);
        crc3.update(bytes, len / 2, len - len / 2);

        assertThat(crc1.getValue()).isEqualTo(crc2.getValue());
        assertThat(crc1.getValue()).isEqualTo(crc3.getValue());
    }

    @Test
    public void testValue() {
        final byte[] bytes = "Some String".getBytes();
        assertThat(Crc32C.compute(bytes, 0, bytes.length)).isEqualTo(608512271);
    }

}