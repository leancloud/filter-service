package cn.leancloud.filter.service.utils;

import java.time.ZonedDateTime;

public interface Timer {
    Timer DEFAULT_TIMER = new DefaultTimer();

    ZonedDateTime utcNow();
}
