package com.authenticationservice.util;

import com.authenticationservice.constants.EmailConstants;

/**
 * Builds plain text and HTML email payloads using shared templates.
 */
public final class EmailTemplateFactory {

    private EmailTemplateFactory() {
    }

    public static String buildVerificationText(String code, String link) {
        return String.format(EmailConstants.VERIFICATION_EMAIL_TEXT_TEMPLATE, code, link);
    }

    public static String buildVerificationHtml(String code, String link) {
        return String.format(EmailConstants.VERIFICATION_EMAIL_HTML_TEMPLATE, code, link);
    }

    public static String buildResetPasswordText(String link) {
        return String.format(EmailConstants.RESET_PASSWORD_TEXT_TEMPLATE, link);
    }

    public static String buildResetPasswordHtml(String link) {
        return String.format(EmailConstants.RESET_PASSWORD_HTML_TEMPLATE, link, link, link);
    }

    public static String buildAdminInviteText(String tempPassword, String verificationLink) {
        return String.format(EmailConstants.ADMIN_INVITE_TEXT_TEMPLATE, tempPassword, verificationLink);
    }

    public static String buildAdminInviteHtml(String tempPassword, String verificationLink) {
        return String.format(EmailConstants.ADMIN_INVITE_HTML_TEMPLATE, tempPassword, verificationLink,
                verificationLink, verificationLink);
    }

    public static String buildGoogleResetText() {
        return EmailConstants.GOOGLE_PASSWORD_RESET_TEXT_TEMPLATE;
    }

    public static String buildAccountLockedText(int lockTimeMinutes, String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_TEMPORARILY_LOCKED_TEMPLATE, lockTimeMinutes, frontendUrl,
                lockTimeMinutes);
    }

    public static String buildAccountLockedHtml(int lockTimeMinutes, String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_TEMPORARILY_LOCKED_HTML_TEMPLATE, lockTimeMinutes, frontendUrl,
                lockTimeMinutes);
    }

    public static String buildAccountBlockedText(String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_BLOCKED_TEMPLATE, frontendUrl);
    }

    public static String buildAccountBlockedHtml(String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_BLOCKED_HTML_TEMPLATE, frontendUrl);
    }
}
