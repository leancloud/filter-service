package cn.leancloud.filter.service;

public final class FilterNotFoundException extends Exception {
    // We don't need stack trace for this exception
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
