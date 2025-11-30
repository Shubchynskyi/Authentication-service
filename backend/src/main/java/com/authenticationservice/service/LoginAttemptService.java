package com.authenticationservice.service;

import com.authenticationservice.constants.EmailConstants;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLogin(User user, String frontendUrl) {
        // Reload user from database to get current state and avoid detached entity issues
        // Try to find by ID first, if not found (e.g., due to transaction isolation), try by email
        User dbUser = null;
        if (user.getId() != null) {
            dbUser = userRepository.findById(user.getId()).orElse(null);
        }
        if (dbUser == null) {
            dbUser = userRepository.findByEmail(user.getEmail()).orElse(null);
        }
        if (dbUser == null) {
            log.error("User not found in LoginAttemptService for email: {}, ID: {}", user.getEmail(), user.getId());
            // If user not found, we can't increment attempts, but this shouldn't break the login flow
            // Just log the error and return - the InvalidCredentialsException will still be thrown
            return;
        }

        dbUser.incrementFailedLoginAttempts();
        int currentAttempts = dbUser.getFailedLoginAttempts();

        // Check if we just reached 5 attempts (temporary lock)
        if (currentAttempts == 5 && dbUser.getLockTime() == null) {
            dbUser.setLockTime(LocalDateTime.now().plusMinutes(5));
            userRepository.save(dbUser);
            
            // Send email about temporary lock
            String emailContent = String.format(
                    EmailConstants.ACCOUNT_TEMPORARILY_LOCKED_TEMPLATE,
                    5, frontendUrl, 5);
            emailService.sendEmail(dbUser.getEmail(), EmailConstants.ACCOUNT_TEMPORARILY_LOCKED_SUBJECT, emailContent);
            log.info("Temporary lock email sent to {}", dbUser.getEmail());
        } else if (currentAttempts > 5 && dbUser.getLockTime() == null) {
            // Set lock time if it wasn't set before
            dbUser.setLockTime(LocalDateTime.now().plusMinutes(5));
        }

        userRepository.save(dbUser);

        // Check if we just reached 10 attempts (full block)
        if (currentAttempts == 10 && dbUser.isBlocked()) {
            // Send email about full block
            String emailContent = String.format(
                    EmailConstants.ACCOUNT_BLOCKED_TEMPLATE,
                    frontendUrl);
            emailService.sendEmail(dbUser.getEmail(), EmailConstants.ACCOUNT_BLOCKED_SUBJECT, emailContent);
            log.info("Account blocked email sent to {}", dbUser.getEmail());
        }
    }
}
