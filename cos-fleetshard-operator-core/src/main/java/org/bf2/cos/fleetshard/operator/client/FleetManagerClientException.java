package org.bf2.cos.fleetshard.operator.client;

import javax.ws.rs.WebApplicationException;

import org.bf2.cos.fleet.manager.api.model.cp.Error;

public class FleetManagerClientException extends RuntimeException {
    private Error error;

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

    public FleetManagerClientException(Throwable cause, Error error) {
        super(cause);

        this.error = error;
    }

    public FleetManagerClientException(WebApplicationException cause) {
        super(cause);

        this.error = cause.getResponse().readEntity(Error.class);
    }

    public Error getError() {
        return error;
    }
}
