package com.authenticationservice.constants;

import java.util.Arrays;
import java.util.List;

public final class CorsConstants {
    private CorsConstants() {
        throw new IllegalStateException("Constants class");
    }

    public static final List<String> ALLOWED_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
    public static final List<String> ALLOWED_HEADERS = Arrays.asList(
        SecurityConstants.AUTHORIZATION_HEADER,
        SecurityConstants.CONTENT_TYPE_HEADER,
        "Accept-Language",
        "Accept",
        "Origin",
        "X-Requested-With"
    );
    public static final String ALL_PATHS = "/**";
} 