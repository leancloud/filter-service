package cn.leancloud.filter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class CountUpdateBloomFilterWrapperTest {
    private LongAdder filterUpdateTimesCounter;
    private BloomFilter innerFilter;
    private CountUpdateBloomFilterWrapper wrapper;

    @Before
    public void setUp() {
        filterUpdateTimesCounter = new LongAdder();
        innerFilter = mock(BloomFilter.class);
        wrapper = new CountUpdateBloomFilterWrapper(innerFilter, filterUpdateTimesCounter);
    }

    @Test
    public void testDelegate() throws IOException {
        final int expectedInsertions = 1000;
        final double fpp = 0.01;
        final String testingValue = "testingValue";
        final OutputStream out = new ByteArrayOutputStream();

        when(innerFilter.expectedInsertions()).thenReturn(expectedInsertions);
        when(innerFilter.fpp()).thenReturn(fpp);
        when(innerFilter.mightContain(testingValue)).thenReturn(true);
        when(innerFilter.set(testingValue)).thenReturn(false);
        when(innerFilter.valid()).thenReturn(false);
        doNothing().when(innerFilter).writeTo(out);

        assertThat(wrapper.expectedInsertions()).isEqualTo(expectedInsertions);
        assertThat(wrapper.fpp()).isEqualTo(fpp);
        assertThat(wrapper.mightContain(testingValue)).isTrue();
        assertThat(wrapper.set(testingValue)).isFalse();
        assertThat(wrapper.valid()).isFalse();
        wrapper.writeTo(out);
        assertThat(filterUpdateTimesCounter.sum()).isEqualTo(1);

        verify(innerFilter, times(1)).expectedInsertions();
        verify(innerFilter, times(1)).fpp();
        verify(innerFilter, times(1)).mightContain(testingValue);
        verify(innerFilter, times(1)).set(testingValue);
        verify(innerFilter, times(1)).valid();
        verify(innerFilter, times(1)).writeTo(out);
    }

    @Test
    public void testUpdateCounter() {
        final String testingValue = "testingValue";
        when(innerFilter.set(testingValue)).thenReturn(false);
        assertThat(filterUpdateTimesCounter.sum()).isZero();
        assertThat(wrapper.set(testingValue)).isFalse();
        assertThat(filterUpdateTimesCounter.sum()).isEqualTo(1);
    }

    @Test
    public void testWriteToThrowException() throws IOException {
        final OutputStream out = new ByteArrayOutputStream();
        final IOException expectedException = new IOException("expected IO exception");
        doThrow(expectedException).when(innerFilter).writeTo(out);

        assertThatThrownBy(() -> wrapper.writeTo(out)).isSameAs(expectedException);
        verify(innerFilter, times(1)).writeTo(out);
    }

    @Test
    public void testToJson() {
        final Duration validPeriodAfterAccess = Duration.ofSeconds(10);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        config.setValidPeriodAfterAccess(validPeriodAfterAccess);
        final GuavaBloomFilter innerFilter = new GuavaBloomFilterFactory().createFilter(config);
        final ObjectMapper mapper = new ObjectMapper();
        final String expectedJson = mapper.valueToTree(innerFilter).toString();

        final CountUpdateBloomFilterWrapper filter = new CountUpdateBloomFilterWrapper(innerFilter, filterUpdateTimesCounter);
        assertThat(mapper.valueToTree(filter).toString()).isEqualTo(expectedJson);
    }
}