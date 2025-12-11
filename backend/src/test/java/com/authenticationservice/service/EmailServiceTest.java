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
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
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
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
    }

    @Nested
    @DisplayName("Successful Email Sending Tests")
    class SuccessfulEmailSendingTests {
        @Test
        @DisplayName("Should send email successfully")
        void sendEmail_shouldSucceed_whenMailSenderWorks() throws Exception {
            // Arrange
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(
                TestConstants.UserData.TEST_EMAIL,
                TestConstants.Email.TEST_SUBJECT,
                TestConstants.Email.TEST_MESSAGE
            );
            
            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            
            assertNotNull(sentMessage, "Captured message should not be null");
            
            assertNotNull(sentMessage.getAllRecipients(), "Recipients array should not be null");
            assertTrue(sentMessage.getAllRecipients().length > 0, "Recipients array should not be empty");
            assertEquals(TestConstants.UserData.TEST_EMAIL, sentMessage.getAllRecipients()[0].toString(),
                        "Email should be sent to the correct recipient");
            assertEquals(TestConstants.Email.TEST_SUBJECT, sentMessage.getSubject(),
                        "Email subject should match");
            assertEquals(TestConstants.Email.TEST_MESSAGE, sentMessage.getContent(),
                        "Email content should match");
        }

        @Test
        @DisplayName("Should send email with different email addresses")
        void sendEmail_shouldSendToDifferentAddresses() throws Exception {
            // Arrange
            String differentEmail = "different@example.com";
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(differentEmail, "Subject", "Message");

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            assertEquals(differentEmail, sentMessage.getAllRecipients()[0].toString());
        }

        @Test
        @DisplayName("Should send email with long subject and message")
        void sendEmail_shouldSendWithLongContent() throws Exception {
            // Arrange
            String longSubject = "This is a very long subject that might contain special characters: !@#$%^&*()";
            String longMessage = "This is a very long message that contains multiple lines\n" +
                    "Line 2\n" +
                    "Line 3 with special characters: !@#$%^&*()";
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, longSubject, longMessage);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            assertEquals(longSubject, sentMessage.getSubject());
            assertEquals(longMessage, sentMessage.getContent());
        }

        @Test
        @DisplayName("Should send multipart email when HTML is provided")
        void sendEmail_shouldSendHtmlAlternative() throws Exception {
            // Arrange
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(
                TestConstants.UserData.TEST_EMAIL,
                TestConstants.Email.TEST_SUBJECT,
                "Plain text body",
                "<p>HTML body</p>"
            );

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            assertNotNull(sentMessage.getContent(), "Email content should exist");
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
                .when(mailSender).send(any(MimeMessage.class));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendEmail(
                    TestConstants.UserData.TEST_EMAIL,
                    TestConstants.Email.TEST_SUBJECT,
                    TestConstants.Email.TEST_MESSAGE
                )
            );
            assertTrue(ex.getMessage().contains("Failed to send email"));
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should throw exception when MailException occurs")
        void sendEmail_shouldThrowException_whenMailExceptionOccurs() {
            // Arrange
            MailException mailException = new MailSendException("SMTP server error");
            doThrow(mailException).when(mailSender).send(any(MimeMessage.class));

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
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should wrap MailException in RuntimeException")
        void sendEmail_shouldWrapMailExceptionInRuntimeException() {
            // Arrange
            MailException mailException = new MailSendException("Connection timeout");
            doThrow(mailException).when(mailSender).send(any(MimeMessage.class));

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
        void sendEmail_shouldHandleEmptySubject() throws Exception {
            // Arrange
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, "", TestConstants.Email.TEST_MESSAGE);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            assertEquals("", sentMessage.getSubject());
        }

        @Test
        @DisplayName("Should handle empty message")
        void sendEmail_shouldHandleEmptyMessage() throws Exception {
            // Arrange
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, TestConstants.Email.TEST_SUBJECT, "");

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            assertEquals("", sentMessage.getContent());
        }

        @Test
        @DisplayName("Should handle null subject")
        void sendEmail_shouldHandleNullSubject() throws Exception {
            // Arrange
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, null, TestConstants.Email.TEST_MESSAGE);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            assertEquals("", sentMessage.getSubject());
        }

        @Test
        @DisplayName("Should handle null message")
        void sendEmail_shouldHandleNullMessage() throws Exception {
            // Arrange
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // Act
            emailService.sendEmail(TestConstants.UserData.TEST_EMAIL, TestConstants.Email.TEST_SUBJECT, null);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            assertEquals("", sentMessage.getContent());
        }
    }
} 