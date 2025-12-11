package com.authenticationservice.controller;

import com.authenticationservice.config.BaseIntegrationTest;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.User;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.yml")
@Import(com.authenticationservice.config.TestConfig.class)
@DisplayName("ProfileController Integration Tests")
class ProfileControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        transactionTemplate.execute(status -> {
            cleanupTestData();
            ensureRolesExist();
            ensureAccessModeSettings(AccessMode.WHITELIST);

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
                .andExpect(status().isUnauthorized());
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
    @DisplayName("Should return bad request when new password is provided without current password")
    void updateProfile_shouldReturnBadRequest_whenPasswordWithoutCurrent() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(TestConstants.TestData.UPDATED_NAME);
        request.setPassword(TestConstants.TestData.NEW_PASSWORD_VALUE);

        mockMvc.perform(post(ApiConstants.PROTECTED_BASE_URL + ApiConstants.PROFILE_URL)
                        .principal(() -> TestConstants.UserData.TEST_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current password is required")));
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
