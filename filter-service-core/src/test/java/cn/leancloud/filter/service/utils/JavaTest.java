package cn.leancloud.filter.service.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaTest {
    @Test
    public void testJavaVersion() {
        Java.Version v = Java.parseVersion("9");
        assertThat(v.majorVersion).isEqualTo(9);
        assertThat(v.minorVersion).isEqualTo(0);
        assertThat(v.isJava9Compatible()).isTrue();

        v = Java.parseVersion("9.0.1");
        assertThat(v.majorVersion).isEqualTo(9);
        assertThat(v.minorVersion).isEqualTo(0);
        assertThat(v.isJava9Compatible()).isTrue();

        v = Java.parseVersion("9.0.0.15"); // Azul Zulu
        assertThat(v.majorVersion).isEqualTo(9);
        assertThat(v.minorVersion).isEqualTo(0);
        assertThat(v.isJava9Compatible()).isTrue();

        v = Java.parseVersion("9.1");
        assertThat(v.majorVersion).isEqualTo(9);
        assertThat(v.minorVersion).isEqualTo(1);
        assertThat(v.isJava9Compatible()).isTrue();

        v = Java.parseVersion("1.8.0_152");
        assertThat(v.majorVersion).isEqualTo(1);
        assertThat(v.minorVersion).isEqualTo(8);
        assertThat(v.isJava9Compatible()).isFalse();


        v = Java.parseVersion("1.7.0_80");
        assertThat(v.majorVersion).isEqualTo(1);
        assertThat(v.minorVersion).isEqualTo(7);
        assertThat(v.isJava9Compatible()).isFalse();
    }
}