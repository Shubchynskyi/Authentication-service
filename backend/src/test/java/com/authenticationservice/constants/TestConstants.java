package com.authenticationservice.constants;

/**
 * Constants used in test classes.
 */
public final class TestConstants {
    
    public static Object REFRESH_TOKEN;

    // Prevent instantiation
    private TestConstants() {}
    
    // ************** User Data Constants **************
    public static final class UserData {
        private UserData() {}
        
        // Email addresses
        public static final String ADMIN_EMAIL = "admin@example.com";
        public static final String TEST_EMAIL = "test@example.com";
        public static final String INVALID_EMAIL = "invalid@example.com";
        
        // Usernames
        public static final String ADMIN_USERNAME = "admin";
        public static final String TEST_USERNAME = "testuser";
        public static final String NEW_USERNAME = "newusername";
        
        // Passwords
        public static final String TEST_PASSWORD = "password";
        public static final String ENCODED_PASSWORD = "encodedPassword";
        public static final String NEW_PASSWORD = "newPassword";
        public static final String CURRENT_PASSWORD = "currentPassword";
        public static final String NEW_ENCODED_PASSWORD = "newEncodedPassword";
        public static final String EXPECTED_NEW_ENCODED_PASSWORD = "encoded-newPassword";
    }
    
    // ************** Role Constants **************
    public static final class Roles {
        private Roles() {}
        
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
        public static final String ROLE_USER = "ROLE_USER";
    }
    
    // ************** URL Constants **************
    public static final class Urls {
        private Urls() {}
        
        public static final String FRONTEND_URL = "http://localhost:8080";
    }
    
    // ************** Token Constants **************
    public static final class Tokens {
        private Tokens() {}
        
        public static final String RESET_TOKEN = "resetToken";
        public static final String ACCESS_TOKEN = "accessToken";
        public static final String REFRESH_TOKEN = "refreshToken";
        public static final String VERIFICATION_TOKEN = "verificationToken";
    }
    
    // ************** Error Message Constants **************
    public static final class ErrorMessages {
        private ErrorMessages() {}
        
        public static final String USER_NOT_FOUND = "User not found";
        public static final String USER_NOT_FOUND_RUSSIAN = "Пользователь не найден";
        public static final String INCORRECT_PASSWORD = "Incorrect current password";
        public static final String EMAIL_SEND_ERROR = "Failed to send email";
        public static final String ADMIN_ROLE_NOT_FOUND = "Admin role not found";
        public static final String EMAIL_ALREADY_VERIFIED = "Email is already verified.";
        public static final String EMAIL_ALREADY_IN_WHITELIST = "Email already exists in whitelist";
        public static final String EMAIL_NOT_IN_WHITELIST = "Email not found in whitelist";
        public static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions";
        public static final String USER_ALREADY_EXISTS = "User with this email already exists";
        public static final String INVALID_VERIFICATION_CODE = "Invalid verification code";
    }
    
    // ************** Email Constants **************
    public static final class Email {
        private Email() {}
        
        public static final String TEST_SUBJECT = "Test Subject";
        public static final String TEST_MESSAGE = "Test Message";
    }
} 