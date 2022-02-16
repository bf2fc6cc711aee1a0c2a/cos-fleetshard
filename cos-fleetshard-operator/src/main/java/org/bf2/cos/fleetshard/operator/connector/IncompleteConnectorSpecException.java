package org.bf2.cos.fleetshard.operator.connector;

public class IncompleteConnectorSpecException extends IllegalArgumentException {

    private static final long serialVersionUID = -2501269288893345775L;

    public IncompleteConnectorSpecException() {
        super();
    }

    public IncompleteConnectorSpecException(String s) {
        super(s);
    }

    public IncompleteConnectorSpecException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompleteConnectorSpecException(Throwable cause) {
        super(cause);
    }
}
