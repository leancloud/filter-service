package cn.leancloud.filter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

interface BackgroundJob {
    Logger logger = LoggerFactory.getLogger(BackgroundJob.class);

    void start(ScheduledExecutorService scheduledExecutorService);

    void stop();
}
