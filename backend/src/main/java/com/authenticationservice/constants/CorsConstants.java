package com.authenticationservice.constants;

import java.util.List;

public final class CorsConstants {
    private CorsConstants() {}
    
    public static final String FRONTEND_URL = "http://localhost:5173";
    
    public static final List<String> ALLOWED_METHODS = List.of(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    );
    
    public static final List<String> ALLOWED_HEADERS = List.of(
        SecurityConstants.AUTHORIZATION_HEADER,
        SecurityConstants.CONTENT_TYPE_HEADER
    );
} 