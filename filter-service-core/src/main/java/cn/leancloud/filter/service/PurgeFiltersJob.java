package cn.leancloud.filter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurgeFiltersJob implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PurgeFiltersJob.class);
    private final Purgatory purgatory;

    PurgeFiltersJob(Purgatory purgatory) {
        this.purgatory = purgatory;
    }

    @Override
    public void run() {
        try {
            purgatory.purge();
        } catch (Exception ex) {
            logger.error("Purge bloom filter service failed.", ex);
        }
    }
}
