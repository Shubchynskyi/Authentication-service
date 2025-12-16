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
import com.authenticationservice.util.LoggingSanitizer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.AUTH_BASE_URL)
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }

    @PostMapping(ApiConstants.REGISTER_URL)
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        log.debug("Registration request received for email: {}", maskEmail(request.getEmail()));
        authService.register(request);
        log.info("Registration completed successfully for email: {}", maskEmail(request.getEmail()));
        return ResponseEntity.ok(MessageConstants.REGISTRATION_SUCCESS);
    }

    @PostMapping(ApiConstants.LOGIN_URL)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        log.debug("Login request received for email: {}", maskEmail(req.getEmail()));
        Map<String, String> tokens = authService.login(req);
        log.info("Login successful for email: {}", maskEmail(req.getEmail()));
        return ResponseEntity.ok(tokens);
    }

    @PostMapping(ApiConstants.REFRESH_URL)
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request received");
        Map<String, String> tokens = authService.refresh(request.getRefreshToken());
        log.info("Token refresh successful");
        return ResponseEntity.ok(tokens);
    }

    @PostMapping(ApiConstants.VERIFY_URL)
    public ResponseEntity<String> verify(@Valid @RequestBody VerificationRequest request) {
        log.debug("Email verification request received for email: {}", maskEmail(request.getEmail()));
        authService.verifyEmail(request);
        log.info("Email verified successfully for email: {}", maskEmail(request.getEmail()));
        return ResponseEntity.ok(MessageConstants.EMAIL_VERIFIED_SUCCESS);
    }

    @PostMapping(ApiConstants.RESEND_VERIFICATION_URL)
    public ResponseEntity<String> resendVerification(@Valid @RequestBody EmailRequest request) {
        log.debug("Resend verification request received for email: {}", maskEmail(request.getEmail()));
        authService.resendVerification(request.getEmail());
        log.info("Verification email resent successfully for email: {}", maskEmail(request.getEmail()));
        return ResponseEntity.ok(MessageConstants.VERIFICATION_RESENT_SUCCESS);
    }

    @PostMapping(ApiConstants.FORGOT_PASSWORD_URL)
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody EmailRequest request) {
        log.debug("Password reset initiation request received for email: {}", maskEmail(request.getEmail()));
        authService.initiatePasswordReset(request.getEmail());
        log.info("Password reset initiated successfully for email: {}", maskEmail(request.getEmail()));
        return ResponseEntity.ok(String.format(
                MessageConstants.PASSWORD_RESET_INITIATED,
                authService.getPasswordResetCooldownMinutes()));
    }

    @PostMapping(ApiConstants.RESET_PASSWORD_URL)
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.debug("Password reset request received");
        authService.resetPassword(request.getToken(), request.getNewPassword());
        log.info("Password reset completed successfully");
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

        log.debug("OAuth2 login request received for email: {}", maskEmail(email));
        Map<String, String> tokens = authService.handleOAuth2Login(email, name);
        log.info("OAuth2 login successful for email: {}", maskEmail(email));
        return ResponseEntity.ok(tokens);
    }
}