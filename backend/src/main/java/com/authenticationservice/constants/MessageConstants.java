package com.authenticationservice.constants;

public class MessageConstants {
    public static final String USER_CREATED = "User created";
    public static final String USER_UPDATED = "User updated";
    public static final String USER_DELETED = "User deleted";
    public static final String EMAIL_ADDED_TO_WHITELIST = "Email added to whitelist";
    public static final String EMAIL_REMOVED_FROM_WHITELIST = "Email removed from whitelist";
    public static final String PASSWORD_VERIFIED = "Password verified";
    public static final String PASSWORD_IS_REQUIRED = "Password is required";
    public static final String INVALID_PASSWORD = "Invalid password";

    // AuthController specific messages
    public static final String REGISTRATION_SUCCESS = "Check your email (verification code is in server console)!";
    public static final String EMAIL_VERIFIED_SUCCESS = "Email verified. Now you can login.";
    public static final String UNKNOWN_ERROR = "Unknown error: ";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String EMAIL_REQUIRED = "Email is required.";
    public static final String VERIFICATION_RESENT_SUCCESS = "Verification code resent. Check your email (server console output).";
    public static final String PASSWORD_RESET_INITIATED = "If an account with that email exists, you will receive a password reset link within %d minutes.";
    public static final String RESET_PASSWORD_FIELDS_REQUIRED = "Token, new password, and confirm password are required.";
    public static final String PASSWORDS_DO_NOT_MATCH = "Passwords do not match.";
    public static final String PASSWORD_RESET_SUCCESS = "Password has been reset successfully.";
    public static final String VERIFICATION_CODE_INVALID_OR_EXPIRED = "verification.code.invalidOrExpired";
    public static final String RESEND_RATE_LIMIT_EXCEEDED = "Too many resend attempts. Please wait before trying again.";

    // ProfileController specific messages
    public static final String PROFILE_UPDATED_SUCCESS = "Profile updated successfully";

    // Access list reasons
    public static final String WHITELIST_REASON_ADMIN_CREATED = "User was created by administrator";

    // Access mode / admin verification
    public static final String ACCESS_MODE_REQUIRED_FIELDS = "Mode, password, and OTP code are required";
    public static final String ACCESS_MODE_INVALID = "Invalid access mode";
    public static final String ACCESS_MODE_CHANGED = "Access mode changed successfully";
    public static final String EMAIL_REMOVED_FROM_BLACKLIST = "Email removed from blacklist";
    public static final String TOO_MANY_REQUESTS = "Too many requests";

    // Authentication
    public static final String ACCOUNT_DISABLED = "Account is disabled";
    public static final String INVALID_REFRESH_TOKEN = "Invalid/expired refresh token";
    public static final String INVALID_REMEMBER_DAYS = "Invalid remember days value";

}