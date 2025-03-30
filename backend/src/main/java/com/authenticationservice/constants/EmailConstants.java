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
} 