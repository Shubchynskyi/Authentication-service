package com.authenticationservice.service;

import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.BlockedEmail;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.BlockedEmailRepository;
import com.authenticationservice.util.EmailUtils;
import com.authenticationservice.util.LoggingSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for centralized access control checks (whitelist/blacklist logic).
 * Supports both WHITELIST and BLACKLIST modes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AllowedEmailRepository allowedEmailRepository;
    private final BlockedEmailRepository blockedEmailRepository;
    private final AccessModeService accessModeService;

    private String normalizeEmail(String email) {
        return EmailUtils.normalize(email);
    }

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }

    /**
     * Checks if email is allowed for registration (local or OAuth2).
     * 
     * WHITELIST mode: email must be in whitelist; blacklist is ignored for registration.
     * BLACKLIST mode: email must not be in blacklist.
     * 
     * @param email Email to check
     * @throws RuntimeException if email is not allowed for registration
     */
    public void checkRegistrationAccess(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("Checking registration access for email: {}", maskEmail(normalizedEmail));
        
        AccessMode currentMode = accessModeService.getCurrentMode();
        
        if (currentMode == AccessMode.WHITELIST) {
            // WHITELIST mode: email must be in whitelist
            Optional<AllowedEmail> allowed = allowedEmailRepository.findByEmail(normalizedEmail);
            if (allowed.isEmpty()) {
                log.error("Email {} is not in whitelist. Registration denied.", maskEmail(normalizedEmail));
                throw new RuntimeException("This email is not in whitelist. Registration is forbidden.");
            }
            log.debug("Email {} is in whitelist, registration allowed", maskEmail(normalizedEmail));
        } else {
            // BLACKLIST mode: email must not be in blacklist
            Optional<BlockedEmail> blocked = blockedEmailRepository.findByEmail(normalizedEmail);
            if (blocked.isPresent()) {
                log.error("Email {} is in blacklist. Registration denied.", maskEmail(normalizedEmail));
                throw new RuntimeException("This email is in blacklist. Registration is forbidden.");
            }
            log.debug("Email {} is not in blacklist, registration allowed", maskEmail(normalizedEmail));
        }
    }

    /**
     * Checks if email is allowed for login.
     * 
     * WHITELIST mode: email must not be in blacklist (blacklist blocks login even in whitelist mode).
     * BLACKLIST mode: email must not be in blacklist.
     * 
     * @param email Email to check
     * @throws RuntimeException if email is not allowed for login
     */
    public void checkLoginAccess(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("Checking login access for email: {}", maskEmail(normalizedEmail));
        
        // In both modes, blacklist blocks login
        Optional<BlockedEmail> blocked = blockedEmailRepository.findByEmail(normalizedEmail);
        if (blocked.isPresent()) {
            log.error("Email {} is in blacklist. Login denied.", maskEmail(normalizedEmail));
            throw new RuntimeException("This email is in blacklist. Login is forbidden.");
        }
        
        log.debug("Email {} is allowed for login", maskEmail(normalizedEmail));
    }
}

