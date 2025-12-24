package com.authenticationservice.service;

import com.authenticationservice.constants.EmailConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.dto.VerificationRequest;
import com.authenticationservice.exception.InvalidCredentialsException;
import com.authenticationservice.exception.TooManyRequestsException;
import com.authenticationservice.model.AuthProvider;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import com.authenticationservice.service.AccessControlService;
import com.authenticationservice.service.EmailService;
import com.authenticationservice.service.LoginAttemptService;
import com.authenticationservice.service.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private AllowedEmailRepository allowedEmailRepository;

        @Mock
        private AccessControlService accessControlService;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private JwtTokenProvider jwtTokenProvider;

        @Mock
        private BCryptPasswordEncoder passwordEncoder;

        @Mock
        private LoginAttemptService loginAttemptService;

        @Mock
        private EmailService emailService;

        @Mock
        private RateLimitingService rateLimitingService;

        @Mock
        private MessageSource messageSource;

        @Mock
        private com.authenticationservice.util.EmailTemplateFactory emailTemplateFactory;

        @InjectMocks
        private AuthService authService;

        private User testUser;
        private LoginRequest loginRequest;
        private Bucket resendBucket;

        @BeforeEach
        void setUp() {
                // Create a fresh test user for each test
                testUser = createTestUser();

                // Create a login request
                loginRequest = createLoginRequest(
                                TestConstants.UserData.TEST_EMAIL,
                                TestConstants.UserData.TEST_PASSWORD);

                // Setup service configuration
                ReflectionTestUtils.setField(authService, "frontendUrl", TestConstants.Urls.FRONTEND_URL);
                ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
                ReflectionTestUtils.setField(authService, "passwordResetCooldownMinutes", 10);
                ReflectionTestUtils.setField(authService, "messageSource", messageSource);

                resendBucket = mock(Bucket.class);
                ConsumptionProbe successProbe = mock(ConsumptionProbe.class);
                lenient().when(successProbe.isConsumed()).thenReturn(true);
                lenient().when(resendBucket.tryConsumeAndReturnRemaining(anyLong())).thenReturn(successProbe);
                lenient().when(rateLimitingService.resolveResendBucket(anyString())).thenReturn(resendBucket);

                lenient().when(messageSource.getMessage(
                                eq(com.authenticationservice.constants.MessageConstants.VERIFICATION_CODE_INVALID_OR_EXPIRED),
                                any(),
                                any()))
                                .thenReturn(TestConstants.ErrorMessages.INVALID_VERIFICATION_CODE);

                // Mock EmailTemplateFactory methods
                lenient().when(emailTemplateFactory.buildVerificationText(anyString())).thenReturn("Verification text");
                lenient().when(emailTemplateFactory.buildVerificationHtml(anyString())).thenReturn("Verification html");
                lenient().when(emailTemplateFactory.buildResetPasswordText(anyString())).thenReturn("Reset password text");
                lenient().when(emailTemplateFactory.buildResetPasswordHtml(anyString())).thenReturn("Reset password html");
                lenient().when(emailTemplateFactory.buildGoogleResetText()).thenReturn("Google reset text");
                lenient().when(emailTemplateFactory.buildGoogleResetHtml()).thenReturn("Google reset html");
        }

        /**
         * Creates a test user with default values
         * 
         * @return User with default test data
         */
        private User createTestUser() {
                User user = new User();
                user.setId(1L);
                user.setName(TestConstants.UserData.TEST_USERNAME);
                user.setEmail(TestConstants.UserData.TEST_EMAIL);
                user.setPassword(TestConstants.UserData.ENCODED_PASSWORD);
                user.setEnabled(true);
                user.setBlocked(false);
                user.setEmailVerified(true);
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);

                Role userRole = new Role();
                userRole.setName(SecurityConstants.ROLE_USER);
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);

                return user;
        }

        /**
         * Creates a login request with specified credentials
         * 
         * @param email    User email
         * @param password User password
         * @return Configured login request
         */
        private LoginRequest createLoginRequest(String email, String password) {
                LoginRequest request = new LoginRequest();
                request.setEmail(email);
                request.setPassword(password);
                return request;
        }

        @Nested
        @DisplayName("Registration Tests")
        class RegistrationTests {
                @Test
                @DisplayName("Should succeed when all data is valid")
                void register_shouldSucceed_whenAllDataValid() {
                        // Arrange
                        RegistrationRequest request = createRegistrationRequest();
                        Role userRole = createUserRole();

                        doNothing().when(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.empty());
                        when(roleRepository.findByName(SecurityConstants.ROLE_USER))
                                        .thenReturn(Optional.of(userRole));
                        when(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD))
                                        .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.register(request));
                        verify(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                        verify(userRepository).save(any(User.class));
                        verify(emailService).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.VERIFICATION_SUBJECT),
                                anyString(),
                                anyString());
                }

                @Test
                @DisplayName("Should normalize email before registration checks")
                void register_shouldNormalizeEmail_beforeChecks() {
                        // Arrange
                        RegistrationRequest request = createRegistrationRequest();
                        String mixedCaseEmail = "User@Example.COM";
                        String normalizedEmail = mixedCaseEmail.toLowerCase();
                        request.setEmail(mixedCaseEmail);
                        Role userRole = createUserRole();

                        doNothing().when(accessControlService).checkRegistrationAccess(normalizedEmail);
                        when(userRepository.findByEmail(normalizedEmail))
                                        .thenReturn(Optional.empty());
                        when(roleRepository.findByName(SecurityConstants.ROLE_USER))
                                        .thenReturn(Optional.of(userRole));
                        when(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD))
                                        .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
                        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
                        when(userRepository.save(userCaptor.capture()))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // Act
                        authService.register(request);

                        // Assert
                        assertEquals(normalizedEmail, request.getEmail());
                        User savedUser = userCaptor.getValue();
                        assertEquals(normalizedEmail, savedUser.getEmail());
                        verify(accessControlService).checkRegistrationAccess(normalizedEmail);
                }

                @Test
                @DisplayName("Should throw exception when email is not allowed")
                void register_shouldThrowException_whenEmailNotAllowed() {
                        // Arrange
                        RegistrationRequest request = createRegistrationRequest();
                        doThrow(new com.authenticationservice.exception.RegistrationForbiddenException())
                                        .when(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);

                        // Act & Assert
                        com.authenticationservice.exception.RegistrationForbiddenException ex = assertThrows(
                                        com.authenticationservice.exception.RegistrationForbiddenException.class,
                                        () -> authService.register(request));
                        assertEquals("registration.forbidden", ex.getMessage());
                        verify(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                }

                @Test
                @DisplayName("Should throw exception when user already exists")
                void register_shouldThrowException_whenUserAlreadyExists() {
                        // Arrange
                        RegistrationRequest request = createRegistrationRequest();
                        doNothing().when(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        com.authenticationservice.exception.RegistrationForbiddenException ex = assertThrows(
                                        com.authenticationservice.exception.RegistrationForbiddenException.class,
                                        () -> authService.register(request));
                        assertEquals("registration.forbidden", ex.getMessage());
                        verify(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                }

                @Test
                @DisplayName("Should throw exception when role is not found")
                void register_shouldThrowException_whenRoleNotFound() {
                        // Arrange
                        RegistrationRequest request = createRegistrationRequest();
                        doNothing().when(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.empty());
                        when(roleRepository.findByName(SecurityConstants.ROLE_USER))
                                        .thenReturn(Optional.empty());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.register(request));
                        assertEquals("Role ROLE_USER not found in database.", ex.getMessage());
                        verify(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                }

                @Test
                @DisplayName("Should throw exception when email sending fails")
                void register_shouldThrowException_whenEmailSendingFails() {
                        // Arrange
                        RegistrationRequest request = createRegistrationRequest();
                        Role userRole = createUserRole();

                        doNothing().when(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.empty());
                        when(roleRepository.findByName(SecurityConstants.ROLE_USER))
                                        .thenReturn(Optional.of(userRole));
                        when(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD))
                                        .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        doThrow(new RuntimeException("Mail server error"))
                                        .when(emailService).sendEmail(anyString(), anyString(), anyString(), anyString());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.register(request));
                        assertNotNull(ex.getMessage(), "Should propagate email send failure");
                        verify(accessControlService).checkRegistrationAccess(TestConstants.UserData.TEST_EMAIL);
                }
        }

        @Nested
        @DisplayName("Email Verification Tests")
        class EmailVerificationTests {
                @Test
                @DisplayName("Should succeed when verification code is valid")
                void verifyEmail_shouldSucceed_whenCodeValid() {
                        // Arrange
                        VerificationRequest request = createVerificationRequest();
                        testUser.setVerificationToken(request.getCode());
                        testUser.setEmailVerified(false);

                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.verifyEmail(request));
                        assertTrue(testUser.isEmailVerified());
                        verify(userRepository).save(testUser);
                }

                @Test
                @DisplayName("Should throw exception when verification code is invalid")
                void verifyEmail_shouldThrowException_whenCodeInvalid() {
                        // Arrange
                        VerificationRequest request = createVerificationRequest();
                        testUser.setVerificationToken("correctCode");
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.verifyEmail(request));
                        assertEquals(TestConstants.ErrorMessages.INVALID_VERIFICATION_CODE, ex.getMessage());
                }

                @Test
                @DisplayName("Should succeed when resending verification for unverified user")
                void resendVerification_shouldSucceed_whenUserNotVerified() {
                        // Arrange
                        testUser.setEmailVerified(false);
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.resendVerification(testUser.getEmail()));
                        verify(userRepository).save(testUser);
                        verify(emailService).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.VERIFICATION_SUBJECT),
                                anyString(),
                                anyString());
                }

                @Test
                @DisplayName("Should throw TooManyRequestsException when resend is rate limited")
                void resendVerification_shouldThrowTooManyRequests_whenCooldownActive() {
                        // Arrange
                        testUser.setEmailVerified(false);
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        ConsumptionProbe blockedProbe = mock(ConsumptionProbe.class);
                        when(blockedProbe.isConsumed()).thenReturn(false);
                        when(blockedProbe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L);
                        when(resendBucket.tryConsumeAndReturnRemaining(anyLong())).thenReturn(blockedProbe);

                        // Act & Assert
                        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                                        () -> authService.resendVerification(testUser.getEmail()));

                        assertEquals(MessageConstants.RESEND_RATE_LIMIT_EXCEEDED, ex.getMessage());
                        assertEquals(30L, ex.getRetryAfterSeconds());
                }

                @Test
                @DisplayName("Should throw exception when email is already verified")
                void resendVerification_shouldThrowException_whenEmailAlreadyVerified() {
                        // Arrange
                        testUser.setEmailVerified(true);
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.resendVerification(testUser.getEmail()));
                        assertEquals(TestConstants.ErrorMessages.EMAIL_ALREADY_VERIFIED, ex.getMessage());
                }

                @Test
                @DisplayName("Should throw exception when user is not found")
                void resendVerification_shouldThrowException_whenUserNotFound() {
                        // Arrange
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.empty());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.resendVerification(TestConstants.UserData.TEST_EMAIL));
                        assertEquals("User not found.", ex.getMessage());
                }
        }

        @Nested
        @DisplayName("Authentication Tests")
        class AuthenticationTests {
                @Test
                @DisplayName("Should normalize email before login lookup")
                void login_shouldNormalizeEmail_beforeLookup() {
                        // Arrange
                        String mixedCaseEmail = "Test@Example.COM";
                        loginRequest.setEmail(mixedCaseEmail);
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(jwtTokenProvider.generateAccessToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.ACCESS_TOKEN);
                        when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.REFRESH_TOKEN);
                        when(userRepository.save(any(User.class)))
                                        .thenReturn(testUser);
                        doNothing().when(accessControlService).checkLoginAccess(TestConstants.UserData.TEST_EMAIL);

                        // Act
                        Map<String, String> tokens = authService.login(loginRequest);

                        // Assert
                        assertEquals(TestConstants.UserData.TEST_EMAIL, loginRequest.getEmail());
                        assertNotNull(tokens);
                        verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
                        verify(accessControlService).checkLoginAccess(TestConstants.UserData.TEST_EMAIL);
                }

                @Test
                @DisplayName("Should return tokens when credentials are valid")
                void login_shouldReturnTokens_whenCredentialsValid() {
                        // Arrange
                        when(userRepository.findByEmail(loginRequest.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(jwtTokenProvider.generateAccessToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.ACCESS_TOKEN);
                        when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.REFRESH_TOKEN);
                        when(userRepository.save(any(User.class)))
                                        .thenReturn(testUser);

                        // Act
                        Map<String, String> tokens = authService.login(loginRequest);

                        // Assert
                        assertNotNull(tokens, "Returned tokens map should not be null");
                        assertEquals(TestConstants.Tokens.ACCESS_TOKEN, tokens.get("accessToken"),
                                        "Access token should match expected value");
                        assertEquals(TestConstants.Tokens.REFRESH_TOKEN, tokens.get("refreshToken"),
                                        "Refresh token should match expected value");
                        verify(userRepository).findByEmail(loginRequest.getEmail());
                        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());

                        // Verify failed attempts are reset on successful login
                        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
                        verify(userRepository).save(userCaptor.capture());
                        assertEquals(0, userCaptor.getValue().getFailedLoginAttempts(),
                                        "Failed login attempts should be reset to 0 on successful login");
                }

                @Test
                @DisplayName("Should throw exception when password is invalid")
                void login_shouldThrowException_whenPasswordInvalid() {
                        // Arrange
                        when(userRepository.findByEmail(loginRequest.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                                        .thenReturn(false);
                        doNothing().when(loginAttemptService).handleFailedLogin(any(User.class), anyString());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.login(loginRequest),
                                        "Should throw RuntimeException when password is invalid");
                        assertEquals(SecurityConstants.INVALID_CREDENTIALS_ERROR, ex.getMessage(),
                                        "Exception message should indicate invalid password");
                        verify(userRepository).findByEmail(loginRequest.getEmail());
                        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());

                        // Verify that LoginAttemptService was called to handle failed login
                        verify(loginAttemptService).handleFailedLogin(any(User.class), anyString());
                }

                @Test
                @DisplayName("Should handle multiple failed login attempts")
                void login_shouldHandleMultipleFailedAttempts() {
                        // Arrange - Set up a user with multiple failed attempts
                        testUser.setFailedLoginAttempts(5); // Assuming a high number of failed attempts

                        when(userRepository.findByEmail(loginRequest.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                                        .thenReturn(false);
                        doNothing().when(loginAttemptService).handleFailedLogin(any(User.class), anyString());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.login(loginRequest),
                                        "Should throw RuntimeException when password is invalid");
                        assertEquals(SecurityConstants.INVALID_CREDENTIALS_ERROR, ex.getMessage(),
                                        "Exception message should indicate invalid password");

                        // Verify that LoginAttemptService was called to handle failed login
                        verify(loginAttemptService).handleFailedLogin(any(User.class), anyString());
                }

                @Test
                @DisplayName("Should throw exception when user is disabled")
                void login_shouldThrowException_whenUserDisabled() {
                        // Arrange
                        testUser.setEnabled(false);
                        when(userRepository.findByEmail(loginRequest.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.login(loginRequest),
                                        "Should throw RuntimeException when user is disabled");
                        assertEquals("Account is disabled", ex.getMessage(),
                                        "Exception message should indicate disabled account");
                        verify(userRepository).findByEmail(loginRequest.getEmail());
                        verifyNoInteractions(jwtTokenProvider, passwordEncoder);
                }

                @Test
                @DisplayName("Should throw exception when user is blocked")
                void login_shouldThrowException_whenUserBlocked() {
                        // Arrange
                        testUser.setBlocked(true);
                        testUser.setBlockReason("Test block reason");
                        when(userRepository.findByEmail(loginRequest.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.login(loginRequest),
                                        "Should throw RuntimeException when user is blocked");
                        String expectedMessage = TestConstants.ErrorMessages.ACCOUNT_BLOCKED + " Test block reason";
                        assertEquals(expectedMessage, ex.getMessage(),
                                        "Exception message should match expected blocked account message");
                        verify(userRepository).findByEmail(loginRequest.getEmail());
                        verifyNoInteractions(jwtTokenProvider);
                }

                @Test
                @DisplayName("Should throw exception when email is not verified")
                void login_shouldThrowException_whenEmailNotVerified() {
                        // Arrange
                        testUser.setEmailVerified(false);
                        when(userRepository.findByEmail(loginRequest.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(userRepository.save(any(User.class)))
                                        .thenReturn(testUser);

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.login(loginRequest));
                        assertEquals("EMAIL_NOT_VERIFIED:" + testUser.getEmail(), ex.getMessage());
                }
        }

        @Nested
        @DisplayName("Token Refresh Tests")
        class TokenRefreshTests {
                @Test
                @DisplayName("Should return new tokens when refresh token is valid")
                void refresh_shouldReturnNewTokens_whenRefreshTokenValid() {
                        // Arrange
                        String refreshToken = TestConstants.Tokens.REFRESH_TOKEN;
                        when(jwtTokenProvider.validateRefreshToken(refreshToken))
                                        .thenReturn(true);
                        when(jwtTokenProvider.getEmailFromRefresh(refreshToken))
                                        .thenReturn(testUser.getEmail());
                        when(jwtTokenProvider.getRememberDaysFromRefresh(refreshToken))
                                        .thenReturn(null);
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(jwtTokenProvider.generateAccessToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.ACCESS_TOKEN);
                        when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.REFRESH_TOKEN);

                        // Act
                        Map<String, String> tokens = authService.refresh(refreshToken);

                        // Assert
                        assertNotNull(tokens);
                        assertEquals(TestConstants.Tokens.ACCESS_TOKEN, tokens.get("accessToken"));
                        assertEquals(TestConstants.Tokens.REFRESH_TOKEN, tokens.get("refreshToken"));
                }

                @Test
                @DisplayName("Should throw exception when refresh token is invalid")
                void refresh_shouldThrowException_whenRefreshTokenInvalid() {
                        // Arrange
                        String refreshToken = "invalidToken";
                        when(jwtTokenProvider.validateRefreshToken(refreshToken))
                                        .thenReturn(false);

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.refresh(refreshToken));
                        assertEquals("Invalid/expired refresh token", ex.getMessage());
                }
        }

        @Nested
        @DisplayName("Password Management Tests")
        class PasswordManagementTests {
                @Test
                @DisplayName("Should succeed when initiating password reset for existing user")
                void initiatePasswordReset_shouldSucceed_whenUserExists() {
                        // Arrange
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.initiatePasswordReset(testUser.getEmail()));
                        verify(userRepository).save(testUser);
                        verify(emailService).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.RESET_PASSWORD_SUBJECT),
                                anyString(),
                                anyString());
                }

                @Test
                @DisplayName("Should skip sending reset email when cooldown not expired")
                void initiatePasswordReset_shouldSkip_whenCooldownActive() {
                        // Arrange
                        testUser.setLastPasswordResetRequestedAt(LocalDateTime.now().minusMinutes(5));
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.initiatePasswordReset(testUser.getEmail()));
                        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString());
                        verify(userRepository, never()).save(any(User.class));
                }

                @Test
                @DisplayName("Should skip sending reset email for Google user when cooldown not expired")
                void initiatePasswordReset_shouldSkip_whenCooldownActiveForGoogleUser() {
                        // Arrange
                        testUser.setAuthProvider(AuthProvider.GOOGLE);
                        testUser.setLastPasswordResetRequestedAt(LocalDateTime.now().minusMinutes(5));
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.initiatePasswordReset(testUser.getEmail()));
                        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString());
                        verify(userRepository, never()).save(any(User.class));
                }

                @Test
                @DisplayName("Should not throw when user is not found during password reset")
                void initiatePasswordReset_shouldNotThrow_whenUserNotFound() {
                        // Arrange
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.empty());

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.initiatePasswordReset(TestConstants.UserData.TEST_EMAIL));
                }

                @Test
                @DisplayName("Should send blocked account notification instead of password reset for blocked LOCAL user")
                void initiatePasswordReset_shouldSendBlockedNotification_whenUserBlockedLocal() {
                        // Arrange
                        testUser.setBlocked(true);
                        testUser.setBlockReason("Blocked by administrator");
                        testUser.setBlockedAt(LocalDateTime.now().minusDays(1));
                        testUser.setAuthProvider(AuthProvider.LOCAL);
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        when(emailTemplateFactory.buildAccountBlockedByAdminText(anyString(), any(LocalDateTime.class)))
                                        .thenReturn("Blocked notification text");
                        when(emailTemplateFactory.buildAccountBlockedByAdminHtml(anyString(), any(LocalDateTime.class)))
                                        .thenReturn("Blocked notification html");

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.initiatePasswordReset(testUser.getEmail()));
                        verify(userRepository).save(testUser);
                        verify(emailService).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.ACCOUNT_BLOCKED_BY_ADMIN_SUBJECT),
                                anyString(),
                                anyString());
                        // Verify that password reset email is NOT sent
                        verify(emailService, never()).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.RESET_PASSWORD_SUBJECT),
                                anyString(),
                                anyString());
                }

                @Test
                @DisplayName("Should send blocked account notification instead of password reset for blocked GOOGLE user")
                void initiatePasswordReset_shouldSendBlockedNotification_whenUserBlockedGoogle() {
                        // Arrange
                        testUser.setBlocked(true);
                        testUser.setBlockReason("Blocked by administrator");
                        testUser.setBlockedAt(LocalDateTime.now().minusDays(1));
                        testUser.setAuthProvider(AuthProvider.GOOGLE);
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        when(emailTemplateFactory.buildAccountBlockedByAdminText(anyString(), any(LocalDateTime.class)))
                                        .thenReturn("Blocked notification text");
                        when(emailTemplateFactory.buildAccountBlockedByAdminHtml(anyString(), any(LocalDateTime.class)))
                                        .thenReturn("Blocked notification html");

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.initiatePasswordReset(testUser.getEmail()));
                        verify(userRepository).save(testUser);
                        verify(emailService).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.ACCOUNT_BLOCKED_BY_ADMIN_SUBJECT),
                                anyString(),
                                anyString());
                        // Verify that Google password reset email is NOT sent
                        verify(emailService, never()).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.GOOGLE_PASSWORD_RESET_SUBJECT),
                                anyString(),
                                anyString());
                }

                @Test
                @DisplayName("Should send blocked account notification when user is blocked without block reason")
                void initiatePasswordReset_shouldSendBlockedNotification_whenUserBlockedWithoutReason() {
                        // Arrange
                        testUser.setBlocked(true);
                        testUser.setBlockReason(null);
                        testUser.setBlockedAt(LocalDateTime.now().minusDays(1));
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        when(emailTemplateFactory.buildAccountBlockedByAdminText(any(), any(LocalDateTime.class)))
                                        .thenReturn("Blocked notification text");
                        when(emailTemplateFactory.buildAccountBlockedByAdminHtml(any(), any(LocalDateTime.class)))
                                        .thenReturn("Blocked notification html");

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.initiatePasswordReset(testUser.getEmail()));
                        verify(emailService).sendEmail(
                                eq(TestConstants.UserData.TEST_EMAIL),
                                eq(EmailConstants.ACCOUNT_BLOCKED_BY_ADMIN_SUBJECT),
                                anyString(),
                                anyString());
                }

                @Test
                @DisplayName("Should succeed when resetting password with valid token")
                void resetPassword_shouldSucceed_whenTokenValid() {
                        // Arrange
                        String resetToken = TestConstants.Tokens.RESET_TOKEN;
                        testUser.setResetPasswordToken(resetToken);
                        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
                        when(userRepository.findByResetPasswordToken(resetToken))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD))
                                        .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // Act & Assert
                        assertDoesNotThrow(() -> authService.resetPassword(resetToken,
                                        TestConstants.UserData.TEST_PASSWORD));
                        assertEquals(TestConstants.UserData.ENCODED_PASSWORD, testUser.getPassword());
                        assertNull(testUser.getResetPasswordToken());
                        assertNull(testUser.getResetPasswordTokenExpiry());
                        verify(userRepository).save(testUser);
                }

                @Test
                @DisplayName("Should throw exception when reset token is invalid")
                void resetPassword_shouldThrowException_whenTokenInvalid() {
                        // Arrange
                        when(userRepository.findByResetPasswordToken("invalidToken"))
                                        .thenReturn(Optional.empty());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.resetPassword("invalidToken",
                                                        TestConstants.UserData.TEST_PASSWORD));
                        assertEquals("Invalid or expired reset token.", ex.getMessage());
                }

                @Test
                @DisplayName("Should throw exception when reset token is expired")
                void resetPassword_shouldThrowException_whenTokenExpired() {
                        // Arrange
                        String resetToken = "resetTokenExpired";
                        testUser.setResetPasswordToken(resetToken);
                        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().minusMinutes(5));
                        when(userRepository.findByResetPasswordToken(resetToken))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.resetPassword(resetToken,
                                                        TestConstants.UserData.TEST_PASSWORD));
                        assertEquals("Expired reset token.", ex.getMessage());
                }

                @Test
                @DisplayName("Should return token when generating password reset token for existing user")
                void generatePasswordResetToken_shouldReturnToken_whenUserExists() {
                        // Arrange
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // Act
                        String token = authService.generatePasswordResetToken(testUser.getEmail());

                        // Assert
                        assertNotNull(token);
                        assertEquals(token, testUser.getResetPasswordToken());
                        assertNotNull(testUser.getResetPasswordTokenExpiry());
                        verify(userRepository).save(testUser);
                }

                @Test
                @DisplayName("Should throw exception when user is not found during token generation")
                void generatePasswordResetToken_shouldThrowException_whenUserNotFound() {
                        // Arrange
                        when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                                        .thenReturn(Optional.empty());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService
                                                        .generatePasswordResetToken(TestConstants.UserData.TEST_EMAIL));
                        assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, ex.getMessage());
                }

                @Test
                @DisplayName("Should return secure password when generating random password")
                void generateRandomPassword_shouldReturnSecurePassword_whenCalled() {
                        // Act
                        String randomPassword1 = authService.generateRandomPassword();
                        String randomPassword2 = authService.generateRandomPassword();

                        // Assert
                        assertNotNull(randomPassword1, "Generated password should not be null");
                        assertEquals(12, randomPassword1.length(), "Password should be 12 characters long");

                        // Check for randomness by generating two passwords and comparing
                        assertNotEquals(randomPassword1, randomPassword2,
                                        "Two generated passwords should not be identical");
                }
        }

        @Nested
        @DisplayName("OAuth2 Tests")
        class OAuth2Tests {
                @Test
                @DisplayName("Should return tokens when handling OAuth2 login for existing user")
                void handleOAuth2Login_shouldReturnTokens_whenUserExists() {
                        // Arrange
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));
                        when(jwtTokenProvider.generateAccessToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.ACCESS_TOKEN);
                        when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.REFRESH_TOKEN);

                        // Act
                        Map<String, String> tokens = authService.handleOAuth2Login(testUser.getEmail(),
                                        testUser.getName());

                        // Assert
                        assertNotNull(tokens);
                        assertEquals(TestConstants.Tokens.ACCESS_TOKEN, tokens.get("accessToken"));
                        assertEquals(TestConstants.Tokens.REFRESH_TOKEN, tokens.get("refreshToken"));
                }

                @Test
                @DisplayName("Should create user and return tokens when handling OAuth2 login for new user in whitelist")
                void handleOAuth2Login_shouldCreateUserAndReturnTokens_whenUserNew() {
                        // Arrange
                        String newEmail = "newuser@example.com";
                        String newName = "New User";
                        when(userRepository.findByEmail(newEmail))
                                        .thenReturn(Optional.empty());
                        
                        // Email is allowed for registration
                        doNothing().when(accessControlService).checkRegistrationAccess(newEmail);

                        Role userRole = createUserRole();
                        when(roleRepository.findByName(SecurityConstants.ROLE_USER))
                                        .thenReturn(Optional.of(userRole));
                        when(passwordEncoder.encode(anyString()))
                                        .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);

                        User newUser = createNewUser(newEmail, newName, userRole);
                        when(userRepository.save(any(User.class)))
                                        .thenReturn(newUser);
                        when(jwtTokenProvider.generateAccessToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.ACCESS_TOKEN);
                        when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                                        .thenReturn(TestConstants.Tokens.REFRESH_TOKEN);

                        // Act
                        Map<String, String> tokens = authService.handleOAuth2Login(newEmail, newName);

                        // Assert
                        assertNotNull(tokens);
                        assertEquals(TestConstants.Tokens.ACCESS_TOKEN, tokens.get("accessToken"));
                        assertEquals(TestConstants.Tokens.REFRESH_TOKEN, tokens.get("refreshToken"));
                        verify(accessControlService).checkRegistrationAccess(newEmail);
                }

                @Test
                @DisplayName("Should throw exception when email is not in whitelist during OAuth2 registration")
                void handleOAuth2Login_shouldThrowException_whenEmailNotInWhitelist() {
                        // Arrange
                        String newEmail = "notallowed@example.com";
                        String newName = "Not Allowed User";
                        when(userRepository.findByEmail(newEmail))
                                        .thenReturn(Optional.empty());
                        
                        // Email is not allowed for registration
                        doThrow(new com.authenticationservice.exception.RegistrationForbiddenException())
                                        .when(accessControlService).checkRegistrationAccess(newEmail);

                        // Act & Assert
                        com.authenticationservice.exception.RegistrationForbiddenException ex = assertThrows(
                                        com.authenticationservice.exception.RegistrationForbiddenException.class,
                                        () -> authService.handleOAuth2Login(newEmail, newName));
                        assertEquals("registration.forbidden", ex.getMessage());
                        verify(accessControlService).checkRegistrationAccess(newEmail);
                }

                @Test
                @DisplayName("Should throw exception when user is disabled during OAuth2 login")
                void handleOAuth2Login_shouldThrowException_whenUserDisabled() {
                        // Arrange
                        testUser.setEnabled(false);
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> authService.handleOAuth2Login(testUser.getEmail(), testUser.getName()));
                        assertEquals("Account is disabled", ex.getMessage());
                }

                @Test
                @DisplayName("Should throw InvalidCredentialsException when user is blocked during OAuth2 login")
                void handleOAuth2Login_shouldThrowInvalidCredentialsException_whenUserBlocked() {
                        // Arrange
                        testUser.setBlocked(true);
                        testUser.setBlockReason("Blocked for testing");
                        testUser.setBlockedAt(LocalDateTime.now());
                        when(userRepository.findByEmail(testUser.getEmail()))
                                        .thenReturn(Optional.of(testUser));

                        // Act & Assert
                        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                                        () -> authService.handleOAuth2Login(testUser.getEmail(), testUser.getName()));
                        assertNotNull(ex);
                        // Verify that InvalidCredentialsException is thrown to hide account status
                }
        }

        // ************** Helper Methods **************

        /**
         * Creates a registration request with default values
         * 
         * @return RegistrationRequest with default test data
         */
        private RegistrationRequest createRegistrationRequest() {
                RegistrationRequest request = new RegistrationRequest();
                request.setEmail(TestConstants.UserData.TEST_EMAIL);
                request.setName(TestConstants.UserData.TEST_USERNAME);
                request.setPassword(TestConstants.UserData.TEST_PASSWORD);
                return request;
        }


        /**
         * Creates a user role with default values
         * 
         * @return Role with default test data
         */
        private Role createUserRole() {
                Role userRole = new Role();
                userRole.setName(SecurityConstants.ROLE_USER);
                return userRole;
        }

        /**
         * Creates a verification request with default values
         * 
         * @return VerificationRequest with default test data
         */
        private VerificationRequest createVerificationRequest() {
                VerificationRequest request = new VerificationRequest();
                request.setEmail(TestConstants.UserData.TEST_EMAIL);
                String token = UUID.randomUUID().toString();
                request.setCode(token);
                return request;
        }

        /**
         * Creates a new user with specified parameters
         * 
         * @param email User email
         * @param name  Username
         * @param role  User role
         * @return User with specified parameters
         */
        private User createNewUser(String email, String name, Role role) {
                User user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setEnabled(true);
                user.setEmailVerified(true);
                user.setPassword(TestConstants.UserData.ENCODED_PASSWORD);
                Set<Role> roles = new HashSet<>();
                roles.add(role);
                user.setRoles(roles);
                return user;
        }
}
