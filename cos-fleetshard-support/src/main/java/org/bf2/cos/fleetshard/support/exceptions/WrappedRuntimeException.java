package org.bf2.cos.fleetshard.support.exceptions;

public class WrappedRuntimeException extends RuntimeException {
    public WrappedRuntimeException() {
    }

    public WrappedRuntimeException(String message) {
        super(message);
    }

    public WrappedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrappedRuntimeException(Throwable cause) {
        super(cause);
    }
}
