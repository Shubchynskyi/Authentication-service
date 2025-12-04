package com.authenticationservice.validation;

import com.authenticationservice.service.PasswordValidationService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordValidator Tests")
class PasswordValidatorTest {

    @Mock
    private PasswordValidationService passwordValidationService;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @InjectMocks
    private PasswordValidator passwordValidator;

    private PasswordValid passwordValidAnnotation;

    @BeforeEach
    void setUp() {
        passwordValidAnnotation = mock(PasswordValid.class);
    }

    @Test
    @DisplayName("Should initialize successfully")
    void initialize_shouldInitializeSuccessfully() {
        // Act & Assert
        assertDoesNotThrow(() -> passwordValidator.initialize(passwordValidAnnotation));
    }

    @Test
    @DisplayName("Should return true when password is valid")
    void isValid_shouldReturnTrue_whenPasswordIsValid() {
        // Arrange
        String validPassword = "ValidPassword123@";
        when(passwordValidationService.isValid(validPassword)).thenReturn(true);

        // Act
        boolean result = passwordValidator.isValid(validPassword, constraintValidatorContext);

        // Assert
        assertTrue(result, "Should return true for valid password");
        verify(passwordValidationService).isValid(validPassword);
        verify(constraintValidatorContext, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("Should return false when password is invalid")
    void isValid_shouldReturnFalse_whenPasswordIsInvalid() {
        // Arrange
        String invalidPassword = "short";
        when(passwordValidationService.isValid(invalidPassword)).thenReturn(false);

        // Act
        boolean result = passwordValidator.isValid(invalidPassword, constraintValidatorContext);

        // Assert
        assertFalse(result, "Should return false for invalid password");
        verify(passwordValidationService).isValid(invalidPassword);
    }

    @Test
    @DisplayName("Should return false when password is null")
    void isValid_shouldReturnFalse_whenPasswordIsNull() {
        // Arrange
        when(passwordValidationService.isValid(null)).thenReturn(false);

        // Act
        boolean result = passwordValidator.isValid(null, constraintValidatorContext);

        // Assert
        assertFalse(result, "Should return false for null password");
        verify(passwordValidationService).isValid(null);
    }

    @Test
    @DisplayName("Should return false when password is empty")
    void isValid_shouldReturnFalse_whenPasswordIsEmpty() {
        // Arrange
        String emptyPassword = "";
        when(passwordValidationService.isValid(emptyPassword)).thenReturn(false);

        // Act
        boolean result = passwordValidator.isValid(emptyPassword, constraintValidatorContext);

        // Assert
        assertFalse(result, "Should return false for empty password");
        verify(passwordValidationService).isValid(emptyPassword);
    }

    @Test
    @DisplayName("Should delegate validation to PasswordValidationService")
    void isValid_shouldDelegateToPasswordValidationService() {
        // Arrange
        String password = "TestPassword123@";
        when(passwordValidationService.isValid(password)).thenReturn(true);

        // Act
        passwordValidator.isValid(password, constraintValidatorContext);

        // Assert
        verify(passwordValidationService, times(1)).isValid(password);
        verifyNoMoreInteractions(passwordValidationService);
    }

    @Test
    @DisplayName("Should handle different password formats")
    void isValid_shouldHandleDifferentPasswordFormats() {
        // Test case 1: Valid password with all requirements
        String validPassword1 = "ComplexPass123!@#";
        when(passwordValidationService.isValid(validPassword1)).thenReturn(true);
        assertTrue(passwordValidator.isValid(validPassword1, constraintValidatorContext));

        // Test case 2: Password without special characters
        String invalidPassword1 = "NoSpecialChar123";
        when(passwordValidationService.isValid(invalidPassword1)).thenReturn(false);
        assertFalse(passwordValidator.isValid(invalidPassword1, constraintValidatorContext));

        // Test case 3: Password without uppercase
        String invalidPassword2 = "nouppercase123!";
        when(passwordValidationService.isValid(invalidPassword2)).thenReturn(false);
        assertFalse(passwordValidator.isValid(invalidPassword2, constraintValidatorContext));

        // Test case 4: Password without lowercase
        String invalidPassword3 = "NOLOWERCASE123!";
        when(passwordValidationService.isValid(invalidPassword3)).thenReturn(false);
        assertFalse(passwordValidator.isValid(invalidPassword3, constraintValidatorContext));

        // Test case 5: Password without digits
        String invalidPassword4 = "NoDigitsHere!@";
        when(passwordValidationService.isValid(invalidPassword4)).thenReturn(false);
        assertFalse(passwordValidator.isValid(invalidPassword4, constraintValidatorContext));

        // Verify all calls were made
        verify(passwordValidationService, times(5)).isValid(anyString());
    }
}

