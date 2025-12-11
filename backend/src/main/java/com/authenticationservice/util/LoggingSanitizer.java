package com.authenticationservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggingSanitizer {

    private static final String MASK = "***";

    /**
     * Masks the local part of an email, keeping the domain visible.
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return MASK + email.substring(atIndex);
        }
        return email.charAt(0) + MASK + email.substring(atIndex);
    }

    /**
     * Masks sensitive token-like values fully.
     */
    public String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        return MASK;
    }
}

