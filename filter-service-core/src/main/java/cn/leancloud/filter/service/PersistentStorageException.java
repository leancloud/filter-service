package cn.leancloud.filter.service;

public class PersistentStorageException extends RuntimeException  {
    private static final long serialVersionUID = -1L;

    public PersistentStorageException() {
    }

    public PersistentStorageException(String message) {
        super(message);
    }

    public PersistentStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistentStorageException(Throwable cause) {
        super(cause);
    }
}
