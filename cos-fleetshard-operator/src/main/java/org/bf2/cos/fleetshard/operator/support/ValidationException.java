package org.bf2.cos.fleetshard.operator.support;

public class ValidationException extends Exception {
    private final String type;
    private final String status;
    private final String reason;
    private final String message;

    public ValidationException(String type, String reason, String message) {
        this(type, "False", reason, message);
    }

    public ValidationException(String type, String status, String reason, String message) {
        this.type = type;
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
