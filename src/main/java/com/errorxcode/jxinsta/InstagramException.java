package com.errorxcode.jxinsta;

public class InstagramException extends Exception {
    private final Reason reason;

    public InstagramException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public InstagramException(String message) {
        super(message);
        this.reason = Reason.UNKNOWN;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        TOO_MUCH_PICTURES,
        UNKNOWN,
        UNKNOWN_LOGIN_ERROR,
        INVALID_CREDENTIAL,
        INVALID_LOGIN_TYPE,
        INCORRECT_PASSWORD,
        INCORRECT_USERNAME,
        LOGIN_EXPIRED,
        CHECKPOINT_REQUIRED,
        RATE_LIMITED,
        TWO_FACTOR_REQUIRED,
        CSRF_AUTHENTICATION_FAILED,
    }
}
