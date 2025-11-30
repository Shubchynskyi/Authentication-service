package com.authenticationservice.constants;

public final class EmailConstants {
    private EmailConstants() {}
    
    public static final String VERIFICATION_SUBJECT = "Email Verification - Authentication Service";
    public static final String RESET_PASSWORD_SUBJECT = "Password Reset Request";
    
    public static final String VERIFICATION_EMAIL_TEMPLATE = 
            "Hello!\n\n" +
            "Thank you for registering with our Authentication Service. To complete your registration, please use the following verification code:\n\n" +
            "Verification Code: %s\n\n" +
            "This code will expire in 15 minutes.\n\n" +
            "Please enter this code on the verification page to activate your account.\n\n" +
            "Best regards,\nAuthentication Service Team";
            
    public static final String RESET_PASSWORD_EMAIL_TEMPLATE = 
            "You have requested a password reset. Please click the link below to reset your password:\n\n%s\n\n" +
            "If you did not request a password reset, please ignore this email.";
    
    public static final String ACCOUNT_TEMPORARILY_LOCKED_SUBJECT = "Account Temporarily Locked";
    public static final String ACCOUNT_TEMPORARILY_LOCKED_TEMPLATE = 
            "Your account has been temporarily locked for %d minutes due to multiple failed login attempts.\n\n" +
            "If this was not you, please secure your account immediately by resetting your password: %s/reset-password\n\n" +
            "The account will be automatically unlocked after %d minutes.\n\n" +
            "Best regards,\nAuthentication Service Team";
    
    public static final String ACCOUNT_BLOCKED_SUBJECT = "Account Blocked";
    public static final String ACCOUNT_BLOCKED_TEMPLATE = 
            "Your account has been blocked due to exceeding the maximum number of login attempts.\n\n" +
            "If this was not you, please secure your account immediately.\n\n" +
            "To unlock your account, you need to reset your password.\n" +
            "Follow the link to reset your password: %s/reset-password\n\n" +
            "Best regards,\nAuthentication Service Team";
} 