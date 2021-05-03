package net.openhft.chronicle.releasenotes.connector;

public class MigrateException extends RuntimeException {

    public MigrateException() {
        super();
    }

    public MigrateException(String message) {
        super(message);
    }

    public MigrateException(String message, Throwable cause) {
        super(message, cause);
    }

    public MigrateException(Throwable cause) {
        super(cause);
    }

    public MigrateException(String message, Throwable cause, boolean enableSuppression, boolean writeableStacktrace) {
        super(message, cause, enableSuppression, writeableStacktrace);
    }
}
