package com.authenticationservice.service;

import com.authenticationservice.config.PasswordValidationConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for password validation.
 * Provides a single point of access for password validation logic.
 * Can be used both programmatically in services and through annotations via PasswordValidator.
 */
@Service
@RequiredArgsConstructor
public class PasswordValidationService {
    
    private final PasswordValidationConfig passwordValidationConfig;
    
    /**
     * Validates password against configured pattern.
     * 
     * @param password password to validate
     * @return true if password is valid, false otherwise
     */
    public boolean isValid(String password) {
        if (password == null) {
            return true; // Let @NotNull handle null validation
        }
        
        String pattern = passwordValidationConfig.getPattern();
        if (pattern == null || pattern.isEmpty()) {
            // Fallback to default pattern if not configured
            pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!\\-_*?])(?=\\S+$).{8,}$";
        }
        
        return password.matches(pattern);
    }
}

