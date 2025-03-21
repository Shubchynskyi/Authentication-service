package com.authenticationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.EmailConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.VerificationRequest;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AllowedEmailRepository allowedEmailRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void register(RegistrationRequest request) {
        Optional<AllowedEmail> allowed = allowedEmailRepository.findByEmail(request.getEmail());
        if (allowed.isEmpty()) {
            throw new RuntimeException("Данный email не находится в белом списке. Регистрация запрещена.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email уже существует.");
        }

        String verificationCode = UUID.randomUUID().toString();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setVerificationCode(verificationCode);

        Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Роль ROLE_USER не найдена в базе."));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        System.out.println("Код подтверждения для " + request.getEmail() + ": " + verificationCode);
        sendVerificationEmail(request.getEmail(), verificationCode);
    }

    private void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(EmailConstants.VERIFICATION_SUBJECT);
        String emailText = String.format(EmailConstants.VERIFICATION_EMAIL_TEMPLATE, code);
        message.setText(emailText);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Ошибка при отправке email подтверждения на " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Ошибка отправки email подтверждения.", e);
        }
    }

    public void verifyEmail(VerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));

        if (user.getVerificationCode().equals(request.getCode())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Неверный код подтверждения");
        }
    }

    public Map<String, String> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(SecurityConstants.USER_NOT_FOUND_ERROR));
        if (!user.isEmailVerified()) {
            throw new RuntimeException(SecurityConstants.EMAIL_VERIFIED_ERROR);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException(SecurityConstants.INVALID_PASSWORD_ERROR);
        }
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        return result;
    }

    public Map<String, String> refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Невалидный/просроченный refresh‑токен");
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

        String verificationCode = UUID.randomUUID().toString();
        user.setVerificationCode(verificationCode);
        userRepository.save(user);

        sendVerificationEmail(email, verificationCode);
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(new Date(System.currentTimeMillis() + SecurityConstants.ONE_HOUR_IN_MS));
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
            System.err.println("Ошибка при отправке email сброса пароля на " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Ошибка отправки email сброса пароля.", e);
        }
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));

        if (user.getResetPasswordTokenExpiry().before(new Date())) {
            throw new RuntimeException("Expired reset token.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }
}