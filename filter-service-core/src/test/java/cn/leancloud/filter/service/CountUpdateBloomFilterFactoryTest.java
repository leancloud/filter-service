package cn.leancloud.filter.service;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class CountUpdateBloomFilterFactoryTest {
    private LongAdder filterUpdateTimesCounter;
    private BloomFilterFactory<BloomFilter, BloomFilterConfig> innerFilterFactory;
    private CountUpdateBloomFilterFactory wrapper;

    @Before
    public void setUp() {
        filterUpdateTimesCounter = new LongAdder();
        innerFilterFactory = mock(BloomFilterFactory.class);
        wrapper = new CountUpdateBloomFilterFactory(innerFilterFactory, filterUpdateTimesCounter);
    }

    @Test
    public void testDelegate() throws Exception {
        final InputStream in = new ByteArrayInputStream(new byte[0]);
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();
        final BloomFilter filter = mock(BloomFilter.class);
        when(innerFilterFactory.createFilter(config)).thenReturn(filter);
        when(innerFilterFactory.readFrom(in)).thenReturn(filter);

        assertThat(wrapper.createFilter(config)).isEqualTo(new CountUpdateBloomFilterWrapper(filter, filterUpdateTimesCounter));
        assertThat(wrapper.readFrom(in)).isEqualTo(new CountUpdateBloomFilterWrapper(filter, filterUpdateTimesCounter));
        assertThat(filterUpdateTimesCounter.sum()).isEqualTo(1);

        verify(innerFilterFactory, times(1)).createFilter(config);
        verify(innerFilterFactory, times(1)).readFrom(in);
    }

    @Test
    public void testReadFromThrowsException() throws Exception {
        final InputStream in = new ByteArrayInputStream(new byte[0]);
        final IOException expectedEx = new IOException("expected exception");

        doThrow(expectedEx).when(innerFilterFactory).readFrom(in);

        assertThatThrownBy(() -> wrapper.readFrom(in)).isEqualTo(expectedEx);
        verify(innerFilterFactory, times(1)).readFrom(in);
    }
}