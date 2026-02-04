package com.authenticationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authenticationservice.dto.ProfileResponse;
import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.util.LoggingSanitizer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRotationService refreshTokenRotationService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProfileResponse profileResponse = new ProfileResponse();
        profileResponse.setEmail(user.getEmail());
        profileResponse.setName(user.getName());
        profileResponse.setRoles(user.getRoles().stream().map(Role::getName).toList());
        profileResponse.setAuthProvider(user.getAuthProvider());

        return profileResponse;
    }

    @Transactional
    public void updateProfile(String email, ProfileUpdateRequest request) {
        log.debug("Updating profile for email: {}", maskEmail(email));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found for email: {}", maskEmail(email));
                    return new RuntimeException("User not found");
                });

        boolean passwordUpdated = false;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                log.error("Current password is missing for email: {}", maskEmail(email));
                throw new RuntimeException("Current password is required when updating password");
            }
            // Security: Never log password hashes or password validation details
            boolean passwordMatches = passwordEncoder.matches(request.getCurrentPassword(), user.getPassword());
            if (!passwordMatches) {
                log.error("Incorrect current password for email: {}", maskEmail(email));
                throw new RuntimeException("Incorrect current password");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            passwordUpdated = true;
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            log.debug("Updating name for email: {}", maskEmail(email));
            user.setName(request.getName());
        }
        userRepository.save(user);
        if (passwordUpdated) {
            refreshTokenRotationService.revokeForPasswordChange(user.getId());
        }
        log.debug("Profile updated successfully for email: {}", maskEmail(email));
    }

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }
}
