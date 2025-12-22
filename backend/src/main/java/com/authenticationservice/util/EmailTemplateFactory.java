package com.authenticationservice.util;

import com.authenticationservice.config.EmailConfig;
import com.authenticationservice.constants.EmailConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds plain text and HTML email payloads using shared templates.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateFactory {

    private final EmailConfig emailConfig;

    public String getEmailSignature() {
        return emailConfig.getSignature();
    }

    public String buildVerificationText(String link) {
        return String.format(EmailConstants.VERIFICATION_EMAIL_TEXT_TEMPLATE, link, getEmailSignature());
    }

    public String buildVerificationHtml(String link) {
        Map<String, String> variables = new HashMap<>();
        variables.put("verificationLink", link);
        variables.put("emailSignature", getEmailSignature());
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_VERIFICATION_HTML,
                variables);
    }

    public String buildResetPasswordText(String link) {
        return String.format(EmailConstants.RESET_PASSWORD_TEXT_TEMPLATE, link, getEmailSignature());
    }

    public String buildResetPasswordHtml(String link) {
        Map<String, String> variables = new HashMap<>();
        variables.put("resetLink", link);
        variables.put("emailSignature", getEmailSignature());
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_RESET_PASSWORD_HTML,
                variables);
    }

    public String buildAdminInviteText(String tempPassword, String verificationLink) {
        return String.format(EmailConstants.ADMIN_INVITE_TEXT_TEMPLATE, tempPassword, verificationLink, getEmailSignature());
    }

    public String buildAdminInviteHtml(String tempPassword, String verificationLink) {
        Map<String, String> variables = new HashMap<>();
        variables.put("tempPassword", tempPassword);
        variables.put("verificationLink", verificationLink);
        variables.put("emailSignature", getEmailSignature());
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_ACCOUNT_CREATED_HTML,
                variables);
    }

    public String buildGoogleResetText() {
        return EmailConstants.GOOGLE_PASSWORD_RESET_TEXT_TEMPLATE;
    }

    public String buildGoogleResetHtml() {
        Map<String, String> variables = new HashMap<>();
        variables.put("emailSignature", getEmailSignature());
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_GOOGLE_PASSWORD_RESET_HTML,
                variables);
    }

    public String buildOtpAccessModeText(String otp) {
        return String.format(EmailConstants.OTP_ACCESS_MODE_TEXT_TEMPLATE, otp, getEmailSignature());
    }

    public String buildOtpAccessModeHtml(String otp) {
        Map<String, String> variables = new HashMap<>();
        variables.put("otp", otp);
        variables.put("emailSignature", getEmailSignature());
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_OTP_ACCESS_MODE_HTML,
                variables);
    }

    public String buildAccountLockedText(int lockTimeMinutes, String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_TEMPORARILY_LOCKED_TEMPLATE, lockTimeMinutes, frontendUrl,
                lockTimeMinutes, getEmailSignature());
    }

    public String buildAccountLockedHtml(int lockTimeMinutes, String frontendUrl) {
        Map<String, String> variables = new HashMap<>();
        variables.put("lockTimeMinutes", String.valueOf(lockTimeMinutes));
        variables.put("frontendUrl", frontendUrl);
        variables.put("emailSignature", getEmailSignature());
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_ACCOUNT_LOCKED_HTML,
                variables);
    }

    public String buildAccountBlockedText(String frontendUrl) {
        return String.format(EmailConstants.ACCOUNT_BLOCKED_TEMPLATE, frontendUrl, getEmailSignature());
    }

    public String buildAccountBlockedHtml(String frontendUrl) {
        Map<String, String> variables = new HashMap<>();
        variables.put("frontendUrl", frontendUrl);
        variables.put("emailSignature", getEmailSignature());
        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_ACCOUNT_BLOCKED_HTML,
                variables);
    }

    public String buildAccountBlockedByAdminText(String blockReason, LocalDateTime blockedAt) {
        String dateText = "";
        if (blockedAt != null) {
            String formattedDate = blockedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dateText = "\n\nBlocked on: " + formattedDate;
        }

        String reasonText = "";
        if (blockReason != null && !blockReason.trim().isEmpty()) {
            reasonText = "\nReason: " + blockReason;
        }

        return String.format(EmailConstants.ACCOUNT_BLOCKED_BY_ADMIN_TEXT_TEMPLATE, dateText, reasonText, getEmailSignature());
    }

    public String buildAccountBlockedByAdminHtml(String blockReason, LocalDateTime blockedAt) {
        Map<String, String> variables = new HashMap<>();
        variables.put("emailSignature", getEmailSignature());

        // Build date row HTML if blockedAt is not null
        String dateRow = "";
        if (blockedAt != null) {
            String formattedDate = blockedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dateRow = "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">" +
                    "Blocked on: <strong>" + formattedDate + "</strong></td></tr>";
        }
        variables.put("blockDateRow", dateRow);

        // Build reason row HTML if blockReason is not null or empty
        String reasonRow = "";
        if (blockReason != null && !blockReason.trim().isEmpty()) {
            reasonRow = "<tr><td style=\"padding:0 24px 16px;font-size:14px;color:#4b5563;line-height:1.6;\">" +
                    "Reason: " + blockReason + "</td></tr>";
        }
        variables.put("blockReasonRow", reasonRow);

        return EmailHtmlTemplateRenderer.renderFromClasspath(
                EmailConstants.TEMPLATE_ACCOUNT_BLOCKED_BY_ADMIN_HTML,
                variables);
    }
}
