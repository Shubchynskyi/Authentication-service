package com.authenticationservice.controller;

import com.authenticationservice.config.TestPropertyConfigurator;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeSettings;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AccessModeSettingsRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Transactional
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.yml")
@Import(com.authenticationservice.config.TestConfig.class)
@DisplayName("ProfileController Integration Tests")
class ProfileControllerIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers manages lifecycle automatically via @Testcontainers
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestConstants.TestDatabase.POSTGRES_IMAGE)
            .withDatabaseName(TestConstants.TestDatabase.DATABASE_NAME)
            .withUsername(TestConstants.TestDatabase.USERNAME)
            .withPassword(TestConstants.TestDatabase.PASSWORD);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        TestPropertyConfigurator.configureProperties(registry, postgres);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccessModeSettingsRepository accessModeSettingsRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Use TransactionTemplate to explicitly commit transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            // Clean up
            userRepository.deleteAll();
            userRepository.flush();
            // Don't delete roles - DatabaseInitializer creates them on startup
            // Find existing roles or create if they don't exist
            Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(SecurityConstants.ROLE_USER);
                        return roleRepository.save(role);
                    });

            roleRepository.findByName(SecurityConstants.ROLE_ADMIN)
                    .orElseGet(() -> {
                        Role adminRole = new Role();
                        adminRole.setName(SecurityConstants.ROLE_ADMIN);
                        return roleRepository.save(adminRole);
                    });

            // Initialize AccessModeSettings if not exists
            if (accessModeSettingsRepository.findById(1L).isEmpty()) {
                AccessModeSettings settings = new AccessModeSettings();
                settings.setId(1L);
                settings.setMode(AccessMode.WHITELIST);
                accessModeSettingsRepository.save(settings);
            }

            // Create test user
            testUser = new User();
            testUser.setEmail(TestConstants.UserData.TEST_EMAIL);
            testUser.setName(TestConstants.UserData.TEST_USERNAME);
            testUser.setPassword(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD));
            testUser.setEnabled(true);
            testUser.setBlocked(false);
            testUser.setEmailVerified(true);
            testUser.setLockTime(null);
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(userRole);
            testUser.setRoles(userRoles);
            userRepository.save(testUser);
            // Force flush to ensure user is persisted
            userRepository.flush();
            return null;
        });
        
        // Verify user is actually in database after commit
        transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                .orElseThrow(() -> new RuntimeException("User was not saved to database in setUp!"))
        );
    }

    @Test
    @DisplayName("Should get profile successfully")
    void getProfile_shouldGetProfileSuccessfully() throws Exception {
        // Act & Assert - with addFilters = false, we need to mock Principal directly
        mockMvc.perform(get(ApiConstants.PROTECTED_BASE_URL + ApiConstants.PROFILE_URL)
                .principal(() -> TestConstants.UserData.TEST_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TestConstants.UserData.TEST_EMAIL))
                .andExpect(jsonPath("$.name").value(TestConstants.UserData.TEST_USERNAME));
    }

    @Test
    @DisplayName("Should return unauthorized without token")
    void getProfile_shouldReturnUnauthorizedWithoutToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ApiConstants.PROTECTED_BASE_URL + ApiConstants.PROFILE_URL))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should update profile successfully")
    void updateProfile_shouldUpdateProfileSuccessfully() throws Exception {
        // Arrange - verify user exists and password matches in a new transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User userBeforeUpdate = transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found in database"))
        );
        
        assertNotNull(userBeforeUpdate, "User should not be null");
        assertTrue(passwordEncoder.matches(TestConstants.UserData.TEST_PASSWORD, userBeforeUpdate.getPassword()),
                "Current password should match before update");

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(TestConstants.TestData.UPDATED_NAME);
        request.setCurrentPassword(TestConstants.UserData.TEST_PASSWORD);
        request.setPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.PROTECTED_BASE_URL + ApiConstants.PROFILE_URL)
                .principal(() -> TestConstants.UserData.TEST_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify the update in the database - check in a new transaction
        User updatedUser = transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found after update"))
        );
        assertNotNull(updatedUser, "Updated user should not be null");
        assertEquals(TestConstants.TestData.UPDATED_NAME, updatedUser.getName());
        assertTrue(passwordEncoder.matches(TestConstants.TestData.NEW_PASSWORD_VALUE, updatedUser.getPassword()));
    }

    @Test
    @DisplayName("Should update profile name only without password")
    void updateProfile_shouldUpdateNameOnly_whenPasswordNotProvided() throws Exception {
        // Arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String originalPassword = testUser.getPassword();

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(TestConstants.TestData.UPDATED_NAME);
        // No password fields

        // Act & Assert
        mockMvc.perform(post(ApiConstants.PROTECTED_BASE_URL + ApiConstants.PROFILE_URL)
                .principal(() -> TestConstants.UserData.TEST_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify the update in the database
        User updatedUser = transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found after update"))
        );
        assertNotNull(updatedUser, "Updated user should not be null");
        assertEquals(TestConstants.TestData.UPDATED_NAME, updatedUser.getName());
        // Password should remain unchanged
        assertEquals(originalPassword, updatedUser.getPassword());
    }

    @Test
    @DisplayName("Should return bad request when current password is incorrect")
    void updateProfile_shouldReturnBadRequest_whenCurrentPasswordIncorrect() throws Exception {
        // Arrange
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(TestConstants.TestData.UPDATED_NAME);
        request.setCurrentPassword("WrongPassword123@");
        request.setPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.PROTECTED_BASE_URL + ApiConstants.PROFILE_URL)
                .principal(() -> TestConstants.UserData.TEST_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Incorrect current password")));

        // Verify user was not updated
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User user = transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found"))
        );
        assertNotNull(user, "User should not be null");
        assertEquals(TestConstants.UserData.TEST_USERNAME, user.getName(), "Name should not be updated");
    }

    @Test
    @DisplayName("Should return bad request when new password is invalid")
    void updateProfile_shouldReturnBadRequest_whenNewPasswordInvalid() throws Exception {
        // Arrange
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(TestConstants.TestData.UPDATED_NAME);
        request.setCurrentPassword(TestConstants.UserData.TEST_PASSWORD);
        request.setPassword("123"); // Invalid password - too short

        // Act & Assert
        mockMvc.perform(post(ApiConstants.PROTECTED_BASE_URL + ApiConstants.PROFILE_URL)
                .principal(() -> TestConstants.UserData.TEST_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Password must be at least 8 characters")));

        // Verify user was not updated
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User user = transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found"))
        );
        assertNotNull(user, "User should not be null");
        assertEquals(TestConstants.UserData.TEST_USERNAME, user.getName(), "Name should not be updated");
    }
}
