package org.bf2.cos.fleetshard.operator.client;

public class FleetManagerClientException extends RuntimeException {
    private int statusCode;

    public FleetManagerClientException() {
    }

    public FleetManagerClientException(String message) {
        super(message);
    }

    public FleetManagerClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public FleetManagerClientException(Throwable cause) {
        super(cause);
    }

    public FleetManagerClientException(Throwable cause, int statusCode) {
        super(cause);

        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
