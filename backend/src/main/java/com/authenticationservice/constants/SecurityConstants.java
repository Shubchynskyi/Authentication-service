package com.authenticationservice.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class SecurityConstants {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String API_AUTH_PREFIX = "/api/auth/**";
    public static final String API_ADMIN_PREFIX = "/api/admin/**";
    public static final String API_PROTECTED_PREFIX = "/api/protected/**";
    public static final String API_PUBLIC_PREFIX = "/api/public/**";

    public static final String USER_NOT_FOUND_ERROR = "User not found";
    public static final String INVALID_CREDENTIALS_ERROR = "Invalid email or password";

    public static final String ROOT_PATH = "/";
    public static final String LOGIN_PATH = "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String VERIFY_PATH = "/verify";
    public static final String FORGOT_PASSWORD_PATH = "/forgot-password";
    public static final String RESET_PASSWORD_PATH = "/reset-password";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    // AuthController specific constants
    public static final String REFRESH_TOKEN_KEY = "refreshToken";
    public static final String ADMIN_PANEL_RESOURCE = "admin-panel";
    public static final String USER_MANAGEMENT_RESOURCE = "user-management";
    public static final String OAUTH2_EMAIL_ATTRIBUTE = "email";
    public static final String OAUTH2_NAME_ATTRIBUTE = "name";
    public static final String ACCESS_TOKEN_KEY = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
}
