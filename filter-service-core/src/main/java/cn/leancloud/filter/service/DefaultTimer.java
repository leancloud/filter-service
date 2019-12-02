package cn.leancloud.filter.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class DefaultTimer implements Timer{
    @Override
    public ZonedDateTime utcNow() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }
}
