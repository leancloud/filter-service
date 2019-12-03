package cn.leancloud.filter.service;

public class InvalidFilterException extends PersistentStorageException {
    public InvalidFilterException() {
    }

    public InvalidFilterException(String message) {
        super(message);
    }

    public InvalidFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFilterException(Throwable cause) {
        super(cause);
    }
}
