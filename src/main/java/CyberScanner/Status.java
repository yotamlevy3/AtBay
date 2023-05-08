package CyberScanner;

enum Status {
    ACCEPTED(System.currentTimeMillis()),
    RUNNING(System.currentTimeMillis()),
    ERROR(System.currentTimeMillis()),
    COMPLETE(System.currentTimeMillis());

    private final long timestamp;

    Status(long timestamp) {
        this.timestamp = timestamp;
    }
}
