package com.authenticationservice.service;

import com.authenticationservice.constants.EmailConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.VerificationRequest;
import com.authenticationservice.exception.AccountBlockedException;
import com.authenticationservice.exception.AccountLockedException;
import com.authenticationservice.exception.InvalidCredentialsException;
import com.authenticationservice.model.AuthProvider;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AllowedEmailRepository allowedEmailRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JavaMailSender mailSender;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AccessControlService accessControlService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public void register(RegistrationRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        // Check access control (whitelist/blacklist) - this must be the first check
        accessControlService.checkRegistrationAccess(request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("User with email {} already exists", request.getEmail());
            throw new RuntimeException("User with this email already exists.");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setVerificationToken(verificationToken);
        user.setAuthProvider(AuthProvider.LOCAL);

        Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                .orElseThrow(() -> {
                    log.error("Role ROLE_USER not found in database");
                    return new RuntimeException("Role ROLE_USER not found in database.");
                });
        user.setRoles(Set.of(userRole));

        userRepository.save(user);
        log.info("User saved to database");

        // Security: Never log verification tokens
        sendVerificationEmail(request.getEmail(), verificationToken);
        log.info("Verification email sent to {}", request.getEmail());
    }

    private void sendVerificationEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(EmailConstants.VERIFICATION_SUBJECT);
        String emailText = String.format(EmailConstants.VERIFICATION_EMAIL_TEMPLATE, token);
        message.setText(emailText);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error sending verification email to {}", toEmail, e);
            throw new RuntimeException("Error sending verification email.", e);
        }
    }

    public void verifyEmail(VerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(request.getCode())) {
            throw new RuntimeException("Invalid verification code");
        }
        
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public Map<String, String> login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found for email: {}", request.getEmail());
                    return new InvalidCredentialsException();
                });
        log.debug("User found: {}", user.getEmail());
        log.debug("User enabled: {}", user.isEnabled());
        log.debug("User blocked: {}", user.isBlocked());
        log.debug("User verified: {}", user.isEmailVerified());

        // Check access control (whitelist/blacklist) - this must be the first check
        accessControlService.checkLoginAccess(request.getEmail());

        // Check disabled account before any other validation
        if (!user.isEnabled()) {
            log.error("Account is disabled for email: {}", request.getEmail());
            throw new RuntimeException("Account is disabled");
        }

        if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            log.error("Account is temporarily locked for email: {}", request.getEmail());
            long seconds = java.time.Duration.between(LocalDateTime.now(), user.getLockTime()).getSeconds();
            throw new AccountLockedException(seconds);
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) {
            // Use separate service with REQUIRES_NEW transaction to ensure counter is saved
            loginAttemptService.handleFailedLogin(user, frontendUrl);

            log.error("Invalid password for email: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        if (user.isBlocked()) {
            log.error("Account is blocked for email: {}", request.getEmail());
            throw new AccountBlockedException(user.getBlockReason());
        }

        if (!user.isEmailVerified()) {
            log.error("Email not verified for email: {}", request.getEmail());
            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            userRepository.save(user);

            String emailContent = String.format(
                    "To verify your email, use the code: %s",
                    verificationToken);
            emailService.sendEmail(user.getEmail(), "Email verification", emailContent);

            throw new RuntimeException("EMAIL_NOT_VERIFIED:" + user.getEmail());
        }

        log.debug("All validations passed for email: {}", request.getEmail());
        user.resetFailedLoginAttempts();
        user.setLockTime(null);
        userRepository.save(user);

        try {
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            Map<String, String> tokens = new java.util.HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            return tokens;
        } catch (Exception e) {
            log.error("Error generating tokens for email: {}, error: {}", request.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Error generating tokens: "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    public Map<String, String> refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid/expired refresh token");
        }
        String email = jwtTokenProvider.getEmailFromRefresh(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

        // Check access control (whitelist/blacklist)
        accessControlService.checkLoginAccess(email);

        // Security: Check if user account is still active
        if (!user.isEnabled()) {
            log.error("Refresh token used for disabled account: {}", email);
            throw new RuntimeException("Account is disabled");
        }
        
        if (user.isBlocked()) {
            log.error("Refresh token used for blocked account: {}", email);
            throw new AccountBlockedException(user.getBlockReason());
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);

        return result;
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified.");
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        userRepository.save(user);

        sendVerificationEmail(email, verificationToken);
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return;
        }

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            String emailContent = "We noticed you requested a password reset, but your account is linked with Google.\n"
                    +
                    "Please sign in using the 'Continue with Google' option.\n" +
                    "If you forgot your Google password, use Google's account recovery.";
            emailService.sendEmail(user.getEmail(), "Password reset unavailable for Google sign-in", emailContent);
            return;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        sendPasswordResetEmail(email, resetToken);
    }

    private void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(EmailConstants.RESET_PASSWORD_SUBJECT);
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String emailText = String.format(EmailConstants.RESET_PASSWORD_EMAIL_TEMPLATE, resetLink);
        message.setText(emailText);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error sending password reset email to {}", toEmail, e);
            throw new RuntimeException("Error sending password reset email.", e);
        }
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));

        if (user.getResetPasswordTokenExpiry() == null || 
            user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired reset token.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }

    public String generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        return token;
    }

    @Transactional(readOnly = true)
    public String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    @Transactional
    public Map<String, String> handleOAuth2Login(String email, String name) {
        log.debug("Handling OAuth2 login for email: {}, name: {}", email, name);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creating new OAuth2 user: {}", email);
                    
                    // Check access control (whitelist/blacklist) - this must be the first check for new users
                    accessControlService.checkRegistrationAccess(email);
                    
                    User newUser = new User();
                    newUser.setEmail(email);
                    // Use email as fallback if name is null or empty
                    newUser.setName((name != null && !name.isEmpty()) ? name : email);
                    newUser.setEnabled(true);
                    newUser.setEmailVerified(true);
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setAuthProvider(AuthProvider.GOOGLE);

                    Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Role ROLE_USER not found"));
                    newUser.setRoles(Set.of(userRole));

                    User savedUser = userRepository.save(newUser);
                    log.info("Created new OAuth2 user with ID: {}", savedUser.getId());
                    return savedUser;
                });

        // Handle existing user with LOCAL provider and unverified email
        // If user registered locally but didn't verify email, and now logs in via Google,
        // we should mark email as verified and update auth provider to GOOGLE
        if (user.getAuthProvider() == AuthProvider.LOCAL && !user.isEmailVerified()) {
            log.info("User {} with LOCAL provider and unverified email is logging in via Google. " +
                    "Updating emailVerified to true and authProvider to GOOGLE.", email);
            user.setEmailVerified(true);
            user.setAuthProvider(AuthProvider.GOOGLE);
            userRepository.save(user);
        }

        // Update name if it changed (for existing users)
        if (name != null && !name.isEmpty() && !name.equals(user.getName())) {
            log.debug("Updating user name from {} to {}", user.getName(), name);
            user.setName(name);
            userRepository.save(user);
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        if (user.isBlocked()) {
            throw new RuntimeException("Account is blocked. " +
                    (user.getBlockReason() != null ? user.getBlockReason() : ""));
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.debug("Generated tokens for OAuth2 user: {}", email);
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken);
    }
}