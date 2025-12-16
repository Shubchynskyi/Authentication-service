package com.authenticationservice.constants;

public final class EmailConstants {
        private EmailConstants() {
        }

        public static final String VERIFICATION_SUBJECT = "Email Verification - Authentication Service";
        public static final String RESET_PASSWORD_SUBJECT = "Password Reset Request";
        public static final String ADMIN_INVITE_SUBJECT = "Welcome to Authentication Service";
        public static final String GOOGLE_PASSWORD_RESET_SUBJECT = "Password reset unavailable for Google sign-in";

        public static final String VERIFICATION_EMAIL_TEXT_TEMPLATE = "Hello!\n\n" +
                        "Thank you for registering with our Authentication Service. To complete your registration, please use the following verification code:\n\n"
                        +
                        "Verification Code: %s\n\n" +
                        "You can also verify using this link: %s\n\n" +
                        "This code will expire in 15 minutes.\n\n" +
                        "If you did not request this, you can safely ignore this email.\n\n" +
                        "Best regards,\nAuthentication Service Team";

        public static final String VERIFICATION_EMAIL_HTML_TEMPLATE = "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\" />" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                        "<title>Email Verification</title>" +
                        "</head>" +
                        "<body style=\"margin:0;padding:0;background-color:#f4f4f7;font-family:'Helvetica Neue',Arial,sans-serif;\">"
                        +
                        "<table role=\"presentation\" width=\"100%%\" style=\"background-color:#f4f4f7;padding:24px 0;\">"
                        +
                        "<tr><td align=\"center\">" +
                        "<table role=\"presentation\" width=\"600\" style=\"background-color:#ffffff;border:1px solid #e4e7ec;border-radius:12px;overflow:hidden;\">"
                        +
                        "<tr><td style=\"padding:24px 24px 8px;font-size:20px;font-weight:600;color:#111827;\">Confirm your email</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">Thanks for registering with Authentication Service. Use the code below to verify your email.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;\"><div style=\"background:#f3f4f6;border:1px dashed #d1d5db;border-radius:8px;padding:16px;font-size:18px;font-weight:600;color:#111827;text-align:center;letter-spacing:0.08em;\">%s</div></td></tr>"
                        +
                        "<tr><td align=\"center\" style=\"padding:0 24px 24px;\"><a href=\"%s\" style=\"display:inline-block;padding:14px 48px;background-color:#2563eb;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;font-size:16px;\">Verify email</a></td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 24px;font-size:14px;color:#4b5563;\">Best regards,<br>Authentication Service Team</td></tr>"
                        +
                        "</table>" +
                        "</td></tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        public static final String RESET_PASSWORD_TEXT_TEMPLATE = "You have requested a password reset. Please click the link below to reset your password:\n\n%s\n\n"
                        +
                        "If you did not request a password reset, please ignore this email.";

        public static final String RESET_PASSWORD_HTML_TEMPLATE = "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\" />" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                        "<title>Password Reset</title>" +
                        "</head>" +
                        "<body style=\"margin:0;padding:0;background-color:#f4f4f7;font-family:'Helvetica Neue',Arial,sans-serif;\">"
                        +
                        "<table role=\"presentation\" width=\"100%%\" style=\"background-color:#f4f4f7;padding:24px 0;\">"
                        +
                        "<tr><td align=\"center\">" +
                        "<table role=\"presentation\" width=\"600\" style=\"background-color:#ffffff;border:1px solid #e4e7ec;border-radius:12px;overflow:hidden;\">"
                        +
                        "<tr><td style=\"padding:24px 24px 8px;font-size:20px;font-weight:600;color:#111827;\">Reset your password</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">We received a request to reset your password. Click the button below to continue.</td></tr>"
                        +
                        "<tr><td align=\"center\" style=\"padding:0 24px 24px;\"><a href=\"%s\" style=\"display:inline-block;padding:14px 32px;background-color:#16a34a;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;font-size:16px;\">Reset password</a></td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 24px;font-size:14px;color:#4b5563;\">Best regards,<br>Authentication Service Team</td></tr>"
                        +
                        "</table>" +
                        "</td></tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        public static final String ADMIN_INVITE_TEXT_TEMPLATE = "Your account has been created by administrator.\n\n" +
                        "Your temporary password: %s\n" +
                        "Please verify your email by clicking this link: %s\n\n" +
                        "After verification, you can log in and change your password.\n" +
                        "For security reasons, please change your password after first login.";

        public static final String ADMIN_INVITE_HTML_TEMPLATE = "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\" />" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                        "<title>Welcome</title>" +
                        "</head>" +
                        "<body style=\"margin:0;padding:0;background-color:#f4f4f7;font-family:'Helvetica Neue',Arial,sans-serif;\">"
                        +
                        "<table role=\"presentation\" width=\"100%%\" style=\"background-color:#f4f4f7;padding:24px 0;\">"
                        +
                        "<tr><td align=\"center\">" +
                        "<table role=\"presentation\" width=\"600\" style=\"background-color:#ffffff;border:1px solid #e4e7ec;border-radius:12px;overflow:hidden;\">"
                        +
                        "<tr><td style=\"padding:24px 24px 8px;font-size:20px;font-weight:600;color:#111827;\">Welcome to Authentication Service</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">Your account has been created by an administrator. Use the temporary password below and verify your email to get started.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;\">" +
                        "<div style=\"background:#f3f4f6;border:1px dashed #d1d5db;border-radius:8px;padding:16px;font-size:18px;font-weight:600;color:#111827;text-align:center;word-break:break-all;position:relative;\">"
                        +
                        "<span style=\"font-size:24px;vertical-align:middle;margin-right:8px;\"></span>%s" +
                        "</div>" +
                        "</td></tr>" +
                        "<tr><td align=\"center\" style=\"padding:0 24px 24px;\"><a href=\"%s\" style=\"display:inline-block;padding:14px 48px;background-color:#2563eb;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;font-size:16px;\">Verify email</a></td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 24px;font-size:14px;color:#4b5563;\">Best regards,<br>Authentication Service Team</td></tr>"
                        +
                        "</table>" +
                        "</td></tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        public static final String GOOGLE_PASSWORD_RESET_TEXT_TEMPLATE = "We noticed you requested a password reset, but your account is linked with Google.\n"
                        +
                        "Please sign in using the 'Continue with Google' option.\n" +
                        "If you forgot your Google password, use Google's account recovery.";

        public static final String OTP_ACCESS_MODE_SUBJECT = "Access Mode Change - OTP Code";
        public static final String OTP_ACCESS_MODE_TEXT_TEMPLATE = "You requested to change access mode.\n\n" +
                        "Your OTP code: %s\n" +
                        "This code will expire in 10 minutes.\n\n" +
                        "If you did not request this change, please ignore this email.";

        public static final String OTP_ACCESS_MODE_HTML_TEMPLATE = "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\" />" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                        "<title>OTP Code</title>" +
                        "</head>" +
                        "<body style=\"margin:0;padding:0;background-color:#f4f4f7;font-family:'Helvetica Neue',Arial,sans-serif;\">"
                        +
                        "<table role=\"presentation\" width=\"100%%\" style=\"background-color:#f4f4f7;padding:24px 0;\">"
                        +
                        "<tr><td align=\"center\">" +
                        "<table role=\"presentation\" width=\"600\" style=\"background-color:#ffffff;border:1px solid #e4e7ec;border-radius:12px;overflow:hidden;\">"
                        +
                        "<tr><td style=\"padding:24px 24px 8px;font-size:20px;font-weight:600;color:#111827;\">Access Mode Change - OTP Code</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">You requested to change access mode. Use the code below to confirm the change.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;\"><div style=\"background:#f3f4f6;border:1px dashed #d1d5db;border-radius:8px;padding:16px;font-size:24px;font-weight:600;color:#111827;text-align:center;letter-spacing:0.15em;\">%s</div></td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:12px;color:#6b7280;line-height:1.6;text-align:center;\">This code will expire in 10 minutes.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;text-align:center;\">If you did not request this change, please ignore this email.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 24px;font-size:14px;color:#4b5563;\">Best regards,<br>Authentication Service Team</td></tr>"
                        +
                        "</table>" +
                        "</td></tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        public static final String ACCOUNT_TEMPORARILY_LOCKED_SUBJECT = "Account Temporarily Locked";
        public static final String ACCOUNT_TEMPORARILY_LOCKED_TEMPLATE = "Your account has been temporarily locked for %d minutes due to multiple failed login attempts.\n\n"
                        +
                        "If this was not you, please secure your account immediately by resetting your password: %s/reset-password\n\n"
                        +
                        "The account will be automatically unlocked after %d minutes.\n\n" +
                        "Best regards,\nAuthentication Service Team";

        public static final String ACCOUNT_TEMPORARILY_LOCKED_HTML_TEMPLATE = "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\" />" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                        "<title>Account Temporarily Locked</title>" +
                        "</head>" +
                        "<body style=\"margin:0;padding:0;background-color:#f4f4f7;font-family:'Helvetica Neue',Arial,sans-serif;\">"
                        +
                        "<table role=\"presentation\" width=\"100%%\" style=\"background-color:#f4f4f7;padding:24px 0;\">"
                        +
                        "<tr><td align=\"center\">" +
                        "<table role=\"presentation\" width=\"600\" style=\"background-color:#ffffff;border:1px solid #e4e7ec;border-radius:12px;overflow:hidden;\">"
                        +
                        "<tr><td style=\"padding:24px 24px 8px;font-size:20px;font-weight:600;color:#111827;\">Account Locked</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">Your account has been temporarily locked for <strong>%d minutes</strong> due to multiple failed login attempts.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">If this was not you, please secure your account immediately by resetting your password.</td></tr>"
                        +
                        "<tr><td align=\"center\" style=\"padding:0 24px 24px;\"><a href=\"%s/reset-password\" style=\"display:inline-block;padding:14px 32px;background-color:#dc2626;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;font-size:16px;\">Reset Password</a></td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 24px;font-size:12px;color:#6b7280;line-height:1.6;\">The account will be automatically unlocked after %d minutes.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 24px;font-size:14px;color:#4b5563;\">Best regards,<br>Authentication Service Team</td></tr>"
                        +
                        "</table>" +
                        "</td></tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        public static final String ACCOUNT_BLOCKED_SUBJECT = "Account Blocked";
        public static final String ACCOUNT_BLOCKED_TEMPLATE = "Your account has been blocked due to exceeding the maximum number of login attempts.\n\n"
                        +
                        "If this was not you, please secure your account immediately.\n\n" +
                        "To unlock your account, you need to reset your password.\n" +
                        "Follow the link to reset your password: %s/reset-password\n\n" +
                        "Best regards,\nAuthentication Service Team";

        public static final String ACCOUNT_BLOCKED_HTML_TEMPLATE = "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\" />" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                        "<title>Account Blocked</title>" +
                        "</head>" +
                        "<body style=\"margin:0;padding:0;background-color:#f4f4f7;font-family:'Helvetica Neue',Arial,sans-serif;\">"
                        +
                        "<table role=\"presentation\" width=\"100%%\" style=\"background-color:#f4f4f7;padding:24px 0;\">"
                        +
                        "<tr><td align=\"center\">" +
                        "<table role=\"presentation\" width=\"600\" style=\"background-color:#ffffff;border:1px solid #e4e7ec;border-radius:12px;overflow:hidden;\">"
                        +
                        "<tr><td style=\"padding:24px 24px 8px;font-size:20px;font-weight:600;color:#111827;\">Account Blocked</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">Your account has been blocked due to exceeding the maximum number of login attempts.</td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">To unlock your account and secure it, please reset your password immediately.</td></tr>"
                        +
                        "<tr><td align=\"center\" style=\"padding:0 24px 24px;\"><a href=\"%s/reset-password\" style=\"display:inline-block;padding:14px 32px;background-color:#dc2626;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;font-size:16px;\">Reset Password</a></td></tr>"
                        +
                        "<tr><td style=\"padding:0 24px 24px;font-size:14px;color:#4b5563;\">Best regards,<br>Authentication Service Team</td></tr>"
                        +
                        "</table>" +
                        "</td></tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";
}