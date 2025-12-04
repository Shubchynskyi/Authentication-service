package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
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

    @Nested
    @DisplayName("Successful Email Sending Tests")
    class SuccessfulEmailSendingTests {
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
        @DisplayName("Should send email with different email addresses")
        void sendEmail_shouldSendToDifferentAddresses() {
            // Arrange
            String differentEmail = "different@example.com";
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // Act
            emailService.sendEmail(differentEmail, "Subject", "Message");

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals(differentEmail, sentMessage.getTo()[0]);
        }

        @Test
        @DisplayName("Should send email with long subject and message")
        void sendEmail_shouldSendWithLongContent() {
            // Arrange
            String longSubject = "This is a very long subject that might contain special characters: !@#$%^&*()";
            String longMessage = "This is a very long message that contains multiple lines\n" +
                    "Line 2\n" +
                    "Line 3 with special characters: !@#$%^&*()";
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, longSubject, longMessage);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals(longSubject, sentMessage.getSubject());
            assertEquals(longMessage, sentMessage.getText());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
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
            assertTrue(ex.getMessage().contains("Failed to send email"));
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("Should throw exception when MailException occurs")
        void sendEmail_shouldThrowException_whenMailExceptionOccurs() {
            // Arrange
            MailException mailException = new MailSendException("SMTP server error");
            doThrow(mailException).when(mailSender).send(any(SimpleMailMessage.class));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendEmail(
                    TestConstants.UserData.TEST_EMAIL,
                    TestConstants.Email.TEST_SUBJECT,
                    TestConstants.Email.TEST_MESSAGE
                )
            );
            assertTrue(ex.getMessage().contains("Failed to send email"));
            assertTrue(ex.getMessage().contains("SMTP server error"));
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("Should wrap MailException in RuntimeException")
        void sendEmail_shouldWrapMailExceptionInRuntimeException() {
            // Arrange
            MailException mailException = new MailSendException("Connection timeout");
            doThrow(mailException).when(mailSender).send(any(SimpleMailMessage.class));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendEmail(
                    TestConstants.UserData.TEST_EMAIL,
                    TestConstants.Email.TEST_SUBJECT,
                    TestConstants.Email.TEST_MESSAGE
                )
            );
            assertEquals(RuntimeException.class, ex.getClass());
            assertTrue(ex.getMessage().contains("Failed to send email"));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        @Test
        @DisplayName("Should handle empty subject")
        void sendEmail_shouldHandleEmptySubject() {
            // Arrange
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, "", TestConstants.Email.TEST_MESSAGE);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals("", sentMessage.getSubject());
        }

        @Test
        @DisplayName("Should handle empty message")
        void sendEmail_shouldHandleEmptyMessage() {
            // Arrange
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, TestConstants.Email.TEST_SUBJECT, "");

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals("", sentMessage.getText());
        }

        @Test
        @DisplayName("Should handle null subject")
        void sendEmail_shouldHandleNullSubject() {
            // Arrange
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, null, TestConstants.Email.TEST_MESSAGE);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertNull(sentMessage.getSubject());
        }

        @Test
        @DisplayName("Should handle null message")
        void sendEmail_shouldHandleNullMessage() {
            // Arrange
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, TestConstants.Email.TEST_SUBJECT, null);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertNull(sentMessage.getText());
        }
    }
} 