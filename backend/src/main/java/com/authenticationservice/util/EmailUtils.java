package com.authenticationservice.util;

import java.util.Locale;
import java.util.Optional;

/**
 * Email helper utilities.
 */
public final class EmailUtils {

    private EmailUtils() {
    }

    /**
     * Normalizes email by trimming and converting to lower case.
     *
     * @param email source email value
     * @return normalized email or null when input is null
     */
    public static String normalize(String email) {
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse(null);
    }
}

