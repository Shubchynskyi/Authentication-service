package com.authenticationservice.service;

import com.authenticationservice.constants.EmailConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.VerificationRequest;
import com.authenticationservice.exception.AccountBlockedException;
import com.authenticationservice.exception.AccountLockedException;
import com.authenticationservice.exception.InvalidCredentialsException;
import com.authenticationservice.exception.InvalidVerificationCodeException;
import com.authenticationservice.exception.TooManyRequestsException;
import com.authenticationservice.model.AuthProvider;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;
import com.authenticationservice.util.EmailTemplateFactory;
import com.authenticationservice.util.EmailUtils;
import com.authenticationservice.util.LoggingSanitizer;
import com.authenticationservice.util.StructuredLogger;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AccessControlService accessControlService;
    private final MessageSource messageSource;
    private final RateLimitingService rateLimitingService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${password.reset.cooldown-minutes:10}")
    private int passwordResetCooldownMinutes;

    private String normalizeEmail(String email) {
        return EmailUtils.normalize(email);
    }

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }

    @Transactional
    public void register(RegistrationRequest request) {
        long startTime = System.currentTimeMillis();
        String normalizedEmail = normalizeEmail(request.getEmail());
        request.setEmail(normalizedEmail);
        log.info("Starting registration process for email: {}", maskEmail(normalizedEmail));

        try {
            // Check access control (whitelist/blacklist) - this must be the first check
            accessControlService.checkRegistrationAccess(normalizedEmail);

            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                log.error("User with email {} already exists", maskEmail(normalizedEmail));
                throw new RuntimeException("User with this email already exists.");
            }

            String verificationToken = UUID.randomUUID().toString();

            User user = new User();
            user.setEmail(normalizedEmail);
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
            sendVerificationEmail(normalizedEmail, verificationToken);
            log.info("Verification email sent to {}", maskEmail(normalizedEmail));
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            StructuredLogger.logPerformance(log, "register", duration,
                    "email", maskEmail(normalizedEmail));
        }
    }

    private void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = String.format(
                "%s/verify/email?verificationToken=%s&email=%s",
                frontendUrl, token, urlEncode(toEmail));

        String emailText = EmailTemplateFactory.buildVerificationText(verificationLink);
        String emailHtml = EmailTemplateFactory.buildVerificationHtml(verificationLink);

        emailService.sendEmail(toEmail, EmailConstants.VERIFICATION_SUBJECT, emailText, emailHtml);
    }

    public void verifyEmail(VerificationRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        request.setEmail(normalizedEmail);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(request.getCode())) {
            throw new InvalidVerificationCodeException(
                    getMessage(MessageConstants.VERIFICATION_CODE_INVALID_OR_EXPIRED));
        }

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public Map<String, String> login(LoginRequest request) {
        long startTime = System.currentTimeMillis();
        String normalizedEmail = normalizeEmail(request.getEmail());
        request.setEmail(normalizedEmail);
        log.debug("Login attempt for email: {}", maskEmail(normalizedEmail));

        try {
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> {
                        log.error("User not found for email: {}", maskEmail(normalizedEmail));
                        return new InvalidCredentialsException();
                    });
            log.debug("User found: {}", maskEmail(user.getEmail()));
            log.debug("User enabled: {}", user.isEnabled());
            log.debug("User blocked: {}", user.isBlocked());
            log.debug("User verified: {}", user.isEmailVerified());

            // Check access control (whitelist/blacklist) - this must be the first check
            accessControlService.checkLoginAccess(normalizedEmail);

            // Check disabled account before any other validation
            if (!user.isEnabled()) {
                log.error("Account is disabled for email: {}", maskEmail(normalizedEmail));
                throw new RuntimeException("Account is disabled");
            }

            if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
                log.error("Account is temporarily locked for email: {}", maskEmail(normalizedEmail));
                long seconds = java.time.Duration.between(LocalDateTime.now(), user.getLockTime()).getSeconds();
                throw new AccountLockedException(seconds);
            }

            if (user.isBlocked()) {
                log.error("Account is blocked for email: {}", maskEmail(normalizedEmail));
                throw new AccountBlockedException(user.getBlockReason());
            }

            boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
            if (!passwordMatches) {
                // Use separate service with REQUIRES_NEW transaction to ensure counter is saved
                loginAttemptService.handleFailedLogin(user, frontendUrl);

                log.error("Invalid password for email: {}", maskEmail(normalizedEmail));
                throw new InvalidCredentialsException();
            }

            if (!user.isEmailVerified()) {
                log.error("Email not verified for email: {}", maskEmail(normalizedEmail));
                String verificationToken = UUID.randomUUID().toString();
                user.setVerificationToken(verificationToken);
                userRepository.save(user);

                sendVerificationEmail(user.getEmail(), verificationToken);

                throw new RuntimeException("EMAIL_NOT_VERIFIED:" + user.getEmail());
            }

            log.debug("All validations passed for email: {}", maskEmail(normalizedEmail));
            user.resetFailedLoginAttempts();
            user.setLockTime(null);
            userRepository.save(user);

            try {
                String accessToken = jwtTokenProvider.generateAccessToken(user);
                Integer rememberDays = resolveRememberDays(request.getRememberDevice(), request.getRememberDays());
                String refreshToken = rememberDays != null
                        ? jwtTokenProvider.generateRefreshToken(user, rememberDays)
                        : jwtTokenProvider.generateRefreshToken(user);

                return buildTokenResponse(accessToken, refreshToken);
            } catch (Exception e) {
                log.error("Error generating tokens for email: {}, error: {}", maskEmail(normalizedEmail),
                        e.getMessage(),
                        e);
                throw new RuntimeException("Error generating tokens: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            StructuredLogger.logPerformance(log, "login", duration,
                    "email", maskEmail(normalizedEmail));
        }
    }

    public Map<String, String> refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }
        String email = normalizeEmail(jwtTokenProvider.getEmailFromRefresh(refreshToken));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

        accessControlService.checkLoginAccess(email);

        // Security: Check if user account is still active
        if (!user.isEnabled()) {
            log.error("Refresh token used for disabled account: {}", maskEmail(email));
            throw new RuntimeException(MessageConstants.ACCOUNT_DISABLED);
        }

        if (user.isBlocked()) {
            log.error("Refresh token used for blocked account: {}", maskEmail(email));
            throw new AccountBlockedException(user.getBlockReason());
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        Integer rememberDays = jwtTokenProvider.getRememberDaysFromRefresh(refreshToken);
        String newRefreshToken = rememberDays != null
                ? jwtTokenProvider.generateRefreshToken(user, rememberDays)
                : jwtTokenProvider.generateRefreshToken(user);

        return buildTokenResponse(newAccessToken, newRefreshToken);
    }

    private Integer resolveRememberDays(Boolean rememberDevice, Integer rememberDays) {
        if (rememberDevice == null || !rememberDevice) {
            return null;
        }
        int days = rememberDays != null ? rememberDays : 15;
        if (days != 15 && days != 30 && days != 60 && days != 90) {
            throw new RuntimeException(MessageConstants.INVALID_REMEMBER_DAYS);
        }
        return days;
    }

    public void resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified.");
        }

        Bucket bucket = rateLimitingService.resolveResendBucket(normalizedEmail);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
            throw new TooManyRequestsException(MessageConstants.RESEND_RATE_LIMIT_EXCEEDED, retryAfterSeconds);
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        userRepository.save(user);

        sendVerificationEmail(normalizedEmail, verificationToken);
    }

    public void initiatePasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            return;
        }

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            String emailContent = EmailTemplateFactory.buildGoogleResetText();
            String emailHtml = EmailTemplateFactory.buildGoogleResetHtml();
            emailService.sendEmail(
                    user.getEmail(),
                    EmailConstants.GOOGLE_PASSWORD_RESET_SUBJECT,
                    emailContent,
                    emailHtml);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastRequestedAt = user.getLastPasswordResetRequestedAt();
        if (lastRequestedAt != null && lastRequestedAt.plusMinutes(passwordResetCooldownMinutes).isAfter(now)) {
            log.info("Password reset request ignored due to cooldown for user {}", maskEmail(normalizedEmail));
            return;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        user.setLastPasswordResetRequestedAt(now);
        userRepository.save(user);

        sendPasswordResetEmail(normalizedEmail, resetToken);
    }

    private void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = String.format("%s/reset-password?token=%s", frontendUrl, urlEncode(token));
        String emailText = EmailTemplateFactory.buildResetPasswordText(resetLink);
        String emailHtml = EmailTemplateFactory.buildResetPasswordHtml(resetLink);
        emailService.sendEmail(toEmail, EmailConstants.RESET_PASSWORD_SUBJECT, emailText, emailHtml);
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
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
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

    public int getPasswordResetCooldownMinutes() {
        return passwordResetCooldownMinutes;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Transactional
    public Map<String, String> handleOAuth2Login(String email, String name) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("Handling OAuth2 login for email: {}, name: {}", maskEmail(normalizedEmail), name);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    log.info("Creating new OAuth2 user: {}", maskEmail(normalizedEmail));

                    // Check access control (whitelist/blacklist) - this must be the first check for
                    // new users
                    accessControlService.checkRegistrationAccess(normalizedEmail);

                    User newUser = new User();
                    newUser.setEmail(normalizedEmail);
                    // Use email as fallback if name is null or empty
                    newUser.setName((name != null && !name.isEmpty()) ? name : normalizedEmail);
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
        // If user registered locally but didn't verify email, and now logs in via
        // Google,
        // we should mark email as verified and update auth provider to GOOGLE
        if (user.getAuthProvider() == AuthProvider.LOCAL && !user.isEmailVerified()) {
            log.info("User {} with LOCAL provider and unverified email is logging in via Google. " +
                    "Updating emailVerified to true and authProvider to GOOGLE.", maskEmail(normalizedEmail));
            user.setEmailVerified(true);
            user.setAuthProvider(AuthProvider.GOOGLE);
            userRepository.save(user);
        }

        if (name != null && !name.isEmpty() && !name.equals(user.getName())) {
            log.debug("Updating user name from {} to {}", user.getName(), name);
            user.setName(name);
            userRepository.save(user);
        }

        if (!user.isEnabled()) {
            throw new RuntimeException(MessageConstants.ACCOUNT_DISABLED);
        }

        if (user.isBlocked()) {
            throw new RuntimeException("Account is blocked. " +
                    (user.getBlockReason() != null ? user.getBlockReason() : ""));
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.debug("Generated tokens for OAuth2 user: {}", maskEmail(normalizedEmail));
        return buildTokenResponse(accessToken, refreshToken);
    }

    private String getMessage(String key) {
        String resolvedKey = Objects.requireNonNull(key, "message key must not be null");
        return messageSource.getMessage(resolvedKey, null, LocaleContextHolder.getLocale());
    }

    private Map<String, String> buildTokenResponse(String accessToken, String refreshToken) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put(SecurityConstants.ACCESS_TOKEN_KEY, accessToken);
        tokens.put(SecurityConstants.REFRESH_TOKEN_KEY, refreshToken);
        return tokens;
    }
}