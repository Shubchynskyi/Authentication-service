package com.authenticationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.EmailConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.VerificationRequest;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.AuthProvider;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;

import java.util.*;
import java.time.LocalDateTime;

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

    @Value("${frontend.url}")
    private String frontendUrl;

    public void register(RegistrationRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());
        
        Optional<AllowedEmail> allowed = allowedEmailRepository.findByEmail(request.getEmail());
        if (allowed.isEmpty()) {
            log.error("Email {} is not in whitelist", request.getEmail());
            throw new RuntimeException("This email is not in whitelist. Registration is forbidden.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("User with email {} already exists", request.getEmail());
            throw new RuntimeException("User with this email already exists.");
        }

        String verificationToken = UUID.randomUUID().toString();
        log.info("Generated verification token for email: {}", request.getEmail());

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

        log.info("Verification token for {}: {}", request.getEmail(), verificationToken);
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
            System.err.println("Error sending verification email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Error sending verification email.", e);
        }
    }

    public void verifyEmail(VerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

        if (user.getVerificationToken().equals(request.getCode())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Invalid verification code");
        }
    }

    @Transactional
    public Map<String, String> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);

            if (user.getFailedLoginAttempts() >= 10) {
                String emailContent = String.format(
                    "Your account was blocked due to exceeding the number of login attempts.\n" +
                    "To unlock your account, you need to reset your password.\n" +
                    "Follow the link to reset your password: %s/reset-password",
                    frontendUrl
                );
                emailService.sendEmail(user.getEmail(), "Account blocked", emailContent);
            }

            throw new RuntimeException(SecurityConstants.INVALID_PASSWORD_ERROR);
        }

        if (user.isBlocked()) {
            throw new RuntimeException("Account is blocked. " + 
                (user.getBlockReason() != null ? user.getBlockReason() : ""));
        }

        if (!user.isEmailVerified()) {
            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            userRepository.save(user);

            String emailContent = String.format(
                "To verify your email, use the code: %s",
                verificationToken
            );
            emailService.sendEmail(user.getEmail(), "Email verification", emailContent);

            throw new RuntimeException("EMAIL_NOT_VERIFIED:" + user.getEmail());
        }

        user.resetFailedLoginAttempts();
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken
        );
    }

    public Map<String, String> refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid/expired refresh token");
        }
        String email = jwtTokenProvider.getEmailFromRefresh(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

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
            String emailContent = "We noticed you requested a password reset, but your account is linked with Google.\n" +
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
        String resetLink = ApiConstants.FRONTEND_RESET_PASSWORD_URL + token;
        String emailText = String.format(EmailConstants.RESET_PASSWORD_EMAIL_TEMPLATE, resetLink);
        message.setText(emailText);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending password reset email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Error sending password reset email.", e);
        }
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
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
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setEnabled(true);
                    newUser.setEmailVerified(true);
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    
                    Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Role ROLE_USER not found"));
                    newUser.setRoles(Set.of(userRole));
                    
                    return userRepository.save(newUser);
                });

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        if (user.isBlocked()) {
            throw new RuntimeException("Account is blocked. " + 
                (user.getBlockReason() != null ? user.getBlockReason() : ""));
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken
        );
    }
}