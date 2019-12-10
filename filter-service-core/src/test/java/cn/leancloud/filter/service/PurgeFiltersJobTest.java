package cn.leancloud.filter.service;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class PurgeFiltersJobTest {
    private Purgatory purgatory;
    private PurgeFiltersJob job;

    @Before
    public void setUp() {
        purgatory = mock(Purgatory.class);
        job = new PurgeFiltersJob(purgatory);
    }

    @Test
    public void testPurge() {
        doNothing().when(purgatory).purge();
        job.run();
        verify(purgatory, times(1)).purge();
    }

    @Test
    public void testPurgeThrowsException() {
        final RuntimeException ex = new RuntimeException("expected exception");
        doThrow(ex).when(purgatory).purge();
        job.run();
        verify(purgatory, times(1)).purge();
    }
}