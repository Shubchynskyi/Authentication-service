package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;

import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.ResetPasswordRequest;
import com.authenticationservice.dto.VerificationRequest;
import com.authenticationservice.service.AuthService;
import com.authenticationservice.security.JwtTokenProvider;

import java.util.Map;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.AUTH_BASE_URL)
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping(ApiConstants.REGISTER_URL)
    public ResponseEntity<String> register(@RequestBody RegistrationRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        try {
            authService.register(request);
            log.info("Successfully registered user with email: {}", request.getEmail());
            return ResponseEntity.ok("Check your email (verification code is in server console)!");
        } catch (Exception e) {
            log.error("Registration error for email: {}, error: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(ApiConstants.VERIFY_URL)
    public ResponseEntity<String> verify(@RequestBody VerificationRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok("Email verified. Now you can login.");
    }

    @PostMapping(ApiConstants.LOGIN_URL)
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest req) {
        Map<String, String> tokens = authService.login(req);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping(ApiConstants.REFRESH_URL)
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        Map<String, String> tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping(ApiConstants.RESEND_VERIFICATION_URL)
    public ResponseEntity<String> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        try {
            authService.resendVerification(email);
            return ResponseEntity.ok("Verification code resent. Check your email (server console output).");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(ApiConstants.FORGOT_PASSWORD_URL)
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        try {
            authService.initiatePasswordReset(email);
            return ResponseEntity.ok("If an account with that email exists, a password reset link has been sent.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(ApiConstants.RESET_PASSWORD_URL)
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank() ||
                request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Token, new password, and confirm password are required.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Passwords do not match.");
        }

        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("Password has been reset successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/check-access/{resource}")
    public ResponseEntity<?> checkAccess(@PathVariable String resource, @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        List<String> roles = jwtTokenProvider.getRolesFromAccess(token);

        // Проверяем доступ на основе ролей и ресурса
        boolean hasAccess = switch (resource) {
            case "admin-panel" -> roles.contains("ROLE_ADMIN");
            case "user-management" -> roles.contains("ROLE_ADMIN");
            default -> false;
        };

        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }
}