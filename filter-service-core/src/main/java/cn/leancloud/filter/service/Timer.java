package cn.leancloud.filter.service;

import java.time.ZonedDateTime;

public interface Timer {
    Timer DEFAULT_TIMER = new DefaultTimer();

    ZonedDateTime utcNow();
}
