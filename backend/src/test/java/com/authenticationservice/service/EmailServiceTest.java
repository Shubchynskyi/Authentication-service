package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Should send email successfully")
    void sendEmail_shouldSucceed_whenMailSenderWorks() {
        // Arrange
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        emailService.sendEmail(
            TestConstants.UserData.TEST_EMAIL,
            TestConstants.Email.TEST_SUBJECT,
            TestConstants.Email.TEST_MESSAGE
        );
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should throw exception when mail sender fails")
    void sendEmail_shouldThrowException_whenMailSenderFails() {
        // Arrange
        doThrow(new RuntimeException(TestConstants.ErrorMessages.EMAIL_SEND_ERROR))
            .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            emailService.sendEmail(
                TestConstants.UserData.TEST_EMAIL,
                TestConstants.Email.TEST_SUBJECT,
                TestConstants.Email.TEST_MESSAGE
            )
        );
        assertEquals(TestConstants.ErrorMessages.EMAIL_SEND_ERROR, ex.getMessage());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
} 