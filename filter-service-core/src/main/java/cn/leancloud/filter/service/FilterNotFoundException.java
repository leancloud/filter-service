package cn.leancloud.filter.service;

/**
 * A {@link Exception} that is raised when a requested filter did not found.
 */
public final class FilterNotFoundException extends Exception {
    private static final long serialVersionUID = -1L;

    // We don't need stack trace for this exception
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
