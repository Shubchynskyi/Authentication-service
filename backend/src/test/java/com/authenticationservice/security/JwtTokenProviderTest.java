package com.authenticationservice.security;

import com.authenticationservice.config.JwtProperties;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create JwtProperties with test values
        jwtProperties = new JwtProperties();
        jwtProperties.setAccessSecret(TestConstants.TestProperties.JWT_ACCESS_SECRET);
        jwtProperties.setRefreshSecret(TestConstants.TestProperties.JWT_REFRESH_SECRET);
        jwtProperties.setAccessExpiration(900000L); // 15 minutes
        jwtProperties.setRefreshExpiration(604800000L); // 7 days

        // Create JwtTokenProvider with properties
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        // Create test user
        testUser = createTestUser();
    }

    /**
     * Creates a test user with default values
     * 
     * @return User with default test data
     */
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail(TestConstants.UserData.TEST_EMAIL);
        user.setName(TestConstants.UserData.TEST_USERNAME);

        Role userRole = new Role();
        userRole.setName(SecurityConstants.ROLE_USER);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        user.setLockTime(null);

        return user;
    }

    @Nested
    @DisplayName("Access Token Generation Tests")
    class AccessTokenGenerationTests {
        @Test
        @DisplayName("Should generate valid access token when user is provided")
        void generateAccessToken_shouldGenerateValidToken_whenUserProvided() {
            // Act
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // Assert
            assertNotNull(token, "Generated access token should not be null");
            assertFalse(token.isEmpty(), "Generated access token should not be empty");
            assertTrue(jwtTokenProvider.validateAccessToken(token), 
                      "Generated token should be valid");
        }

        @Test
        @DisplayName("Should extract email from generated access token")
        void generateAccessToken_shouldContainEmail_whenTokenGenerated() {
            // Act
            String token = jwtTokenProvider.generateAccessToken(testUser);
            String extractedEmail = jwtTokenProvider.getEmailFromAccess(token);

            // Assert
            assertEquals(TestConstants.UserData.TEST_EMAIL, extractedEmail,
                        "Extracted email should match test user email");
        }

        @Test
        @DisplayName("Should extract roles from generated access token")
        void generateAccessToken_shouldContainRoles_whenTokenGenerated() {
            // Act
            String token = jwtTokenProvider.generateAccessToken(testUser);
            List<String> extractedRoles = jwtTokenProvider.getRolesFromAccess(token);

            // Assert
            assertNotNull(extractedRoles, "Extracted roles should not be null");
            assertTrue(extractedRoles.contains(SecurityConstants.ROLE_USER),
                      "Extracted roles should contain ROLE_USER");
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void generateAccessToken_shouldGenerateDifferentTokens_whenDifferentUsers() {
            // Arrange
            User user2 = createTestUser();
            user2.setEmail("different@example.com");

            // Act
            String token1 = jwtTokenProvider.generateAccessToken(testUser);
            String token2 = jwtTokenProvider.generateAccessToken(user2);

            // Assert
            assertNotEquals(token1, token2,
                           "Tokens for different users should be different");
        }
    }

    @Nested
    @DisplayName("Refresh Token Generation Tests")
    class RefreshTokenGenerationTests {
        @Test
        @DisplayName("Should generate valid refresh token when user is provided")
        void generateRefreshToken_shouldGenerateValidToken_whenUserProvided() {
            // Act
            String token = jwtTokenProvider.generateRefreshToken(testUser);

            // Assert
            assertNotNull(token, "Generated refresh token should not be null");
            assertFalse(token.isEmpty(), "Generated refresh token should not be empty");
            assertTrue(jwtTokenProvider.validateRefreshToken(token),
                      "Generated refresh token should be valid");
        }

        @Test
        @DisplayName("Should extract email from generated refresh token")
        void generateRefreshToken_shouldContainEmail_whenTokenGenerated() {
            // Act
            String token = jwtTokenProvider.generateRefreshToken(testUser);
            String extractedEmail = jwtTokenProvider.getEmailFromRefresh(token);

            // Assert
            assertEquals(TestConstants.UserData.TEST_EMAIL, extractedEmail,
                        "Extracted email should match test user email");
        }

        @Test
        @DisplayName("Should generate valid refresh tokens for same user on different calls")
        void generateRefreshToken_shouldGenerateValidTokens_whenCalledMultipleTimes() {
            // Act
            String token1 = jwtTokenProvider.generateRefreshToken(testUser);
            // Add small delay to ensure different timestamp
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String token2 = jwtTokenProvider.generateRefreshToken(testUser);

            // Assert - both tokens should be valid
            assertTrue(jwtTokenProvider.validateRefreshToken(token1),
                      "First refresh token should be valid");
            assertTrue(jwtTokenProvider.validateRefreshToken(token2),
                      "Second refresh token should be valid");
            // Tokens may be the same if generated in the same millisecond, which is acceptable
            // The important thing is that both are valid and can be used
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {
        @Test
        @DisplayName("Should validate access token when token is valid")
        void validateAccessToken_shouldReturnTrue_whenTokenValid() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // Act
            boolean isValid = jwtTokenProvider.validateAccessToken(token);

            // Assert
            assertTrue(isValid, "Valid access token should be validated successfully");
        }

        @Test
        @DisplayName("Should invalidate access token when token is invalid")
        void validateAccessToken_shouldReturnFalse_whenTokenInvalid() {
            // Arrange
            String invalidToken = "invalid.token.here";

            // Act
            boolean isValid = jwtTokenProvider.validateAccessToken(invalidToken);

            // Assert
            assertFalse(isValid, "Invalid access token should not be validated");
        }

        @Test
        @DisplayName("Should invalidate access token when token is empty")
        void validateAccessToken_shouldReturnFalse_whenTokenEmpty() {
            // Act
            boolean isValid = jwtTokenProvider.validateAccessToken("");

            // Assert
            assertFalse(isValid, "Empty access token should not be validated");
        }

        @Test
        @DisplayName("Should validate refresh token when token is valid")
        void validateRefreshToken_shouldReturnTrue_whenTokenValid() {
            // Arrange
            String token = jwtTokenProvider.generateRefreshToken(testUser);

            // Act
            boolean isValid = jwtTokenProvider.validateRefreshToken(token);

            // Assert
            assertTrue(isValid, "Valid refresh token should be validated successfully");
        }

        @Test
        @DisplayName("Should invalidate refresh token when token is invalid")
        void validateRefreshToken_shouldReturnFalse_whenTokenInvalid() {
            // Arrange
            String invalidToken = "invalid.refresh.token";

            // Act
            boolean isValid = jwtTokenProvider.validateRefreshToken(invalidToken);

            // Assert
            assertFalse(isValid, "Invalid refresh token should not be validated");
        }

        @Test
        @DisplayName("Should not validate access token with refresh token")
        void validateAccessToken_shouldReturnFalse_whenRefreshTokenProvided() {
            // Arrange
            String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

            // Act
            boolean isValid = jwtTokenProvider.validateAccessToken(refreshToken);

            // Assert
            assertFalse(isValid, "Access token validator should not accept refresh token");
        }

        @Test
        @DisplayName("Should not validate refresh token with access token")
        void validateRefreshToken_shouldReturnFalse_whenAccessTokenProvided() {
            // Arrange
            String accessToken = jwtTokenProvider.generateAccessToken(testUser);

            // Act
            boolean isValid = jwtTokenProvider.validateRefreshToken(accessToken);

            // Assert
            assertFalse(isValid, "Refresh token validator should not accept access token");
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {
        @Test
        @DisplayName("Should extract email from access token")
        void getEmailFromAccess_shouldReturnEmail_whenTokenValid() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // Act
            String email = jwtTokenProvider.getEmailFromAccess(token);

            // Assert
            assertEquals(TestConstants.UserData.TEST_EMAIL, email,
                        "Extracted email should match user email");
        }

        @Test
        @DisplayName("Should extract email from refresh token")
        void getEmailFromRefresh_shouldReturnEmail_whenTokenValid() {
            // Arrange
            String token = jwtTokenProvider.generateRefreshToken(testUser);

            // Act
            String email = jwtTokenProvider.getEmailFromRefresh(token);

            // Assert
            assertEquals(TestConstants.UserData.TEST_EMAIL, email,
                        "Extracted email should match user email");
        }

        @Test
        @DisplayName("Should extract roles from access token")
        void getRolesFromAccess_shouldReturnRoles_whenTokenValid() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // Act
            List<String> roles = jwtTokenProvider.getRolesFromAccess(token);

            // Assert
            assertNotNull(roles, "Extracted roles should not be null");
            assertTrue(roles.contains(SecurityConstants.ROLE_USER),
                      "Extracted roles should contain ROLE_USER");
        }

        @Test
        @DisplayName("Should extract multiple roles when user has multiple roles")
        void getRolesFromAccess_shouldReturnAllRoles_whenUserHasMultipleRoles() {
            // Arrange
            Role adminRole = new Role();
            adminRole.setName(SecurityConstants.ROLE_ADMIN);
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(new Role(SecurityConstants.ROLE_USER));
            userRoles.add(adminRole);
            testUser.setRoles(userRoles);
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // Act
            List<String> roles = jwtTokenProvider.getRolesFromAccess(token);

            // Assert
            assertNotNull(roles, "Extracted roles should not be null");
            assertTrue(roles.contains(SecurityConstants.ROLE_USER),
                      "Extracted roles should contain ROLE_USER");
            assertTrue(roles.contains(SecurityConstants.ROLE_ADMIN),
                      "Extracted roles should contain ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Refresh Token TTL Tests")
    class RefreshTokenTtlTests {
        @Test
        @DisplayName("Should return positive TTL for valid refresh token")
        void getRefreshTokenTtlSeconds_shouldReturnPositive_whenTokenValid() {
            String token = jwtTokenProvider.generateRefreshToken(testUser);

            long ttlSeconds = jwtTokenProvider.getRefreshTokenTtlSeconds(token);

            assertTrue(ttlSeconds > 0, "TTL should be positive for valid refresh token");
            assertTrue(ttlSeconds <= TimeUnit.MILLISECONDS.toSeconds(jwtProperties.getRefreshExpiration()),
                    "TTL should not exceed configured refresh expiration");
        }

        @Test
        @DisplayName("Should return zero TTL for invalid refresh token")
        void getRefreshTokenTtlSeconds_shouldReturnZero_whenTokenInvalid() {
            long ttlSeconds = jwtTokenProvider.getRefreshTokenTtlSeconds("invalid.refresh.token");

            assertEquals(0, ttlSeconds, "TTL should be zero for invalid refresh token");
        }
    }
}

