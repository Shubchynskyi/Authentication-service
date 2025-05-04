package com.authenticationservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.authenticationservice.config.AdminConfig;
import com.authenticationservice.model.User;
import com.authenticationservice.model.Role;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.repository.RoleRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInitializationService {

    private final AdminConfig adminConfig;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public void initializeAdmin() {
        if (!adminConfig.isEnabled()) {
            log.info("Admin initialization is disabled. Skipping.");
            return;
        }

        String adminEmail = adminConfig.getEmail();
        if (adminEmail == null || adminEmail.isEmpty()) {
            log.warn("Admin email not configured. Skipping admin initialization.");
            return;
        }

        User existingUser = userRepository.findByEmail(adminEmail)
                .orElse(null);

        if (existingUser != null) {
            boolean isAdmin = existingUser.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                        .orElseThrow(() -> new RuntimeException("Admin role not found"));
                existingUser.getRoles().add(adminRole);
                userRepository.save(existingUser);
                log.info("Added admin role to existing user: {}", adminEmail);
            }
        } else {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            String tempPassword = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(tempPassword);

            User newAdmin = new User();
            newAdmin.setEmail(adminEmail);
            newAdmin.setName(adminConfig.getUsername());
            newAdmin.setPassword(encodedPassword);
            newAdmin.setEnabled(true);
            newAdmin.setEmailVerified(true);
            newAdmin.getRoles().add(adminRole);

            userRepository.save(newAdmin);

            String resetToken = authService.generatePasswordResetToken(adminEmail);
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            String emailContent = String.format(
                    "Welcome to the system! To set your password, please follow the link: %s",
                    resetLink);

            emailService.sendEmail(
                    adminEmail,
                    "Setup password administrator",
                    emailContent);

            log.info("Created new admin user and sent setup email to: {}", adminEmail);
        }
    }
}