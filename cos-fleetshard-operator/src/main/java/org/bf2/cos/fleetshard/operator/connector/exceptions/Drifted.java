package org.bf2.cos.fleetshard.operator.connector.exceptions;

public class Drifted extends ConnectorControllerException {
    public Drifted() {
    }

    public Drifted(String message) {
        super(message);
    }

    public Drifted(String message, Throwable cause) {
        super(message, cause);
    }

    public Drifted(Throwable cause) {
        super(cause);
    }

    public static Drifted of(String format, Object... args) {
        return new Drifted(String.format(format, args));
    }
}
