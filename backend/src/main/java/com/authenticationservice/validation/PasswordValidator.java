package com.authenticationservice.validation;

import com.authenticationservice.service.PasswordValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Constraint validator for @PasswordValid annotation.
 * Acts as an adapter between Jakarta Bean Validation and PasswordValidationService.
 * All validation logic is delegated to PasswordValidationService.
 */
@Component
@RequiredArgsConstructor
public class PasswordValidator implements ConstraintValidator<PasswordValid, String> {
    
    private final PasswordValidationService passwordValidationService;
    
    @Override
    public void initialize(PasswordValid constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return passwordValidationService.isValid(password);
    }
}

