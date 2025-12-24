package com.authenticationservice.exception;

/**
 * Thrown when registration is forbidden (user already exists, not in whitelist, in blacklist, etc.).
 * Details are logged but not exposed to client for security reasons.
 */
public class RegistrationForbiddenException extends RuntimeException {

    private static final String MESSAGE_KEY = "registration.forbidden";

    public RegistrationForbiddenException() {
        super(MESSAGE_KEY);
    }

    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}


