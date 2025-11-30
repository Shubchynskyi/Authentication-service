package com.authenticationservice.exception;

public class AccountLockedException extends RuntimeException {
    private final long lockSeconds;

    public AccountLockedException(long lockSeconds) {
        super("Account is temporarily locked");
        this.lockSeconds = lockSeconds;
    }

    public long getLockSeconds() {
        return lockSeconds;
    }
}
