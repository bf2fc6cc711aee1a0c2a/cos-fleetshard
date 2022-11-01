package org.bf2.cos.fleet.manager.client;

public class RestClientException extends RuntimeException {
    private int statusCode;

    public RestClientException() {
    }

    public RestClientException(String message) {
        super(message);
    }

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestClientException(Throwable cause) {
        super(cause);
    }

    public RestClientException(Throwable cause, int statusCode) {
        super(cause);

        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
