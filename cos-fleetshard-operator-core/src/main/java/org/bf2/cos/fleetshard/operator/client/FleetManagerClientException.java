package org.bf2.cos.fleetshard.operator.client;

public class FleetManagerClientException extends RuntimeException {

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
}
