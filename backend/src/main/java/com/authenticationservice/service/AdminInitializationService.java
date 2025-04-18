package com.authenticationservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public void initializeAdmin() {
        if (!adminConfig.isEnabled()) {
            log.info("Admin initialization is disabled. Skipping.");
            return;
        }

        if (adminConfig.getEmail() == null || adminConfig.getEmail().isEmpty()) {
            log.warn("Admin email not configured. Skipping admin initialization.");
            return;
        }

        User existingUser = userRepository.findByEmail(adminConfig.getEmail())
            .orElse(null);

        if (existingUser != null) {
            boolean isAdmin = existingUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
                existingUser.getRoles().add(adminRole);
                userRepository.save(existingUser);
                log.info("Added admin role to existing user: {}", adminConfig.getEmail());
            }
        } else {
            String tempPassword = UUID.randomUUID().toString();
            User newAdmin = new User();
            newAdmin.setEmail(adminConfig.getEmail());
            newAdmin.setName(adminConfig.getUsername());
            newAdmin.setPassword(tempPassword);
            newAdmin.setEnabled(true);
            newAdmin.setEmailVerified(true);

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Admin role not found"));
            newAdmin.getRoles().add(adminRole);

            userRepository.save(newAdmin);

            String resetToken = authService.generatePasswordResetToken(newAdmin.getEmail());
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            
            String emailContent = String.format(
                "Welcome to the system! To set the password, please follow the link: %s",
                resetLink
            );

            emailService.sendEmail(
                newAdmin.getEmail(),
                "Setup password administrator",
                emailContent
            );

            log.info("Created new admin user and sent setup email to: {}", adminConfig.getEmail());
        }
    }
} 