package com.authenticationservice.constants;

/**
 * Constants used in test classes.
 */
public final class TestConstants {
    
    // Prevent instantiation
    private TestConstants() {}
    
    // Common email addresses
    public static final String ADMIN_EMAIL = "admin@example.com";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String INVALID_EMAIL = "invalid@example.com";
    
    // User names
    public static final String ADMIN_USERNAME = "admin";
    public static final String TEST_USERNAME = "testuser";
    
    // Roles
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    
    // Passwords
    public static final String TEST_PASSWORD = "password";
    public static final String ENCODED_PASSWORD = "encodedPassword";
    
    // URLs
    public static final String FRONTEND_URL = "http://localhost:8080";
    
    // Tokens
    public static final String RESET_TOKEN = "resetToken";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String VERIFICATION_TOKEN = "verificationToken";
    
    // Error messages
    public static final String USER_NOT_FOUND = "User not found";
    
    // Email constants
    public static final String TEST_SUBJECT = "Test Subject";
    public static final String TEST_MESSAGE = "Test Message";
} 