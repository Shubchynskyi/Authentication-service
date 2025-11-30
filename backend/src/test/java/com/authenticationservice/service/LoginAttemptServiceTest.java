package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService Tests")
class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private User testUser;
    private static final String FRONTEND_URL = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail(TestConstants.UserData.TEST_EMAIL);
        user.setName(TestConstants.UserData.TEST_USERNAME);
        user.setPassword(TestConstants.UserData.ENCODED_PASSWORD);
        user.setEnabled(true);
        user.setBlocked(false);
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        return user;
    }

    @Nested
    @DisplayName("Failed Login Handling Tests")
    class FailedLoginHandlingTests {

        @Test
        @DisplayName("Should increment attempts when user exists")
        void handleFailedLogin_shouldIncrementAttempts_whenUserExists() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            loginAttemptService.handleFailedLogin(testUser, FRONTEND_URL);

            // Assert
            assertEquals(1, testUser.getFailedLoginAttempts());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should set temporary lock when reached 5 attempts")
        void handleFailedLogin_shouldSetTemporaryLock_whenReached5Attempts() {
            // Arrange
            testUser.setFailedLoginAttempts(4);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            loginAttemptService.handleFailedLogin(testUser, FRONTEND_URL);

            // Assert
            assertEquals(5, testUser.getFailedLoginAttempts());
            assertNotNull(testUser.getLockTime());
            assertTrue(testUser.getLockTime().isAfter(LocalDateTime.now()));
            verify(userRepository, atLeast(2)).save(testUser);
        }

        @Test
        @DisplayName("Should send email when reached 5 attempts")
        void handleFailedLogin_shouldSendEmail_whenReached5Attempts() {
            // Arrange
            testUser.setFailedLoginAttempts(4);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            loginAttemptService.handleFailedLogin(testUser, FRONTEND_URL);

            // Assert
            verify(emailService).sendEmail(eq(testUser.getEmail()), anyString(), anyString());
        }

        @Test
        @DisplayName("Should block account when reached 10 attempts")
        void handleFailedLogin_shouldBlockAccount_whenReached10Attempts() {
            // Arrange
            testUser.setFailedLoginAttempts(9);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            loginAttemptService.handleFailedLogin(testUser, FRONTEND_URL);

            // Assert
            assertEquals(10, testUser.getFailedLoginAttempts());
            assertTrue(testUser.isBlocked());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should send email when reached 10 attempts and account is blocked")
        void handleFailedLogin_shouldSendEmail_whenReached10Attempts() {
            // Arrange
            testUser.setFailedLoginAttempts(9);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                // Simulate blocking after save
                if (savedUser.getFailedLoginAttempts() >= 10) {
                    savedUser.setBlocked(true);
                }
                return savedUser;
            });

            // Act
            loginAttemptService.handleFailedLogin(testUser, FRONTEND_URL);

            // Assert
            verify(emailService, atLeastOnce()).sendEmail(eq(testUser.getEmail()), anyString(), anyString());
        }

        @Test
        @DisplayName("Should not throw when user not found")
        void handleFailedLogin_shouldNotThrow_whenUserNotFound() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

            // Act & Assert
            assertDoesNotThrow(() -> loginAttemptService.handleFailedLogin(testUser, FRONTEND_URL));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should find user by email when not found by ID")
        void handleFailedLogin_shouldFindUserByEmail_whenNotFoundById() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            loginAttemptService.handleFailedLogin(testUser, FRONTEND_URL);

            // Assert
            assertEquals(1, testUser.getFailedLoginAttempts());
            verify(userRepository).save(testUser);
        }
    }
}

