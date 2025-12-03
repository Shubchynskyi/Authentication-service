package com.authenticationservice.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TestConstants {
    
    public static Object REFRESH_TOKEN;
    
    // ************** User Data Constants **************
    public static final class UserData {
        private UserData() {}
        
        // Email addresses
        public static final String ADMIN_EMAIL = "admin@example.com";
        public static final String TEST_EMAIL = "test@example.com";
        public static final String INVALID_EMAIL = "invalid@example.com";
        public static final String CREATE_EMAIL = "create@example.com";
        
        // Usernames
        public static final String ADMIN_USERNAME = "admin";
        public static final String TEST_USERNAME = "testuser";
        public static final String NEW_USERNAME = "newusername";
        
        // Passwords - must match password validation requirements (min 8 chars, digit, uppercase, lowercase, special char)
        public static final String TEST_PASSWORD = "Password123@";
        public static final String ENCODED_PASSWORD = "encodedPassword";
        public static final String NEW_PASSWORD = "NewPassword123@";
        public static final String CURRENT_PASSWORD = "CurrentPass123@";
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
        public static final String INCORRECT_PASSWORD = "Incorrect current password";
        public static final String EMAIL_SEND_ERROR = "Failed to send email";
        public static final String ADMIN_ROLE_NOT_FOUND = "Admin role not found";
        public static final String EMAIL_ALREADY_VERIFIED = "Email is already verified.";
        public static final String EMAIL_ALREADY_IN_WHITELIST = "Email already exists in whitelist";
        public static final String EMAIL_NOT_IN_WHITELIST = "Email not found in whitelist";
        public static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions";
        public static final String USER_ALREADY_EXISTS = "User with this email already exists";
        public static final String INVALID_VERIFICATION_CODE = "Invalid verification code";
        public static final String ACCOUNT_BLOCKED = "Account is blocked.";
    }
    
    // ************** Email Constants **************
    public static final class Email {
        private Email() {}
        
        public static final String TEST_SUBJECT = "Test Subject";
        public static final String TEST_MESSAGE = "Test Message";
    }
    
    // ************** Test Database Constants **************
    public static final class TestDatabase {
        private TestDatabase() {}
        
        public static final String POSTGRES_IMAGE = "postgres:17.5";
        public static final String DATABASE_NAME = "testdb";
        public static final String USERNAME = "test";
        public static final String PASSWORD = "test";
    }
    
    // ************** Test Properties Constants **************
    public static final class TestProperties {
        private TestProperties() {}
        
        // JWT Properties
        public static final String JWT_ACCESS_SECRET = "k3Cuu/bg8vP/q7W05bVeV+FcYdt6q+lqRj2acEwud3u+98Pp3rtMn1XF2/KjY4/d";
        public static final String JWT_REFRESH_SECRET = "u+1t/7mY9rG/u4x12zN7z+B2d6Fp7p+T9v9w8x/y/z+1t/7mY9rG/u4x12zN7z+B";
        public static final String JWT_ACCESS_EXPIRATION = "3600000"; // 1 hour
        public static final String JWT_REFRESH_EXPIRATION = "86400000"; // 24 hours
        
        // Mail Properties
        public static final String MAIL_HOST = "localhost";
        public static final String MAIL_PORT = "587";
        public static final String MAIL_USERNAME = "test@test.com";
        public static final String MAIL_PASSWORD = "test";
        
        // Frontend Properties
        public static final String FRONTEND_URL = "http://localhost:3000";
        
        // Admin Properties
        public static final String ADMIN_ENABLED = "false";
        
        // OAuth Properties
        public static final String OAUTH_GOOGLE_CLIENT_ID = "test-client-id";
        public static final String OAUTH_GOOGLE_CLIENT_SECRET = "test-client-secret";
    }
    
    // ************** Test Data Constants **************
    public static final class TestData {
        private TestData() {}
        
        // Additional test emails
        public static final String SECOND_USER_EMAIL = "seconduser@example.com";
        public static final String NEW_USER_EMAIL = "newuser@example.com";
        public static final String WHITELIST_EMAIL = "test@example.com";
        public static final String NEW_WHITELIST_EMAIL = "new@example.com";
        
        // Additional test usernames
        public static final String ADMIN_NAME = "Admin";
        public static final String SECOND_USER_NAME = "Second User";
        public static final String NEW_USER_NAME = "New User";
        public static final String UPDATED_NAME = "Updated Name";
        
        // Additional test passwords
        public static final String ADMIN_PASSWORD = "Admin123@";
        public static final String SECOND_USER_PASSWORD = "Password123@";
        public static final String NEW_USER_PASSWORD = "Password123@";
        public static final String NEW_PASSWORD_VALUE = "NewPassword123@";
    }
} 