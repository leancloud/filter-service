package cn.leancloud.filter.service;

/**
 * A {@link IllegalArgumentException} that is raised when the parameter for an API
 * do not meet the contract.
 */
final class BadParameterException extends IllegalArgumentException {
    private static final long serialVersionUID = -1L;

    /**
     * Create a {@code BadParameterException} with the name of an invalid parameter.
     *
     * @param paramName the name of the invalid parameter.
     * @return the created {@code BadParameterException}
     */
    static BadParameterException invalidParameter(String paramName) {
        return new BadParameterException(String.format("invalid parameter: \"%s\"", paramName));
    }

    /**
     * Create a {@code BadParameterException} with the name of an invalid parameter and an
     * error message about why this parameter is invalid.
     *
     * @param paramName the name of the invalid parameter.
     * @param errorMsg  the error message about why the input parameter is invalid
     * @return the created {@code BadParameterException}
     */
    static BadParameterException invalidParameter(String paramName, String errorMsg) {
        return new BadParameterException(String.format("invalid parameter: \"%s\". %s", paramName, errorMsg));
    }

    /**
     * Create a {@code BadParameterException} with the name of a required parameter. This
     * is used when a API require a parameter but it's not provided.
     *
     * @param paramName the name of the invalid parameter.
     * @return the created {@code BadParameterException}
     */
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
