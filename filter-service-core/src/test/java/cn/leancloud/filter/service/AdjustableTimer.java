package cn.leancloud.filter.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AdjustableTimer implements Timer{
    private ZonedDateTime now;

    public AdjustableTimer() {
        this.now = ZonedDateTime.now(ZoneOffset.UTC);
    }

    public void setNow(ZonedDateTime now) {
        this.now = now;
    }

    @Override
    public ZonedDateTime utcNow() {
        return now;
    }
}
