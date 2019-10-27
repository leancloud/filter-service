package cn.leancloud.filter.service;

final class BadParameterException extends IllegalArgumentException {
    static BadParameterException invalidParameter(String paramName) {
        return new BadParameterException(String.format("invalid parameter: \"%s\"", paramName));
    }

    static BadParameterException invalidParameter(String paramName, String errorMsg) {
        return new BadParameterException(String.format("invalid parameter: \"%s\". %s", paramName, errorMsg));
    }

    static BadParameterException requiredParameter(String paramName) {
        return new BadParameterException(String.format("required parameter: \"%s\"", paramName));
    }

    private BadParameterException(String errorMsg) {
        super(errorMsg);
    }

    // We don't need stack trace for this exception
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
