package net.openhft.chronicle.releasenotes.connector;

public class ReleaseException extends RuntimeException {

    public ReleaseException() {
        super();
    }

    public ReleaseException(String message) {
        super(message);
    }

    public ReleaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReleaseException(Throwable cause) {
        super(cause);
    }

    public ReleaseException(String message, Throwable cause, boolean enableSuppression, boolean writeableStacktrace) {
        super(message, cause, enableSuppression, writeableStacktrace);
    }
}
