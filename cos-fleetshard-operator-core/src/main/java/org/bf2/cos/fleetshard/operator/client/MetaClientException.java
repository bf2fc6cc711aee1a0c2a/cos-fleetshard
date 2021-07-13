package org.bf2.cos.fleetshard.operator.client;

public class MetaClientException extends RuntimeException {
    private int statusCode;

    public MetaClientException() {
    }

    public MetaClientException(String message) {
        super(message);
    }

    public MetaClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetaClientException(Throwable cause) {
        super(cause);
    }

    public MetaClientException(Throwable cause, int statusCode) {
        super(cause);

        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
