package com.authenticationservice.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import com.authenticationservice.dto.AccessListUpdateResponse;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.AllowedEmailDTO;
import com.authenticationservice.dto.BlockedEmailDTO;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.constants.EmailConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.exception.AccessListDuplicateException;
import com.authenticationservice.model.AccessListChangeLog;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeChangeLog;
import com.authenticationservice.model.AccessModeSettings;
import com.authenticationservice.model.MaskedLoginSettings;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.BlockedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AccessListChangeLogRepository;
import com.authenticationservice.repository.AccessModeChangeLogRepository;
import com.authenticationservice.repository.AccessModeSettingsRepository;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.BlockedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.util.EmailTemplateFactory;
import com.authenticationservice.util.EmailUtils;
import com.authenticationservice.util.LoggingSanitizer;
import com.authenticationservice.service.MaskedLoginService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "com.authenticationservice.admin")
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final AllowedEmailRepository allowedEmailRepository;
    private final BlockedEmailRepository blockedEmailRepository;
    private final AccessListChangeLogRepository accessListChangeLogRepository;
    private final AccessModeSettingsRepository accessModeSettingsRepository;
    private final AccessModeChangeLogRepository accessModeChangeLogRepository;
    private final AccessModeService accessModeService;
    private final MaskedLoginService maskedLoginService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final MessageSource messageSource;
    private final EmailTemplateFactory emailTemplateFactory;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public void addToWhitelist(String email) {
        addToWhitelist(email, null);
    }

    public void addToWhitelist(String email, String reason) {
        String normalizedEmail = EmailUtils.normalize(email);
        if (allowedEmailRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AccessListDuplicateException(
                    AccessListChangeLog.AccessListType.WHITELIST,
                    "email.duplicate.whitelist",
                    getMessage("email.duplicate.whitelist"));
        }
        String normalizedReason = reason != null ? reason.trim() : "";
        AllowedEmail allowedEmail = new AllowedEmail(normalizedEmail,
                normalizedReason.isEmpty() ? null : normalizedReason);
        allowedEmailRepository.save(allowedEmail);
        log.info("Email added to whitelist: {}", maskEmail(normalizedEmail));

        logAccessListChange(AccessListChangeLog.AccessListType.WHITELIST, normalizedEmail,
                AccessListChangeLog.AccessListAction.ADD, normalizedReason);
    }

    public void removeFromWhitelist(String email) {
        removeFromWhitelist(email, null);
    }

    public void removeFromWhitelist(String email, String reason) {
        String normalizedEmail = EmailUtils.normalize(email);
        AllowedEmail existing = allowedEmailRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Email not found in whitelist"));
        allowedEmailRepository.delete(existing);
        log.info("Email removed from whitelist: {}", maskEmail(normalizedEmail));

        logAccessListChange(AccessListChangeLog.AccessListType.WHITELIST, normalizedEmail,
                AccessListChangeLog.AccessListAction.REMOVE, reason);
    }

    public AccessListUpdateResponse addToBlacklist(String email, String reason) {
        String normalizedEmail = EmailUtils.normalize(email);
        if (blockedEmailRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AccessListDuplicateException(
                    AccessListChangeLog.AccessListType.BLACKLIST,
                    "email.duplicate.blacklist",
                    getMessage("email.duplicate.blacklist"));
        }
        String normalizedReason = reason != null ? reason.trim() : "";
        BlockedEmail blockedEmail = new BlockedEmail(normalizedEmail,
                normalizedReason.isEmpty() ? null : normalizedReason);
        blockedEmailRepository.save(blockedEmail);
        log.info("Email added to blacklist: {}", maskEmail(normalizedEmail));

        boolean userBlocked = false;
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            User existingUser = userRepository.findByEmail(normalizedEmail).orElseThrow();
            existingUser.setBlocked(true);
            existingUser.setBlockReason(normalizedReason.isEmpty() ? "Email is blacklisted" : normalizedReason);
            userRepository.save(existingUser);
            userBlocked = true;
            log.info("Existing user {} marked as blocked due to blacklist", maskEmail(normalizedEmail));
        }

        logAccessListChange(AccessListChangeLog.AccessListType.BLACKLIST, normalizedEmail,
                AccessListChangeLog.AccessListAction.ADD, reason);

        String message = userBlocked
                ? "Email added to blacklist. Existing user has been blocked and will not be able to login."
                : "Email added to blacklist.";
        return new AccessListUpdateResponse(message, userBlocked, blockedEmail.getReason());
    }

    public void removeFromBlacklist(String email, String reason) {
        String normalizedEmail = EmailUtils.normalize(email);
        BlockedEmail existing = blockedEmailRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Email not found in blacklist"));
        blockedEmailRepository.delete(existing);
        log.info("Email removed from blacklist: {}", maskEmail(normalizedEmail));

        logAccessListChange(AccessListChangeLog.AccessListType.BLACKLIST, normalizedEmail,
                AccessListChangeLog.AccessListAction.REMOVE, reason);
    }

    @Transactional(readOnly = true)
    public List<BlockedEmailDTO> getBlacklist() {
        return blockedEmailRepository.findAll()
                .stream()
                .map(entry -> new BlockedEmailDTO(entry.getEmail(), entry.getReason()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable, String search) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();

        if (search != null && !search.isEmpty()) {
            return userRepository
                    .findByEmailNotAndEmailContainingOrNameContaining(currentUserEmail, search, search, pageable)
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
        String normalizedEmail = EmailUtils.normalize(request.getEmail());
        request.setEmail(normalizedEmail);
        log.info("Starting user creation process for email: {}", maskEmail(normalizedEmail));

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setName(request.getUsername());

        String tempPassword = UUID.randomUUID().toString().substring(0, 12);
        String verificationToken = UUID.randomUUID().toString();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setVerificationToken(verificationToken);

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
            user = userRepository.save(user);
            log.info("User saved to database: {}", maskEmail(user.getEmail()));

            // Automatically add to whitelist and remove from blacklist if present
            String reason = MessageConstants.WHITELIST_REASON_ADMIN_CREATED;
            if (allowedEmailRepository.findByEmail(user.getEmail()).isEmpty()) {
                AllowedEmail allowedEmail = new AllowedEmail(user.getEmail(), reason);
                allowedEmailRepository.save(allowedEmail);
                log.info("Email {} automatically added to whitelist", maskEmail(user.getEmail()));
                logAccessListChange(AccessListChangeLog.AccessListType.WHITELIST, user.getEmail(),
                        AccessListChangeLog.AccessListAction.ADD, reason);
            }

            if (blockedEmailRepository.findByEmail(user.getEmail()).isPresent()) {
                BlockedEmail blocked = blockedEmailRepository.findByEmail(user.getEmail()).orElseThrow();
                blockedEmailRepository.delete(blocked);
                log.info("Email {} automatically removed from blacklist", maskEmail(user.getEmail()));
                logAccessListChange(AccessListChangeLog.AccessListType.BLACKLIST, user.getEmail(),
                        AccessListChangeLog.AccessListAction.REMOVE, reason);
            }

            String verificationLink = String.format("%s/verify/email?verificationToken=%s&email=%s",
                    frontendUrl, verificationToken, urlEncode(user.getEmail()));

            String emailText = emailTemplateFactory.buildAdminInviteText(tempPassword, verificationLink);
            String emailHtml = emailTemplateFactory.buildAdminInviteHtml(tempPassword, verificationLink);

            emailService.sendEmail(
                    user.getEmail(),
                    EmailConstants.ADMIN_INVITE_SUBJECT,
                    emailText,
                    emailHtml);

            log.info("Welcome email sent to: {}", maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("Error during user creation process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    @Transactional
    public UserDTO updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent admin from blocking themselves
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equalsIgnoreCase(user.getEmail())
                && Boolean.TRUE.equals(request.getIsBlocked())) {
            throw new RuntimeException("Admin cannot block themselves");
        }

        // Roles are handled separately via dedicated endpoint
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setName(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() &&
                !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            String normalizedEmail = EmailUtils.normalize(request.getEmail());
            request.setEmail(normalizedEmail);
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new RuntimeException("User with this email already exists");
            }
            user.setEmail(normalizedEmail);
        }

        if (request.getIsBlocked() != null) {
            if (Boolean.TRUE.equals(request.getIsBlocked()) && !user.isBlocked()) {
                user.setBlockedAt(LocalDateTime.now());
                user.setBlockReason(request.getBlockReason());
                log.info("User {} blocked. Reason: {}", maskEmail(user.getEmail()), request.getBlockReason());
            } else if (Boolean.FALSE.equals(request.getIsBlocked()) && user.isBlocked()) {
                user.setUnblockedAt(LocalDateTime.now());
                user.setBlockReason(null); // Clear block reason when unblocking
                log.info("User {} unblocked", maskEmail(user.getEmail()));
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
        log.warn("Deleting user: {}", maskEmail(user.getEmail()));
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AllowedEmailDTO> getWhitelist() {
        return allowedEmailRepository.findAll()
                .stream()
                .map(entry -> new AllowedEmailDTO(entry.getEmail(), entry.getReason()))
                .toList();
    }

    public boolean verifyAdminPassword(String email, String password) {
        String normalizedEmail = EmailUtils.normalize(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

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

    @Transactional(readOnly = true)
    public AccessMode getCurrentAccessMode() {
        return accessModeService.getCurrentMode();
    }

    @Transactional(readOnly = true)
    public AccessModeSettings getAccessModeSettings() {
        return accessModeService.getSettings();
    }

    /**
     * Changes access mode with password and OTP verification.
     * 
     * @param newMode       New access mode
     * @param adminEmail    Admin email
     * @param adminPassword Admin password
     * @param otpCode       OTP code sent to admin email
     * @param reason        Reason for mode change
     */
    @Transactional
    public void changeAccessMode(AccessMode newMode, String adminEmail, String adminPassword,
            String otpCode, String reason) {
        String normalizedAdminEmail = EmailUtils.normalize(adminEmail);
        if (!verifyAdminPassword(normalizedAdminEmail, adminPassword)) {
            throw new RuntimeException("Invalid admin password");
        }

        if (!otpService.validateOtp(normalizedAdminEmail, otpCode)) {
            throw new RuntimeException("Invalid or expired OTP code");
        }

        Long settingsId = 1L;
        AccessModeSettings settings = accessModeSettingsRepository.findById(settingsId)
                .orElseThrow(() -> new RuntimeException("Access mode settings not found"));

        AccessMode oldMode = settings.getMode();

        // Don't change if already in requested mode
        if (oldMode == newMode) {
            throw new RuntimeException("Access mode is already " + newMode);
        }

        settings.setMode(newMode);
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(normalizedAdminEmail);
        settings.setReason(reason);
        accessModeSettingsRepository.save(settings);

        AccessModeChangeLog changeLog = new AccessModeChangeLog();
        changeLog.setOldMode(oldMode);
        changeLog.setNewMode(newMode);
        changeLog.setChangedBy(normalizedAdminEmail);
        changeLog.setChangedAt(LocalDateTime.now());
        changeLog.setReason(reason);
        accessModeChangeLogRepository.save(changeLog);

        log.info("Access mode changed from {} to {} by {} (reason: {})", oldMode, newMode,
                maskEmail(normalizedAdminEmail), reason);
    }

    /**
     * Generates and sends OTP code to admin email for mode change verification.
     * 
     * @param adminEmail Admin email
     */
    public void sendModeChangeOtp(String adminEmail) {
        String normalizedAdminEmail = EmailUtils.normalize(adminEmail);
        String otp = otpService.generateOtp(normalizedAdminEmail);

        String emailText = emailTemplateFactory.buildOtpAccessModeText(otp);
        String emailHtml = emailTemplateFactory.buildOtpAccessModeHtml(otp);

        emailService.sendEmail(normalizedAdminEmail, EmailConstants.OTP_ACCESS_MODE_SUBJECT, emailText, emailHtml);
        log.info("OTP sent to admin email: {}", maskEmail(normalizedAdminEmail));
    }

    /**
     * Logs access list changes to AccessListChangeLog.
     */
    private void logAccessListChange(AccessListChangeLog.AccessListType listType, String email,
            AccessListChangeLog.AccessListAction action, String reason) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String changedBy = auth != null ? auth.getName() : "system";

        AccessListChangeLog logEntry = new AccessListChangeLog();
        logEntry.setListType(listType);
        logEntry.setEmail(email);
        logEntry.setAction(action);
        logEntry.setChangedBy(changedBy);
        logEntry.setChangedAt(LocalDateTime.now());
        logEntry.setReason(reason);

        accessListChangeLogRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public MaskedLoginSettings getMaskedLoginSettings() {
        return maskedLoginService.getSettings();
    }

    @Transactional
    public void updateMaskedLoginSettings(Boolean enabled, Integer templateId, String adminEmail, String adminPassword) {
        if (!verifyAdminPassword(adminEmail, adminPassword)) {
            throw new RuntimeException(MessageConstants.INVALID_PASSWORD);
        }
        maskedLoginService.updateSettings(enabled, templateId, adminEmail);
        log.info("Masked login settings updated: enabled={}, templateId={}, updatedBy={}",
                enabled, templateId, maskEmail(adminEmail));
    }
}
