package com.authenticationservice.exception;

public class AccountBlockedException extends RuntimeException {
    private final String reason;

    public AccountBlockedException(String reason) {
        super(reason != null && !reason.isEmpty() 
            ? "Account is blocked. " + reason 
            : "Account is blocked");
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
