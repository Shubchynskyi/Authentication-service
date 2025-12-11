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
import jakarta.validation.Valid;

import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.dto.EmailRequest;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RefreshTokenRequest;
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
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        // Exception handling is done via GlobalExceptionHandler
        authService.register(request);
        return ResponseEntity.ok(MessageConstants.REGISTRATION_SUCCESS);
    }

    @PostMapping(ApiConstants.LOGIN_URL)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        // Exception handling is done via GlobalExceptionHandler
        Map<String, String> tokens = authService.login(req);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping(ApiConstants.REFRESH_URL)
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        Map<String, String> tokens = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping(ApiConstants.VERIFY_URL)
    public ResponseEntity<String> verify(@Valid @RequestBody VerificationRequest request) {
        // Exception handling is done via GlobalExceptionHandler
        authService.verifyEmail(request);
        return ResponseEntity.ok(MessageConstants.EMAIL_VERIFIED_SUCCESS);
    }

    @PostMapping(ApiConstants.RESEND_VERIFICATION_URL)
    public ResponseEntity<String> resendVerification(@Valid @RequestBody EmailRequest request) {
        // Exception handling is done via GlobalExceptionHandler
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok(MessageConstants.VERIFICATION_RESENT_SUCCESS);
    }

    @PostMapping(ApiConstants.FORGOT_PASSWORD_URL)
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody EmailRequest request) {
        // Exception handling is done via GlobalExceptionHandler
        authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(String.format(
                MessageConstants.PASSWORD_RESET_INITIATED,
                authService.getPasswordResetCooldownMinutes()));
    }

    @PostMapping(ApiConstants.RESET_PASSWORD_URL)
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // Exception handling is done via GlobalExceptionHandler
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(MessageConstants.PASSWORD_RESET_SUCCESS);
    }

    @GetMapping(ApiConstants.CHECK_ACCESS_URL)
    public ResponseEntity<?> checkAccess(@PathVariable String resource,
            @RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String authHeader) {
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX_LENGTH);
        
        // Security: Validate token before extracting claims
        if (!jwtTokenProvider.validateAccessToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
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

        // Exception handling is done via GlobalExceptionHandler
        Map<String, String> tokens = authService.handleOAuth2Login(email, name);
        return ResponseEntity.ok(tokens);
    }
}