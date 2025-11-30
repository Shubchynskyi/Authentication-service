package com.authenticationservice.exception;

import com.authenticationservice.constants.SecurityConstants;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super(SecurityConstants.INVALID_CREDENTIALS_ERROR);
    }
}
