package com.authenticationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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

    // Можно использовать Bean PasswordEncoder из SecurityConfig
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void register(RegistrationRequest request) {
        // Проверка в БД: есть ли email в таблице allowed_emails
        Optional<AllowedEmail> allowed = allowedEmailRepository.findByEmail(request.getEmail());
        if (allowed.isEmpty()) {
            throw new RuntimeException("Данный email не находится в белом списке. Регистрация запрещена.");
        }

        // Проверка: нет ли уже такого пользователя
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email уже существует.");
        }

        // Создаём код подтверждения
        String verificationCode = UUID.randomUUID().toString();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setVerificationCode(verificationCode);

        // По умолчанию присваиваем роль USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Роль ROLE_USER не найдена в базе."));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        // Имитация отправки email: выводим код в лог
        System.out.println("Код подтверждения для " + request.getEmail() + ": " + verificationCode);
        sendVerificationEmail(request.getEmail(), verificationCode);
    }

    private void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Подтверждение Email для BNB Project"); // Более информативная тема
        String emailText = String.format(
                "Здравствуйте!\n\n" +
                        "Для завершения регистрации на BNB Project, пожалуйста, используйте следующий код подтверждения:\n\n"
                        +
                        "Код подтверждения: %s\n\n" +
                        "Этот код действителен в течение 15 минут.\n\n" + // Указание срока действия!
                        "Пожалуйста, введите этот код на странице подтверждения регистрации.\n\n" +
                        "С уважением,\nКоманда BNB Project",
                code); // Подпись
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
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getVerificationCode().equals(request.getCode())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Неверный код подтверждения");
        }
    }

    public Map<String, String> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email не подтвержден");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Обычно refreshToken сохраняется в базе,
        // чтобы можно было его инвалидировать при логауте,
        // но это уже дополнительная логика.

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        return result;
    }

    public Map<String, String> refresh(String refreshToken) {
        // Проверяем refresh‑токен (не просрочен ли, подпись и т.д.)
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Невалидный/просроченный refresh‑токен");
        }
        // Извлекаем email
        String email = jwtTokenProvider.getEmailFromRefresh(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Генерируем новый access‑токен
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);

        // Можно также выдать новый refresh, если хотите «скользящее» продление
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        result.put("refreshToken", newRefreshToken);

        return result;
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        // Check if the user is *already* verified
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified.");
        }

        // Generate a *new* verification code
        String verificationCode = UUID.randomUUID().toString();
        user.setVerificationCode(verificationCode); // Update the code in the database
        userRepository.save(user);

        // Send the verification email (using the existing method)
        sendVerificationEmail(email, verificationCode);
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null); // Don't throw an exception here

        if (user == null) {
            // Don't reveal whether the email exists or not. Return success either way.
            return;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        // Set an expiration time (e.g., 1 hour from now)
        user.setResetPasswordTokenExpiry(new Date(System.currentTimeMillis() + 3600000)); // 1 hour
        userRepository.save(user);

        sendPasswordResetEmail(email, resetToken);
    }

    private void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        String resetLink = "http://localhost:5173/reset-password?token=" + token; // FRONTEND URL!
        String emailText = String.format(
                "You have requested a password reset.  Please click the link below to reset your password:\n\n%s\n\n" +
                        "If you did not request a password reset, please ignore this email.",
                resetLink);
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
        user.setResetPasswordToken(null); // Clear the token
        user.setResetPasswordTokenExpiry(null); // Clear the expiry
        userRepository.save(user);
    }
}