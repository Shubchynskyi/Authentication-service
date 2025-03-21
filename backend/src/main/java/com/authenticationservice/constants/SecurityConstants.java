package com.authenticationservice.constants;

public final class SecurityConstants {
    private SecurityConstants() {}
    
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    
    public static final String API_AUTH_PREFIX = "/api/auth/**";
    public static final String API_ADMIN_PREFIX = "/api/admin/**";
    public static final String API_PROTECTED_PREFIX = "/api/protected/**";
    
    public static final long ONE_HOUR_IN_MS = 3600000;
    
    public static final String EMAIL_VERIFIED_ERROR = "Email не подтвержден";
    public static final String USER_NOT_FOUND_ERROR = "Пользователь не найден";
    public static final String INVALID_PASSWORD_ERROR = "Неверный пароль";

    public static final String ROOT_PATH = "/";
    public static final String LOGIN_PATH = "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String VERIFY_PATH = "/verify";
    public static final String FORGOT_PASSWORD_PATH = "/forgot-password";
    public static final String RESET_PASSWORD_PATH = "/reset-password";
    
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String BEARER_PREFIX = "Bearer ";
    
    public static final String ALL_PATHS = "/**";
} 