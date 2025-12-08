package com.authenticationservice.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    /**
     * Sends email synchronously. Use for critical emails where delivery confirmation is needed.
     */
    public void sendEmail(String to, String subject, String text) {
        try {
            log.info("Attempting to send email to: {}", to);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Email successfully sent to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Sends email asynchronously. Use for non-critical notifications 
     * where the caller should not wait for email delivery.
     */
    @Async
    public void sendEmailAsync(String to, String subject, String text) {
        try {
            log.info("Attempting to send async email to: {}", to);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Async email successfully sent to: {}", to);
        } catch (MailException e) {
            // Only log error for async emails, don't throw exception
            log.error("Failed to send async email to {}: {}", to, e.getMessage(), e);
        }
    }
} 