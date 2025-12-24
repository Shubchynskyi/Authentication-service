package com.authenticationservice.exception;

import com.authenticationservice.constants.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Nested
    @DisplayName("AccountLockedException Tests")
    class AccountLockedExceptionTests {
        @Test
        @DisplayName("Should return 401 with invalid credentials message")
        void handleAccountLocked_shouldReturn401_withInvalidCredentialsMessage() {
            // Arrange
            AccountLockedException ex = new AccountLockedException(300L);

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleAccountLocked(ex);

            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Unauthorized", response.getBody().get("error"));
            assertEquals(SecurityConstants.INVALID_CREDENTIALS_ERROR, response.getBody().get("message"));
        }
    }

    @Nested
    @DisplayName("AccountBlockedException Tests")
    class AccountBlockedExceptionTests {
        @Test
        @DisplayName("Should return 403 with block reason")
        void handleAccountBlocked_shouldReturn403_withBlockReason() {
            // Arrange
            String reason = "Suspicious activity detected";
            AccountBlockedException ex = new AccountBlockedException(reason);

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleAccountBlocked(ex);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Account blocked", response.getBody().get("error"));
            assertTrue(response.getBody().get("message").contains("Account is blocked"));
            assertEquals(reason, response.getBody().get("reason"));
        }

        @Test
        @DisplayName("Should return 403 with Unknown reason when reason is null")
        void handleAccountBlocked_shouldReturn403_withUnknownReason_whenReasonIsNull() {
            // Arrange
            AccountBlockedException ex = new AccountBlockedException(null);

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleAccountBlocked(ex);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Account blocked", response.getBody().get("error"));
            assertEquals("Unknown", response.getBody().get("reason"));
        }
    }

    @Nested
    @DisplayName("RegistrationForbiddenException Tests")
    class RegistrationForbiddenExceptionTests {
        @Test
        @DisplayName("Should return 400 with localized message")
        void handleRegistrationForbidden_shouldReturn400_withLocalizedMessage() {
            // Arrange
            RegistrationForbiddenException ex = new RegistrationForbiddenException();
            String localizedMessage = "Unable to complete registration. Contact administrator.";
            when(messageSource.getMessage(eq("registration.forbidden"), isNull(), any(Locale.class)))
                    .thenReturn(localizedMessage);

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleRegistrationForbidden(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Bad Request", response.getBody().get("error"));
            assertEquals(localizedMessage, response.getBody().get("message"));
            verify(messageSource).getMessage(eq("registration.forbidden"), isNull(), any(Locale.class));
        }
    }

    @Nested
    @DisplayName("InvalidCredentialsException Tests")
    class InvalidCredentialsExceptionTests {
        @Test
        @DisplayName("Should return 401 with invalid credentials message")
        void handleInvalidCredentials_shouldReturn401_withInvalidCredentialsMessage() {
            // Arrange
            InvalidCredentialsException ex = new InvalidCredentialsException();

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleInvalidCredentials(ex);

            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Unauthorized", response.getBody().get("error"));
            assertEquals(SecurityConstants.INVALID_CREDENTIALS_ERROR, response.getBody().get("message"));
        }
    }

    @Nested
    @DisplayName("AccessDeniedException Tests")
    class AccessDeniedExceptionTests {
        @Test
        @DisplayName("Should return 403 with access denied message")
        void handleAccessDenied_shouldReturn403_withAccessDeniedMessage() {
            // Arrange
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleAccessDenied(ex);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Forbidden", response.getBody().get("error"));
            assertEquals("Access denied", response.getBody().get("message"));
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException Tests")
    class MethodArgumentNotValidExceptionTests {
        @Test
        @DisplayName("Should return 400 with validation error messages")
        void handleValidationException_shouldReturn400_withValidationErrorMessages() {
            // Arrange
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            List<FieldError> fieldErrors = new ArrayList<>();
            
            FieldError fieldError1 = new FieldError("object", "email", "Email is required");
            FieldError fieldError2 = new FieldError("object", "password", "Password is required");
            fieldErrors.add(fieldError1);
            fieldErrors.add(fieldError2);
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationException(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Validation Error", response.getBody().get("error"));
            assertTrue(response.getBody().get("message").contains("Email is required"));
            assertTrue(response.getBody().get("message").contains("Password is required"));
        }

        @Test
        @DisplayName("Should resolve localized message when message key is provided")
        void handleValidationException_shouldResolveLocalizedMessage_whenMessageKeyProvided() {
            // Arrange
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            List<FieldError> fieldErrors = new ArrayList<>();
            
            FieldError fieldError = new FieldError("object", "email", "{validation.email.required}");
            fieldErrors.add(fieldError);
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
            lenient().when(messageSource.getMessage(eq("validation.email.required"), isNull(), any(Locale.class)))
                    .thenReturn("Email is required");

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationException(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Validation Error", response.getBody().get("error"));
            assertEquals("Email is required", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should use default message when localized message not found")
        void handleValidationException_shouldUseDefaultMessage_whenLocalizedMessageNotFound() {
            // Arrange
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            List<FieldError> fieldErrors = new ArrayList<>();
            
            FieldError fieldError = new FieldError("object", "email", "{validation.email.required}");
            fieldErrors.add(fieldError);
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
            lenient().when(messageSource.getMessage(eq("validation.email.required"), isNull(), any(Locale.class)))
                    .thenThrow(new org.springframework.context.NoSuchMessageException("Message not found"));

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationException(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Validation Error", response.getBody().get("error"));
            assertEquals("{validation.email.required}", response.getBody().get("message"));
        }
    }

    @Nested
    @DisplayName("RuntimeException Tests")
    class RuntimeExceptionTests {
        @Test
        @DisplayName("Should return 400 for business logic errors - not found")
        void handleRuntimeException_shouldReturn400_forNotFoundError() {
            // Arrange
            RuntimeException ex = new RuntimeException("User not found");

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleRuntimeException(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Bad Request", response.getBody().get("error"));
            assertEquals("User not found", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should return 400 for business logic errors - expired token")
        void handleRuntimeException_shouldReturn400_forExpiredTokenError() {
            // Arrange
            RuntimeException ex = new RuntimeException("Invalid/expired refresh token");

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleRuntimeException(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Bad Request", response.getBody().get("error"));
        }

        @Test
        @DisplayName("Should return 500 for generic runtime exceptions")
        void handleRuntimeException_shouldReturn500_forGenericRuntimeException() {
            // Arrange
            RuntimeException ex = new RuntimeException("Unexpected error occurred");

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleRuntimeException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Internal Server Error", response.getBody().get("error"));
            assertEquals("An error occurred. Please try again later.", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should return 500 when exception message is null")
        void handleRuntimeException_shouldReturn500_whenExceptionMessageIsNull() {
            // Arrange
            RuntimeException ex = new RuntimeException((String) null);

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleRuntimeException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Internal Server Error", response.getBody().get("error"));
        }
    }

    @Nested
    @DisplayName("Generic Exception Tests")
    class GenericExceptionTests {
        @Test
        @DisplayName("Should return 500 with generic error message")
        void handleGenericException_shouldReturn500_withGenericErrorMessage() {
            // Arrange
            Exception ex = new Exception("Internal error");

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Internal Server Error", response.getBody().get("error"));
            assertEquals("An error occurred. Please try again later.", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should not expose internal error details")
        void handleGenericException_shouldNotExposeInternalErrorDetails() {
            // Arrange
            Exception ex = new Exception("Sensitive database connection error: password=secret123");

            // Act
            ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().get("message").contains("password"));
            assertFalse(response.getBody().get("message").contains("secret123"));
            assertEquals("An error occurred. Please try again later.", response.getBody().get("message"));
        }
    }
}

