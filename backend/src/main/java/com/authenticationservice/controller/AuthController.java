package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.ResetPasswordRequest;
import com.authenticationservice.dto.VerificationRequest;
import com.authenticationservice.service.AuthService;
import com.authenticationservice.security.JwtTokenProvider;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.AUTH_BASE_URL)
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping(ApiConstants.REGISTER_URL)
    public ResponseEntity<String> register(@RequestBody RegistrationRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.ok(MessageConstants.REGISTRATION_SUCCESS);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(ApiConstants.VERIFY_URL)
    public ResponseEntity<String> verify(@RequestBody VerificationRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(MessageConstants.EMAIL_VERIFIED_SUCCESS);
    }

    @PostMapping(ApiConstants.LOGIN_URL)
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Map<String, String> tokens = authService.login(req);
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : MessageConstants.UNKNOWN_ERROR + e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MessageConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(ApiConstants.REFRESH_URL)
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get(SecurityConstants.REFRESH_TOKEN_KEY);
        Map<String, String> tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping(ApiConstants.RESEND_VERIFICATION_URL)
    public ResponseEntity<String> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get(SecurityConstants.EMAIL_KEY);
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(MessageConstants.EMAIL_REQUIRED);
        }
        try {
            authService.resendVerification(email);
            return ResponseEntity.ok(MessageConstants.VERIFICATION_RESENT_SUCCESS);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(ApiConstants.FORGOT_PASSWORD_URL)
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get(SecurityConstants.EMAIL_KEY);
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(MessageConstants.EMAIL_REQUIRED);
        }
        try {
            authService.initiatePasswordReset(email);
            return ResponseEntity.ok(MessageConstants.PASSWORD_RESET_INITIATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(ApiConstants.RESET_PASSWORD_URL)
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank() ||
                request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            return ResponseEntity.badRequest().body(MessageConstants.RESET_PASSWORD_FIELDS_REQUIRED);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(MessageConstants.PASSWORDS_DO_NOT_MATCH);
        }

        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(MessageConstants.PASSWORD_RESET_SUCCESS);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(ApiConstants.CHECK_ACCESS_URL)
    public ResponseEntity<?> checkAccess(@PathVariable String resource, @RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String authHeader) {
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX_LENGTH);
        List<String> roles = jwtTokenProvider.getRolesFromAccess(token);

        // Check access based on roles and resource
        boolean hasAccess = switch (resource) {
            case SecurityConstants.ADMIN_PANEL_RESOURCE -> roles.contains(SecurityConstants.ROLE_ADMIN);
            case SecurityConstants.USER_MANAGEMENT_RESOURCE -> roles.contains(SecurityConstants.ROLE_ADMIN);
            default -> false;
        };

        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping(ApiConstants.OAUTH2_SUCCESS_URL)
    public ResponseEntity<Map<String, String>> oauth2Success(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute(SecurityConstants.OAUTH2_EMAIL_ATTRIBUTE);
        String name = oauth2User.getAttribute(SecurityConstants.OAUTH2_NAME_ATTRIBUTE);

        try {
            Map<String, String> tokens = authService.handleOAuth2Login(email, name);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(SecurityConstants.ERROR_KEY, e.getMessage()));
        }
    }
}