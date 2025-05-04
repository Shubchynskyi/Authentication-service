package com.authenticationservice.service;

import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
    private RoleRepository roleRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Arrange: Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName(TestConstants.UserData.TEST_USERNAME);
        testUser.setEmail(TestConstants.UserData.TEST_EMAIL);
        testUser.setPassword(TestConstants.UserData.ENCODED_PASSWORD);
        testUser.setEnabled(true);
        testUser.setBlocked(false);
        testUser.setEmailVerified(true);
        testUser.setFailedLoginAttempts(0);

        Role userRole = new Role();
        userRole.setName(SecurityConstants.ROLE_USER);
        testUser.setRoles(Set.of(userRole));

        // Arrange: Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail(TestConstants.UserData.TEST_EMAIL);
        loginRequest.setPassword(TestConstants.UserData.TEST_PASSWORD);

        // Setup service configuration
        ReflectionTestUtils.setField(authService, "frontendUrl", TestConstants.Urls.FRONTEND_URL);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {
        @Test
        @DisplayName("Should succeed when all data is valid")
        void register_shouldSucceed_whenAllDataValid() {
            // Arrange
            RegistrationRequest request = createRegistrationRequest();
            AllowedEmail allowedEmail = createAllowedEmail();
            Role userRole = createUserRole();

            when(allowedEmailRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(allowedEmail));
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
            verify(userRepository).save(any(User.class));
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("Should throw exception when email is not allowed")
        void register_shouldThrowException_whenEmailNotAllowed() {
            // Arrange
            RegistrationRequest request = createRegistrationRequest();
            when(allowedEmailRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertEquals("This email is not in whitelist. Registration is forbidden.", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user already exists")
        void register_shouldThrowException_whenUserAlreadyExists() {
            // Arrange
            RegistrationRequest request = createRegistrationRequest();
            when(allowedEmailRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(new AllowedEmail()));
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertEquals("User with this email already exists.", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when role is not found")
        void register_shouldThrowException_whenRoleNotFound() {
            // Arrange
            RegistrationRequest request = createRegistrationRequest();
            when(allowedEmailRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(new AllowedEmail()));
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.empty());
            when(roleRepository.findByName(SecurityConstants.ROLE_USER))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertEquals("Role ROLE_USER not found in database.", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when email sending fails")
        void register_shouldThrowException_whenEmailSendingFails() {
            // Arrange
            RegistrationRequest request = createRegistrationRequest();
            AllowedEmail allowedEmail = createAllowedEmail();
            Role userRole = createUserRole();

            when(allowedEmailRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(allowedEmail));
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.empty());
            when(roleRepository.findByName(SecurityConstants.ROLE_USER))
                    .thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD))
                    .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doThrow(new RuntimeException("Mail server error"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertTrue(ex.getMessage().contains("Error sending verification email."));
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
            verify(mailSender).send(any(SimpleMailMessage.class));
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
            assertNotNull(tokens);
            assertEquals(TestConstants.Tokens.ACCESS_TOKEN, tokens.get("accessToken"));
            assertEquals(TestConstants.Tokens.REFRESH_TOKEN, tokens.get("refreshToken"));
            verify(userRepository).findByEmail(loginRequest.getEmail());
            verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        }

        @Test
        @DisplayName("Should throw exception when password is invalid")
        void login_shouldThrowException_whenPasswordInvalid() {
            // Arrange
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                    .thenReturn(false);
            when(userRepository.save(any(User.class)))
                    .thenReturn(testUser);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));
            assertEquals(SecurityConstants.INVALID_PASSWORD_ERROR, ex.getMessage());
            verify(userRepository).findByEmail(loginRequest.getEmail());
            verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        }

        @Test
        @DisplayName("Should throw exception when user is disabled")
        void login_shouldThrowException_whenUserDisabled() {
            // Arrange
            testUser.setEnabled(false);
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                    .thenReturn(true);
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(testUser));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));
            assertEquals("Account is disabled", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user is blocked")
        void login_shouldThrowException_whenUserBlocked() {
            // Arrange
            testUser.setBlocked(true);
            testUser.setBlockReason("Test block reason");
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                    .thenReturn(true);
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(testUser));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));
            assertTrue(ex.getMessage().contains("Account is blocked"));
            assertTrue(ex.getMessage().contains("Test block reason"));
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
            verify(mailSender).send(any(SimpleMailMessage.class));
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
            assertDoesNotThrow(() -> authService.resetPassword(resetToken, TestConstants.UserData.TEST_PASSWORD));
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
                    () -> authService.resetPassword("invalidToken", TestConstants.UserData.TEST_PASSWORD));
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
                    () -> authService.resetPassword(resetToken, TestConstants.UserData.TEST_PASSWORD));
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
                    () -> authService.generatePasswordResetToken(TestConstants.UserData.TEST_EMAIL));
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, ex.getMessage());
        }

        @Test
        @DisplayName("Should generate 12 character random password")
        void generateRandomPassword_shouldReturn12CharString() {
            // Act
            String randomPassword = authService.generateRandomPassword();

            // Assert
            assertNotNull(randomPassword);
            assertEquals(12, randomPassword.length());
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
            Map<String, String> tokens = authService.handleOAuth2Login(testUser.getEmail(), testUser.getName());

            // Assert
            assertNotNull(tokens);
            assertEquals(TestConstants.Tokens.ACCESS_TOKEN, tokens.get("accessToken"));
            assertEquals(TestConstants.Tokens.REFRESH_TOKEN, tokens.get("refreshToken"));
        }

        @Test
        @DisplayName("Should create user and return tokens when handling OAuth2 login for new user")
        void handleOAuth2Login_shouldCreateUserAndReturnTokens_whenUserNew() {
            // Arrange
            String newEmail = "newuser@example.com";
            String newName = "New User";
            when(userRepository.findByEmail(newEmail))
                    .thenReturn(Optional.empty());

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
        @DisplayName("Should throw exception when user is blocked during OAuth2 login")
        void handleOAuth2Login_shouldThrowException_whenUserBlocked() {
            // Arrange
            testUser.setBlocked(true);
            testUser.setBlockReason("Blocked for testing");
            when(userRepository.findByEmail(testUser.getEmail()))
                    .thenReturn(Optional.of(testUser));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.handleOAuth2Login(testUser.getEmail(), testUser.getName()));
            assertTrue(ex.getMessage().contains("Account is blocked"));
            assertTrue(ex.getMessage().contains("Blocked for testing"));
        }
    }

    // ************** Helper Methods **************

    private RegistrationRequest createRegistrationRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setName(TestConstants.UserData.TEST_USERNAME);
        request.setPassword(TestConstants.UserData.TEST_PASSWORD);
        return request;
    }

    private AllowedEmail createAllowedEmail() {
        AllowedEmail allowedEmail = new AllowedEmail();
        allowedEmail.setEmail(TestConstants.UserData.TEST_EMAIL);
        return allowedEmail;
    }

    private Role createUserRole() {
        Role userRole = new Role();
        userRole.setName(SecurityConstants.ROLE_USER);
        return userRole;
    }

    private VerificationRequest createVerificationRequest() {
        VerificationRequest request = new VerificationRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        String token = UUID.randomUUID().toString();
        request.setCode(token);
        return request;
    }

    private User createNewUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setPassword(TestConstants.UserData.ENCODED_PASSWORD);
        user.setRoles(Set.of(role));
        return user;
    }
}
