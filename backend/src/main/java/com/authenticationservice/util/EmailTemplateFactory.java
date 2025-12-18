package com.authenticationservice.util;

import com.authenticationservice.constants.EmailConstants;
import java.util.Map;

/**
 * Builds plain text and HTML email payloads using shared templates.
 */
public final class EmailTemplateFactory {

    private EmailTemplateFactory() {
    }

    public static String buildVerificationText(String link) {
        return String.format(EmailConstants.VERIFICATION_EMAIL_TEXT_TEMPLATE, link);
    }

    public static String buildVerificationHtml(String link) {
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_VERIFICATION_HTML,
                Map.of("verificationLink", link));
    }

    public static String buildResetPasswordText(String link) {
        return String.format(EmailConstants.RESET_PASSWORD_TEXT_TEMPLATE, link);
    }

    public static String buildResetPasswordHtml(String link) {
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_RESET_PASSWORD_HTML,
                Map.of("resetLink", link));
    }

    public static String buildAdminInviteText(String tempPassword, String verificationLink) {
        return String.format(EmailConstants.ADMIN_INVITE_TEXT_TEMPLATE, tempPassword, verificationLink);
    }

    public static String buildAdminInviteHtml(String tempPassword, String verificationLink) {
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_ACCOUNT_CREATED_HTML,
                Map.of(
                        "tempPassword", tempPassword,
                        "verificationLink", verificationLink));
    }

    public static String buildGoogleResetText() {
        return EmailConstants.GOOGLE_PASSWORD_RESET_TEXT_TEMPLATE;
    }

    public static String buildGoogleResetHtml() {
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_GOOGLE_PASSWORD_RESET_HTML,
                Map.of());
    }

    public static String buildOtpAccessModeText(String otp) {
        return String.format(EmailConstants.OTP_ACCESS_MODE_TEXT_TEMPLATE, otp);
    }

    public static String buildOtpAccessModeHtml(String otp) {
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_OTP_ACCESS_MODE_HTML,
                Map.of("otp", otp));
    }

    public static String buildAccountLockedText(int lockTimeMinutes, String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_TEMPORARILY_LOCKED_TEMPLATE, lockTimeMinutes, frontendUrl,
                lockTimeMinutes);
    }

    public static String buildAccountLockedHtml(int lockTimeMinutes, String frontendUrl) {
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_ACCOUNT_LOCKED_HTML,
                Map.of(
                        "lockTimeMinutes", String.valueOf(lockTimeMinutes),
                        "frontendUrl", frontendUrl));
    }

    public static String buildAccountBlockedText(String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_BLOCKED_TEMPLATE, frontendUrl);
    }

    public static String buildAccountBlockedHtml(String frontendUrl) {
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_ACCOUNT_BLOCKED_HTML,
                Map.of("frontendUrl", frontendUrl));
    }
}
