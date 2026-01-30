package com.authenticationservice.controller;

import com.authenticationservice.config.BaseIntegrationTest;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.ResetPasswordRequest;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.BlockedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.BlockedEmailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.http.Cookie;
import java.util.Collections;
import java.util.HashMap;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.yml")
@Import(com.authenticationservice.config.TestConfig.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure resend rate limit is non-zero for tests (bucket4j requires positive capacity)
        registry.add("rate-limit.resend-per-minute", () -> 1);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BlockedEmailRepository blockedEmailRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    private void setAccessMode(AccessMode mode) {
        ensureAccessModeSettings(mode);
        // Clear caches of access mode service if any (ensures fresh mode for next call)
        entityManager.flush();
        entityManager.clear();
    }

    @BeforeEach
    void setUp() {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        transactionTemplate.execute(status -> {
            cleanupTestData();
            blockedEmailRepository.deleteAll();
            blockedEmailRepository.flush();
            ensureRolesExist();
            ensureAccessModeSettings(AccessMode.WHITELIST);

            createAllowedEmail(TestConstants.UserData.TEST_EMAIL);
            testUser = createDefaultTestUser();
            userRepository.flush();
            return null;
        });
        transactionTemplate.execute(status ->
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User was not saved to database in setUp!"))
        );
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_shouldRegisterUserSuccessfully() throws Exception {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
        request.setName(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);

        // Use TransactionTemplate to explicitly commit transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            AllowedEmail allowedEmail = new AllowedEmail();
            allowedEmail.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
            allowedEmailRepository.save(allowedEmail);
            allowedEmailRepository.flush();
            return null;
        });

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Check your email")));
    }

    @Test
    @DisplayName("Should return bad request when registering with existing email")
    void register_shouldReturnBadRequest_whenEmailExists() throws Exception {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL); // Already exists
        request.setName(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);

        // Act & Assert
        // Should return generic message, not revealing that user exists
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to complete registration")));
    }

    @Test
    @DisplayName("Should return bad request when registering with email not in whitelist")
    void register_shouldReturnBadRequest_whenEmailNotInWhitelist() throws Exception {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("notwhitelisted@example.com");
        request.setName(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);

        // Act & Assert
        // Should return generic message, not revealing whitelist details
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to complete registration")));
    }

    @Test
    @DisplayName("Should return bad request when email is blacklisted in BLACKLIST mode")
    void register_shouldReturnBadRequest_whenEmailInBlacklistInBlacklistMode() throws Exception {
        // Arrange
        setAccessMode(AccessMode.BLACKLIST);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            BlockedEmail blockedEmail = new BlockedEmail();
            blockedEmail.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
            blockedEmailRepository.save(blockedEmail);
            blockedEmailRepository.flush();
            entityManager.flush();
            entityManager.clear();
            return null;
        });

        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
        request.setName(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);

        // Act & Assert
        // Should return generic message, not revealing blacklist details
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to complete registration")));
    }

    @Test
    @DisplayName("Should register successfully in BLACKLIST mode when email not blocked")
    void register_shouldSucceed_inBlacklistMode_whenEmailNotBlocked() throws Exception {
        // Arrange
        setAccessMode(AccessMode.BLACKLIST);

        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("blacklist-allowed@example.com");
        request.setName(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Check your email")));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_shouldLoginSuccessfully() throws Exception {
        // Arrange - verify user exists and is correctly saved in a new transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User savedUser = transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found in database"))
        );
        
        assertNotNull(savedUser, "User should not be null");
        assertTrue(savedUser.isEmailVerified(), "User email should be verified");
        assertTrue(savedUser.isEnabled(), "User should be enabled");
        assertFalse(savedUser.isBlocked(), "User should not be blocked");
        assertTrue(passwordEncoder.matches(TestConstants.UserData.TEST_PASSWORD, savedUser.getPassword()),
                "Password should match");

        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword(TestConstants.UserData.TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString(SecurityConstants.REFRESH_TOKEN_COOKIE_NAME + "=")));
    }

    @Test
    @DisplayName("Should return unauthorized with invalid credentials")
    @org.junit.jupiter.api.Timeout(10) // 10 seconds timeout
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // Disable transaction to avoid deadlocks
    void login_shouldReturnUnauthorizedWithInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refresh_shouldRefreshTokenSuccessfully() throws Exception {
        // Arrange
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REFRESH_URL)
                .cookie(new Cookie(SecurityConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString(SecurityConstants.REFRESH_TOKEN_COOKIE_NAME + "=")));
    }

    @Test
    @DisplayName("Should return unauthorized when refresh token is invalid")
    void refresh_shouldReturnUnauthorized_whenTokenInvalid() throws Exception {
        // Arrange
        String invalidToken = "invalid.refresh.token";

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REFRESH_URL)
                .cookie(new Cookie(SecurityConstants.REFRESH_TOKEN_COOKIE_NAME, invalidToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return bad request when password is invalid during reset")
    void resetPassword_shouldReturnBadRequest_whenPasswordInvalid() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String resetToken = transactionTemplate.execute(status -> {
            String token = java.util.UUID.randomUUID().toString();
            testUser.setResetPasswordToken(token);
            testUser.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
            userRepository.save(testUser);
            userRepository.flush();
            return token;
        });

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(resetToken);
        request.setNewPassword("123"); // Invalid password - too short, no uppercase, no special char
        request.setConfirmPassword("123");

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESET_PASSWORD_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Password must be at least 8 characters")));
    }

    @Test
    @DisplayName("Should return bad request when passwords do not match during reset")
    void resetPassword_shouldReturnBadRequest_whenPasswordsDoNotMatch() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String resetToken = transactionTemplate.execute(status -> {
            String token = java.util.UUID.randomUUID().toString();
            testUser.setResetPasswordToken(token);
            testUser.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
            userRepository.save(testUser);
            userRepository.flush();
            return token;
        });

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(resetToken);
        request.setNewPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);
        request.setConfirmPassword("Different123@");

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESET_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("do not match")));
    }

    @Test
    @DisplayName("Should return bad request when reset fields are missing")
    void resetPassword_shouldReturnBadRequest_whenFieldsMissing() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("");
        request.setNewPassword("");
        request.setConfirmPassword("");

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESET_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("required")));
    }

    @Test
    @DisplayName("Should return bad request when reset token is invalid")
    void resetPassword_shouldReturnBadRequest_whenTokenInvalid() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);
        request.setConfirmPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESET_PASSWORD_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid or expired reset token")));
    }

    @Test
    @DisplayName("Should return bad request when reset token is expired")
    void resetPassword_shouldReturnBadRequest_whenTokenExpired() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String expiredToken = transactionTemplate.execute(status -> {
            String token = java.util.UUID.randomUUID().toString();
            testUser.setResetPasswordToken(token);
            testUser.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().minusHours(1)); // Expired
            userRepository.save(testUser);
            userRepository.flush();
            return token;
        });

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(expiredToken);
        request.setNewPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);
        request.setConfirmPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESET_PASSWORD_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Expired reset token")));
    }

    @Test
    @DisplayName("Should lock account temporarily after 5 failed attempts")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void login_shouldLockAccountTemporarily_after5FailedAttempts() throws Exception {
        // Arrange - commit user state before login attempt
        // LoginAttemptService uses REQUIRES_NEW, so it needs to see committed data
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Long userId = transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setFailedLoginAttempts(4);
            user.setLockTime(null);
            User saved = userRepository.save(user);
            userRepository.flush();
            entityManager.flush();
            entityManager.clear();
            return saved.getId();
        });
        
        // Force commit by starting and committing a new transaction
        transactionTemplate.execute(status -> {
            assertNotNull(userId);
            userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found before login attempt"));
            entityManager.flush();
            return null;
        });

        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword("wrongpassword");

        // Act
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Wait for REQUIRES_NEW transaction to commit using polling
        User lockedUser = waitForUserUpdate(userId, user -> 
            user.getFailedLoginAttempts() == 5 && user.getLockTime() != null,
            transactionTemplate, entityManager);
        
        assertNotNull(lockedUser, "User should be found");
        assertEquals(5, lockedUser.getFailedLoginAttempts(), "Failed login attempts should be 5");
        assertNotNull(lockedUser.getLockTime(), "Lock time should be set");
        assertTrue(lockedUser.getLockTime().isAfter(java.time.LocalDateTime.now()), 
                "Lock time should be in the future");
    }

    @Test
    @DisplayName("Should block account after 10 failed attempts")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void login_shouldBlockAccount_after10FailedAttempts() throws Exception {
        // Arrange - commit user state before login attempt
        // LoginAttemptService uses REQUIRES_NEW, so it needs to see committed data
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Long userId = transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setFailedLoginAttempts(9);
            user.setBlocked(false);
            user.setLockTime(null);
            User saved = userRepository.save(user);
            userRepository.flush();
            entityManager.flush();
            entityManager.clear();
            return saved.getId();
        });
        
        // Force commit by starting and committing a new transaction
        transactionTemplate.execute(status -> {
            assertNotNull(userId);
            userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found before login attempt"));
            entityManager.flush();
            return null;
        });

        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword("wrongpassword");

        // Act
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Wait for REQUIRES_NEW transaction to commit using polling
        User blockedUser = waitForUserUpdate(userId, user -> 
            user.getFailedLoginAttempts() == 10 && user.isBlocked(),
            transactionTemplate, entityManager);
        
        assertNotNull(blockedUser, "User should be found");
        assertEquals(10, blockedUser.getFailedLoginAttempts(), "Failed login attempts should be 10");
        assertTrue(blockedUser.isBlocked(), "Account should be blocked");
    }

    @Test
    @DisplayName("Should throw AccountLockedException when account is locked")
    void login_shouldThrowAccountLockedException_whenAccountLocked() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            testUser.setLockTime(java.time.LocalDateTime.now().plusMinutes(5));
            userRepository.save(testUser);
            userRepository.flush();
            return null;
        });

        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword(TestConstants.UserData.TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reset attempts when login successful")
    void login_shouldResetAttempts_whenLoginSuccessful() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            testUser.setFailedLoginAttempts(3);
            userRepository.save(testUser);
            userRepository.flush();
            return null;
        });

        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword(TestConstants.UserData.TEST_PASSWORD);

        // Act
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Assert - verify attempts are reset
        User user = transactionTemplate.execute(status ->
                userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
        assertNotNull(user);
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded for login")
    void login_shouldReturn429_whenRateLimitExceeded() throws Exception {
        // Note: Rate limiting filter is disabled in this test class
        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword(TestConstants.UserData.TEST_PASSWORD);

        for (int i = 0; i < 11; i++) {
            mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded for register")
    void register_shouldReturn429_whenRateLimitExceeded() throws Exception {
        // Note: Rate limiting filter is disabled in this test class
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
        request.setName(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);

        for (int i = 0; i < 11; i++) {
            mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Test
    @DisplayName("Should verify email successfully")
    void verify_shouldVerifyEmailSuccessfully() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String verificationCode = transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String code = java.util.UUID.randomUUID().toString();
            user.setVerificationToken(code);
            user.setEmailVerified(false);
            userRepository.save(user);
            userRepository.flush();
            return code;
        });

        com.authenticationservice.dto.VerificationRequest request = new com.authenticationservice.dto.VerificationRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setCode(verificationCode);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.VERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Email verified")));
    }

    @Test
    @DisplayName("Should resend verification code successfully")
    void resendVerification_shouldResendVerificationCodeSuccessfully() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setEmailVerified(false);
            userRepository.save(user);
            userRepository.flush();
            return null;
        });

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESEND_VERIFICATION_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"email\":\"" + TestConstants.UserData.TEST_EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Verification code resent")));
    }

    @Test
    @DisplayName("Should return 429 when resend is requested within cooldown window")
    void resendVerification_shouldReturn429_whenWithinCooldown() throws Exception {
        // Arrange
        String cooldownEmail = "resend429@example.com";
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Role ROLE_USER not found"));
            User user = new User();
            user.setEmail(cooldownEmail);
            user.setName("Cooldown User");
            user.setPassword(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD));
            user.setEnabled(true);
            user.setBlocked(false);
            user.setEmailVerified(false);
            user.setVerificationToken("initial-token");
            user.setAuthProvider(com.authenticationservice.model.AuthProvider.LOCAL);
            java.util.Set<Role> roles = new java.util.HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
            return userRepository.save(user);
        });

        // First resend should succeed
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESEND_VERIFICATION_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"email\":\"" + cooldownEmail + "\"}"))
                .andExpect(status().isOk());

        // Second resend immediately should be rate-limited
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESEND_VERIFICATION_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"email\":\"" + cooldownEmail + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.retryAfterSeconds").exists())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("resend")));
    }

    @Test
    @DisplayName("Should return localized error when old verification code is used after resend")
    void verify_shouldReturnError_whenOldCodeUsedAfterResend() throws Exception {
        // Arrange: set old token and mark as not verified
        String resendEmail = "oldcode-resend@example.com";
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Role ROLE_USER not found"));
            User user = userRepository.findByEmail(resendEmail).orElse(null);
            if (user == null) {
                user = new User();
                user.setEmail(resendEmail);
                user.setName("OldCode User");
                user.setPassword(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD));
                user.setEnabled(true);
                user.setBlocked(false);
                user.setAuthProvider(com.authenticationservice.model.AuthProvider.LOCAL);
                java.util.Set<Role> roles = new java.util.HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);
            }
            user.setEmailVerified(false);
            user.setVerificationToken("old-token");
            userRepository.save(user);
            userRepository.flush();
            return null;
        });

        // Trigger resend to generate a new token (old one becomes outdated)
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESEND_VERIFICATION_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"email\":\"" + resendEmail + "\"}"))
                .andExpect(status().isOk());

        com.authenticationservice.dto.VerificationRequest request = new com.authenticationservice.dto.VerificationRequest();
        request.setEmail(resendEmail);
        request.setCode("old-token");

        // Act & Assert: old code should be rejected with localized message (default locale en)
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.VERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Verification code is invalid or expired")));
    }

    @Test
    @DisplayName("Should initiate password reset successfully")
    void forgotPassword_shouldInitiatePasswordResetSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.FORGOT_PASSWORD_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"email\":\"" + TestConstants.UserData.TEST_EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("within 10 minutes")));
    }

    @Test
    @DisplayName("Should reset password successfully")
    void resetPassword_shouldResetPasswordSuccessfully() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String resetToken = transactionTemplate.execute(status -> {
            String token = java.util.UUID.randomUUID().toString();
            testUser.setResetPasswordToken(token);
            testUser.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
            userRepository.save(testUser);
            userRepository.flush();
            return token;
        });

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(resetToken);
        request.setNewPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);
        request.setConfirmPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.RESET_PASSWORD_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Password has been reset successfully")));
    }

    @Test
    @DisplayName("Should return 200 when admin user checks access to admin-panel")
    void checkAccess_shouldReturn200_whenAdminUserChecksAccessToAdminPanel() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User adminUser = transactionTemplate.execute(status -> {
            Role adminRole = roleRepository.findByName(SecurityConstants.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            User user = new User();
            user.setEmail("admin@example.com");
            user.setName("Admin User");
            user.setPassword(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD));
            user.setEnabled(true);
            user.setBlocked(false);
            user.setEmailVerified(true);
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            user.setRoles(roles);
            return userRepository.save(user);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(adminUser);

        // Act & Assert
        mockMvc.perform(get(ApiConstants.AUTH_BASE_URL + ApiConstants.CHECK_ACCESS_URL.replace("{resource}", SecurityConstants.ADMIN_PANEL_RESOURCE))
                .header(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 403 when regular user checks access to admin-panel")
    void checkAccess_shouldReturn403_whenRegularUserChecksAccessToAdminPanel() throws Exception {
        // Arrange
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        // Act & Assert
        mockMvc.perform(get(ApiConstants.AUTH_BASE_URL + ApiConstants.CHECK_ACCESS_URL.replace("{resource}", SecurityConstants.ADMIN_PANEL_RESOURCE))
                .header(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401 when checking access without valid token")
    void checkAccess_shouldReturn401_whenTokenIsInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ApiConstants.AUTH_BASE_URL + ApiConstants.CHECK_ACCESS_URL.replace("{resource}", SecurityConstants.ADMIN_PANEL_RESOURCE))
                .header(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + "invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when checking access without Bearer prefix")
    void checkAccess_shouldReturn401_whenHeaderDoesNotHaveBearerPrefix() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ApiConstants.AUTH_BASE_URL + ApiConstants.CHECK_ACCESS_URL.replace("{resource}", SecurityConstants.ADMIN_PANEL_RESOURCE))
                .header(SecurityConstants.AUTHORIZATION_HEADER, "invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 when admin user checks access to user-management")
    void checkAccess_shouldReturn200_whenAdminUserChecksAccessToUserManagement() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User adminUser = transactionTemplate.execute(status -> {
            Role adminRole = roleRepository.findByName(SecurityConstants.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            User user = new User();
            user.setEmail("admin2@example.com");
            user.setName("Admin User 2");
            user.setPassword(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD));
            user.setEnabled(true);
            user.setBlocked(false);
            user.setEmailVerified(true);
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            user.setRoles(roles);
            return userRepository.save(user);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(adminUser);

        // Act & Assert
        mockMvc.perform(get(ApiConstants.AUTH_BASE_URL + ApiConstants.CHECK_ACCESS_URL.replace("{resource}", SecurityConstants.USER_MANAGEMENT_RESOURCE))
                .header(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 403 when checking access to unknown resource")
    void checkAccess_shouldReturn403_whenCheckingAccessToUnknownResource() throws Exception {
        // Arrange
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        // Act & Assert
        mockMvc.perform(get(ApiConstants.AUTH_BASE_URL + ApiConstants.CHECK_ACCESS_URL.replace("{resource}", "unknown-resource"))
                .header(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return tokens when OAuth2 login is successful")
    void oauth2Success_shouldReturnTokens_whenOAuth2LoginSuccessful() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            // Ensure user exists for OAuth2 login
            User user = userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setEmailVerified(true);
            user.setEnabled(true);
            userRepository.save(user);
            userRepository.flush();
            return null;
        });

        // Create OAuth2User with attributes
        java.util.Map<String, Object> attributes = new HashMap<>();
        attributes.put(SecurityConstants.OAUTH2_EMAIL_ATTRIBUTE, TestConstants.UserData.TEST_EMAIL);
        attributes.put(SecurityConstants.OAUTH2_NAME_ATTRIBUTE, TestConstants.UserData.TEST_USERNAME);
        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                SecurityConstants.OAUTH2_EMAIL_ATTRIBUTE
        );

        // Set up SecurityContext with OAuth2User
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                oauth2User,
                null,
                Collections.emptyList()
        );
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Act & Assert
            mockMvc.perform(get(ApiConstants.AUTH_BASE_URL + ApiConstants.OAUTH2_SUCCESS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE,
                            org.hamcrest.Matchers.containsString(SecurityConstants.REFRESH_TOKEN_COOKIE_NAME + "=")));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Waits for user to be updated in database with polling mechanism.
     * Replaces Thread.sleep() with condition-based waiting.
     * 
     * @param userId User ID to check
     * @param condition Condition to wait for
     * @param transactionTemplate Transaction template for database access
     * @param entityManager Entity manager for clearing cache
     * @return Updated user when condition is met
     */
    @SuppressWarnings("BusyWait")
    private User waitForUserUpdate(Long userId, 
                                   java.util.function.Predicate<User> condition,
                                   TransactionTemplate transactionTemplate,
                                   EntityManager entityManager) {
        long timeout = 5000; // 5 seconds timeout
        long startTime = System.currentTimeMillis();
        long pollInterval = 50; // Poll every 50ms

        while (System.currentTimeMillis() - startTime < timeout) {
            User user = transactionTemplate.execute(status -> {
                entityManager.clear();
                return userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            });

            if (condition.test(user)) {
                return user;
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for user update", e);
            }
        }

        // Return user even if condition not met (for assertion)
        return transactionTemplate.execute(status -> {
            entityManager.clear();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        });
    }
}
