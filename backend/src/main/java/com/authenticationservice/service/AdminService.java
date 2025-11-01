// AdminService.java
package com.authenticationservice.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;

import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final AllowedEmailRepository allowedEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public void addToWhitelist(String email) {
        if (allowedEmailRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists in whitelist");
        }
        AllowedEmail allowedEmail = new AllowedEmail(email);
        allowedEmailRepository.save(allowedEmail);
        log.info("Email added to whitelist: {}", email);
    }

    public void removeFromWhitelist(String email) {
        AllowedEmail existing = allowedEmailRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found in whitelist"));
        allowedEmailRepository.delete(existing);
        log.info("Email removed from whitelist: {}", email);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable, String search) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();
        
        if (search != null && !search.isEmpty()) {
            return userRepository.findByEmailNotAndEmailContainingOrNameContaining(currentUserEmail, search, search, pageable)
                .map(UserDTO::fromUser);
        }
        
        return userRepository.findByEmailNot(currentUserEmail, pageable)
            .map(UserDTO::fromUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDTO.fromUser(user);
    }

    @Transactional
    public void createUser(AdminUpdateUserRequest request) {
        log.info("Starting user creation process for email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getUsername());
        
        // Generate temporary password and verification token
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);
        String verificationToken = UUID.randomUUID().toString();
        
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setVerificationToken(verificationToken);
        
        // Set roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);
        
        // Set statuses (enabled is managed implicitly; admin UI controls only blocking)
        user.setBlocked(request.getIsBlocked());
        user.setEmailVerified(false);
        
        try {
            // Save user first
            user = userRepository.save(user);
            log.info("User saved to database: {}", user.getEmail());

            // Send welcome email with temporary password and verification link
            String emailContent = getContent(verificationToken, user, tempPassword);

            emailService.sendEmail(
                user.getEmail(), 
                "Welcome to Authentication Service", 
                emailContent
            );
            
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error during user creation process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    private String getContent(String verificationToken, User user, String tempPassword) {
        String verificationLink = String.format("%s/verify/email?verificationToken=%s&email=%s",
            frontendUrl, verificationToken, user.getEmail());

        return String.format(
            "Your account has been created by administrator.\n\n" +
            "Your temporary password: %s\n" +
            "Please verify your email by clicking this link: %s\n\n" +
            "After verification, you can log in and change your password.\n" +
            "For security reasons, please change your password after first login.",
                tempPassword, verificationLink
        );
    }

    @Transactional
    public UserDTO updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if admin is trying to block themselves (use current persisted email)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equalsIgnoreCase(user.getEmail()) && Boolean.TRUE.equals(request.getIsBlocked())) {
            throw new RuntimeException("Admin cannot block themselves");
        }

        // Update basic info (admin-only endpoint, but roles are handled separately)
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setName(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() &&
                !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("User with this email already exists");
            }
            user.setEmail(request.getEmail());
        }

        // Handle blocking/unblocking and timestamps
        if (request.getIsBlocked() != null) {
            if (Boolean.TRUE.equals(request.getIsBlocked()) && !user.isBlocked()) {
                user.setBlockedAt(LocalDateTime.now());
                user.setBlockReason(request.getBlockReason());
                log.info("User {} blocked. Reason: {}", user.getEmail(), request.getBlockReason());
            } else if (Boolean.FALSE.equals(request.getIsBlocked()) && user.isBlocked()) {
                user.setUnblockedAt(LocalDateTime.now());
                user.setBlockReason(null); // Clear block reason when unblocking
                log.info("User {} unblocked", user.getEmail());
            }
            user.setBlocked(request.getIsBlocked());
        }
        // Only set block reason if user is being blocked (not unblocked)
        if (request.getBlockReason() != null && Boolean.TRUE.equals(request.getIsBlocked())) {
            user.setBlockReason(request.getBlockReason());
        }

        // Roles from this request are intentionally ignored.
        // Use the dedicated roles endpoint to change roles.

        return UserDTO.fromUser(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        log.warn("Deleting user: {}", user.getEmail());
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> getWhitelist() {
        return allowedEmailRepository.findAll()
                .stream().map(AllowedEmail::getEmail)
                .toList();
    }

    public boolean verifyAdminPassword(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is admin
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            throw new RuntimeException("Insufficient permissions");
        }

        return passwordEncoder.matches(password, user.getPassword());
    }

    @Transactional
    public UserDTO updateUserRoles(Long id, List<String> roles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (roles == null || roles.isEmpty()) {
            throw new RuntimeException("Roles list cannot be empty");
        }

        // Build new roles set from provided names
        Set<Role> newRoles = new HashSet<>();
        for (String roleName : roles) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            newRoles.add(role);
        }

        // Prevent removing admin role from yourself
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean editingSelf = auth != null && auth.getName().equalsIgnoreCase(user.getEmail());
        boolean hadAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        boolean willHaveAdmin = newRoles.stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        if (editingSelf && hadAdmin && !willHaveAdmin) {
            throw new RuntimeException("Admin cannot remove their own admin role");
        }

        user.setRoles(newRoles);
        return UserDTO.fromUser(userRepository.save(user));
    }
}
