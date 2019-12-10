package cn.leancloud.filter.service;

import cn.leancloud.filter.service.Configuration.TriggerPersistenceCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

public class PersistentFiltersJob<F extends BloomFilter> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PersistentFiltersJob.class);

    private final BloomFilterManager<F, ?> bloomFilterManager;
    private final PersistentManager<F> persistentManager;
    private final LongAdder filterUpdateTimesCounter;
    private final TriggerPersistenceCriteria criteria;

    PersistentFiltersJob(BloomFilterManager<F, ?> bloomFilterManager,
                         PersistentManager<F> persistentManager,
                         LongAdder filterUpdateTimesCounter,
                         TriggerPersistenceCriteria criteria) {
        this.bloomFilterManager = bloomFilterManager;
        this.persistentManager = persistentManager;
        this.filterUpdateTimesCounter = filterUpdateTimesCounter;
        this.criteria = criteria;
    }

    @Override
    public void run() {
        // synchronized on an instance of a class only to prevent several PersistentFiltersJob to
        // access on the same filterUpdateTimesCounter. It's not mean to and can't prevent thread
        // not in PersistentFiltersJob to access this counter
        synchronized (filterUpdateTimesCounter) {
            final long sum = filterUpdateTimesCounter.sum();
            if (sum > criteria.updatesThreshold()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Updated {} times in last {} seconds meets threshold {} to persistence filters",
                            sum, criteria.checkingPeriod().getSeconds(), criteria.updatesThreshold());
                }

                filterUpdateTimesCounter.reset();
                doPersistence();
            }
        }
    }

    private void doPersistence() {
        try {
            persistentManager.freezeAllFilters(bloomFilterManager);
        } catch (IOException ex) {
            logger.error("Persistent bloom filters failed.", ex);
        } catch (Throwable t) {
            // sorry for the duplication, but currently I don't figure out another way
            // to catch the direct buffer OOM when freeze filters to file
            logger.error("Persistent bloom filters failed.", t);
            throw t;
        }
    }


}
