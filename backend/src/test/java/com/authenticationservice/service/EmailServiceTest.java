package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final String ERROR_MESSAGE = "Failed to send email";

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendEmail_shouldSucceed_whenMailSenderWorks() {
        // Arrange
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        assertDoesNotThrow(() -> emailService.sendEmail(
            TestConstants.TEST_EMAIL, 
            TestConstants.TEST_SUBJECT, 
            TestConstants.TEST_MESSAGE
        ));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldThrowException_whenMailSenderFails() {
        // Arrange
        doThrow(new MailException(ERROR_MESSAGE) {}).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendEmail(
                TestConstants.TEST_EMAIL, 
                TestConstants.TEST_SUBJECT, 
                TestConstants.TEST_MESSAGE
            )
        );
        assertTrue(exception.getMessage().contains(ERROR_MESSAGE));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
} 