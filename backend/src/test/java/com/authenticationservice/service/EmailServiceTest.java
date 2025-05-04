package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;
    
    @BeforeEach
    void setUp() {
        // Common setup for all tests
    }

    @Test
    @DisplayName("Should send email successfully")
    void sendEmail_shouldSucceed_whenMailSenderWorks() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendEmail(
            TestConstants.UserData.TEST_EMAIL,
            TestConstants.Email.TEST_SUBJECT,
            TestConstants.Email.TEST_MESSAGE
        );
        
        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        
        assertNotNull(sentMessage, "Captured message should not be null");
        
        String[] recipients = sentMessage.getTo();
        assertNotNull(recipients, "Recipients array should not be null");
        assertTrue(recipients.length > 0, "Recipients array should not be empty");
        assertEquals(TestConstants.UserData.TEST_EMAIL, recipients[0], 
                    "Email should be sent to the correct recipient");
        assertEquals(TestConstants.Email.TEST_SUBJECT, sentMessage.getSubject(), 
                    "Email subject should match");
        assertEquals(TestConstants.Email.TEST_MESSAGE, sentMessage.getText(), 
                    "Email content should match");
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