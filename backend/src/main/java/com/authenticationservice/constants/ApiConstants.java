package com.authenticationservice.constants;

public final class ApiConstants {
    private ApiConstants() {}
    
    public static final String AUTH_BASE_URL = "/api/auth";
    public static final String ADMIN_BASE_URL = "/api/admin";
    public static final String PROTECTED_BASE_URL = "/api/protected";
    
    // URLs for authentication endpoints
    public static final String REGISTER_URL = "/register";
    public static final String LOGIN_URL = "/login";
    public static final String VERIFY_URL = "/verify";
    public static final String REFRESH_URL = "/refresh";
    public static final String PROFILE_URL = "/profile";
    public static final String RESEND_VERIFICATION_URL = "/resend-verification";
    public static final String FORGOT_PASSWORD_URL = "/forgot-password";
    public static final String RESET_PASSWORD_URL = "/reset-password";
    public static final String CHECK_URL = "/check";
    public static final String CHECK_ACCESS_URL = "/check-access/{resource}";
    public static final String OAUTH2_SUCCESS_URL = "/oauth2/success";

    // URLs for admin endpoints
    public static final String WHITELIST_ADD_URL = "/whitelist/add";
    public static final String WHITELIST_REMOVE_URL = "/whitelist/remove";
    public static final String WHITELIST_URL = "/whitelist";
    public static final String USERS_URL = "/users";
    public static final String USER_ID_URL = "/users/{id}";
    public static final String USERS_ID_ROLES_URL = "/users/{id}/roles";
    public static final String ROLES_URL = "/roles";
    public static final String VERIFY_ADMIN_URL = "/verify-admin";
    public static final String MASKED_LOGIN_SETTINGS_URL = "/masked-login/settings";
} 