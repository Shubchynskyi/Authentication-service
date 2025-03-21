package com.authenticationservice.constants;

public final class EmailConstants {
    private EmailConstants() {}
    
    public static final String VERIFICATION_SUBJECT = "Подтверждение Email для BNB Project";
    public static final String RESET_PASSWORD_SUBJECT = "Password Reset Request";
    
    public static final String VERIFICATION_EMAIL_TEMPLATE = 
            "Здравствуйте!\n\n" +
            "Для завершения регистрации на BNB Project, пожалуйста, используйте следующий код подтверждения:\n\n" +
            "Код подтверждения: %s\n\n" +
            "Этот код действителен в течение 15 минут.\n\n" +
            "Пожалуйста, введите этот код на странице подтверждения регистрации.\n\n" +
            "С уважением,\nКоманда BNB Project";
            
    public static final String RESET_PASSWORD_EMAIL_TEMPLATE = 
            "You have requested a password reset. Please click the link below to reset your password:\n\n%s\n\n" +
            "If you did not request a password reset, please ignore this email.";
} 