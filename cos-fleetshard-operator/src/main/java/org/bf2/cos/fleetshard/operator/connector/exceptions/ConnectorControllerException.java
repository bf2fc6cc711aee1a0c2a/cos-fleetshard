package org.bf2.cos.fleetshard.operator.connector.exceptions;

public class ConnectorControllerException extends Exception {
    public ConnectorControllerException() {
    }

    public ConnectorControllerException(String message) {
        super(message);
    }

    public ConnectorControllerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectorControllerException(Throwable cause) {
        super(cause);
    }
}
