package com.authenticationservice.service;

import com.authenticationservice.util.LoggingSanitizer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    /**
     * Sends email synchronously. Use for critical emails where delivery confirmation is needed.
     */
    public void sendEmail(String to, String subject, String text) {
        sendEmail(to, subject, text, null);
    }

    /**
     * Sends multipart email with plain text and optional HTML body.
     */
    public void sendEmail(String to, String subject, String text, String html) {
        try {
            log.info("Attempting to send email to: {}", maskEmail(to));

            MimeMessage message = mailSender.createMimeMessage();
            String safeText = text == null ? "" : text;
            boolean hasHtml = html != null && !html.isBlank();
            String safeSubject = subject == null ? "" : subject;
            MimeMessageHelper helper = new MimeMessageHelper(message, hasHtml, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(safeSubject);
            if (hasHtml) {
                helper.setText(safeText, html);
            } else {
                helper.setText(safeText, false);
            }

            mailSender.send(message);
            log.info("Email successfully sent to: {}", maskEmail(to));
        } catch (MessagingException e) {
            log.error("Failed to build email to {}: {}", maskEmail(to), e.getMessage(), e);
            throw new RuntimeException("Failed to build email: " + e.getMessage());
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", maskEmail(to), e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Sends email asynchronously. Use for non-critical notifications 
     * where the caller should not wait for email delivery.
     */
    @Async
    public void sendEmailAsync(String to, String subject, String text) {
        sendEmailAsync(to, subject, text, null);
    }

    @Async
    public void sendEmailAsync(String to, String subject, String text, String html) {
        try {
            log.info("Attempting to send async email to: {}", maskEmail(to));
            
            MimeMessage message = mailSender.createMimeMessage();
            String safeText = text == null ? "" : text;
            boolean hasHtml = html != null && !html.isBlank();
            String safeSubject = subject == null ? "" : subject;
            MimeMessageHelper helper = new MimeMessageHelper(message, hasHtml, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(safeSubject);
            if (hasHtml) {
                helper.setText(safeText, html);
            } else {
                helper.setText(safeText, false);
            }

            mailSender.send(message);
            log.info("Async email successfully sent to: {}", maskEmail(to));
        } catch (MessagingException e) {
            log.error("Failed to build async email to {}: {}", maskEmail(to), e.getMessage(), e);
        } catch (MailException e) {
            // Only log error for async emails, don't throw exception
            log.error("Failed to send async email to {}: {}", maskEmail(to), e.getMessage(), e);
        }
    }

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }
} 