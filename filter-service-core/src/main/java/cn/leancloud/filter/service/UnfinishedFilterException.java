package cn.leancloud.filter.service;

public class UnfinishedFilterException extends InvalidFilterException {
    public UnfinishedFilterException() {
    }

    public UnfinishedFilterException(String message) {
        super(message);
    }

    public UnfinishedFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnfinishedFilterException(Throwable cause) {
        super(cause);
    }

    public static UnfinishedFilterException shortReadFilterHeader(String location, int shortBytes) {
        return new UnfinishedFilterException("not enough bytes to read filter record header from: " + location + ". " +
                shortBytes + " bytes short.");
    }

    public static UnfinishedFilterException shortReadFilterBody(String location, int shortBytes) {
        return new UnfinishedFilterException("not enough bytes to read filter record body from: " + location + ". " +
                shortBytes + " bytes short.");
    }
}
