package com.authenticationservice.controller;

import com.authenticationservice.config.BaseIntegrationTest;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UpdateUserRolesRequest;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AccessModeSettingsRepository;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.yml")
@Import(com.authenticationservice.config.TestConfig.class)
@WithMockUser(username = "admin@example.com", roles = {"ADMIN", "USER"})
@DisplayName("AdminController Integration Tests")
class AdminControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessModeSettingsRepository accessModeSettingsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        transactionTemplate.execute(status -> {
            cleanupTestData();
            accessModeSettingsRepository.deleteAll();
            accessModeSettingsRepository.flush();
            ensureRolesExist();
            ensureAccessModeSettings(AccessMode.WHITELIST);

            adminUser = createAdminUser();
            regularUser = createDefaultTestUser();
            User secondUser = createTestUser(
                    TestConstants.TestData.SECOND_USER_EMAIL,
                    TestConstants.TestData.SECOND_USER_NAME,
                    TestConstants.TestData.SECOND_USER_PASSWORD,
                    true, false, true,
                    Set.of(SecurityConstants.ROLE_USER)
            );
            userRepository.save(adminUser);
            userRepository.save(regularUser);
            userRepository.save(secondUser);
            userRepository.flush();
            return null;
        });

    }

    @Test
    @DisplayName("Should get all users successfully")
    void getAllUsers_shouldGetAllUsersSuccessfully() throws Exception {
        // getAllUsers excludes the current user (admin), so we should get regularUser and secondUser (2 users)
        // Act & Assert
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_URL)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should return forbidden for non-admin user")
    void getAllUsers_shouldReturnForbiddenForNonAdmin() throws Exception {
        // Arrange - Clear SecurityContext and set up regular user
        SecurityContextHolder.clearContext();
        org.springframework.security.core.Authentication auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                TestConstants.UserData.TEST_EMAIL, 
                null, 
                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        try {
            // Act & Assert
            mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_URL))
                    .andExpect(status().isForbidden());
        } finally {
            // Clean up
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Should get whitelist successfully")
    void getWhitelist_shouldGetWhitelistSuccessfully() throws Exception {
        // Arrange
        AllowedEmail allowedEmail = new AllowedEmail();
        allowedEmail.setEmail(TestConstants.TestData.WHITELIST_EMAIL);
        allowedEmailRepository.save(allowedEmail);

        // Act & Assert
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.WHITELIST_URL)
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should add email to whitelist successfully")
    void addToWhitelist_shouldAddEmailSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.WHITELIST_ADD_URL)
                        .param("email", TestConstants.TestData.NEW_WHITELIST_EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(MessageConstants.EMAIL_ADDED_TO_WHITELIST));
    }

    @Test
    @DisplayName("Should get all roles successfully")
    void getAllRoles_shouldGetAllRolesSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.ROLES_URL)
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value(SecurityConstants.ROLE_USER))
                .andExpect(jsonPath("$[1]").value(SecurityConstants.ROLE_ADMIN));
    }

    @Test
    @DisplayName("Should get all users with search parameter")
    void getAllUsers_shouldGetUsersWithSearch() throws Exception {
        // Act & Assert - search for "test" should find regularUser
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_URL)
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_shouldCreateUserSuccessfully() throws Exception {
        // Arrange
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
        request.setUsername(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);
        request.setRoles(List.of(SecurityConstants.ROLE_USER));
        request.setIsAktiv(true);
        request.setIsBlocked(false);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(MessageConstants.USER_CREATED));

        // Verify user was created
        assertTrue(userRepository.findByEmail(TestConstants.TestData.NEW_USER_EMAIL).isPresent());
        // Verify email was added to whitelist
        assertTrue(allowedEmailRepository.findByEmail(TestConstants.TestData.NEW_USER_EMAIL).isPresent());
    }

    @Test
    @DisplayName("Should return bad request when creating user with existing email")
    void createUser_shouldReturnBadRequest_whenEmailExists() throws Exception {
        // Arrange
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL); // Already exists
        request.setUsername(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);
        request.setRoles(List.of(SecurityConstants.ROLE_USER));

        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get user by id successfully")
    void getUserById_shouldGetUserSuccessfully() throws Exception {
        // Arrange
        Long userId = regularUser.getId();

        // Act & Assert
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.USER_ID_URL, userId)
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TestConstants.UserData.TEST_EMAIL))
                .andExpect(jsonPath("$.username").value(TestConstants.UserData.TEST_USERNAME));
    }

    @Test
    @DisplayName("Should return not found when user does not exist")
    void getUserById_shouldReturnNotFound_whenUserNotExists() throws Exception {
        // Arrange
        Long nonExistentId = 999L;

        // Act & Assert
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.USER_ID_URL, nonExistentId)
)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_shouldUpdateUserSuccessfully() throws Exception {
        // Arrange
        Long userId = regularUser.getId();
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername(TestConstants.TestData.UPDATED_NAME);
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setIsAktiv(true);
        request.setIsBlocked(false);

        // Act & Assert
        mockMvc.perform(put(ApiConstants.ADMIN_BASE_URL + ApiConstants.USER_ID_URL, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(MessageConstants.USER_UPDATED));

        // Verify user was updated
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(TestConstants.TestData.UPDATED_NAME, updatedUser.getName());
    }

    @Test
    @DisplayName("Should block user when update request has isBlocked true")
    void updateUser_shouldBlockUser_whenIsBlockedTrue() throws Exception {
        // Arrange
        Long userId = regularUser.getId();
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setIsBlocked(true);
        request.setBlockReason("Test block reason");

        // Act & Assert
        mockMvc.perform(put(ApiConstants.ADMIN_BASE_URL + ApiConstants.USER_ID_URL, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify user was blocked
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertTrue(updatedUser.isBlocked());
        assertEquals("Test block reason", updatedUser.getBlockReason());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_shouldDeleteUserSuccessfully() throws Exception {
        // Arrange
        Long userId = regularUser.getId();

        // Act & Assert
        mockMvc.perform(delete(ApiConstants.ADMIN_BASE_URL + ApiConstants.USER_ID_URL, userId)
)
                .andExpect(status().isOk())
                .andExpect(content().string(MessageConstants.USER_DELETED));

        // Verify user was deleted
        assertFalse(userRepository.findById(userId).isPresent());
    }

    @Test
    @DisplayName("Should return bad request when deleting non-existent user")
    void deleteUser_shouldReturnBadRequest_whenUserNotExists() throws Exception {
        // Arrange
        Long nonExistentId = 999L;

        // Act & Assert
        mockMvc.perform(delete(ApiConstants.ADMIN_BASE_URL + ApiConstants.USER_ID_URL, nonExistentId)
)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should update user roles successfully")
    void updateUserRoles_shouldUpdateRolesSuccessfully() throws Exception {
        // Arrange
        Long userId = regularUser.getId();
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(List.of(SecurityConstants.ROLE_USER, SecurityConstants.ROLE_ADMIN));

        // Act & Assert
        mockMvc.perform(put(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_ID_ROLES_URL, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.containsInAnyOrder(SecurityConstants.ROLE_USER, SecurityConstants.ROLE_ADMIN)));

        // Verify roles were updated
        User updatedUser = userRepository.findById(userId).orElseThrow();
        Set<String> roleNames = updatedUser.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(roleNames.contains(SecurityConstants.ROLE_USER));
        assertTrue(roleNames.contains(SecurityConstants.ROLE_ADMIN));
    }

    @Test
    @DisplayName("Should return bad request when updating roles with invalid role")
    void updateUserRoles_shouldReturnBadRequest_whenInvalidRole() throws Exception {
        // Arrange
        Long userId = regularUser.getId();
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(List.of("ROLE_INVALID"));

        // Act & Assert
        mockMvc.perform(put(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_ID_ROLES_URL, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should remove email from whitelist successfully")
    void removeFromWhitelist_shouldRemoveEmailSuccessfully() throws Exception {
        // Arrange
        AllowedEmail allowedEmail = new AllowedEmail();
        allowedEmail.setEmail(TestConstants.TestData.NEW_WHITELIST_EMAIL);
        allowedEmailRepository.save(allowedEmail);
        allowedEmailRepository.flush();

        // Act & Assert
        mockMvc.perform(delete(ApiConstants.ADMIN_BASE_URL + ApiConstants.WHITELIST_REMOVE_URL)
                        .param("email", TestConstants.TestData.NEW_WHITELIST_EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(MessageConstants.EMAIL_REMOVED_FROM_WHITELIST));

        // Verify email was removed
        assertFalse(allowedEmailRepository.findByEmail(TestConstants.TestData.NEW_WHITELIST_EMAIL).isPresent());
    }

    @Test
    @DisplayName("Should return bad request when removing non-existent email from whitelist")
    void removeFromWhitelist_shouldReturnBadRequest_whenEmailNotExists() throws Exception {
        // Act & Assert
        mockMvc.perform(delete(ApiConstants.ADMIN_BASE_URL + ApiConstants.WHITELIST_REMOVE_URL)
                        .param("email", "nonexistent@example.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should verify admin password successfully")
    void verifyAdmin_shouldVerifyPasswordSuccessfully() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("password", TestConstants.TestData.ADMIN_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.VERIFY_ADMIN_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.UserData.ADMIN_EMAIL)
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                        .principal(() -> TestConstants.UserData.ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(MessageConstants.PASSWORD_VERIFIED));
    }

    @Test
    @DisplayName("Should return unauthorized when admin password is incorrect")
    void verifyAdmin_shouldReturnUnauthorized_whenPasswordIncorrect() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("password", "WrongPassword123@");

        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.VERIFY_ADMIN_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.UserData.ADMIN_EMAIL)
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                        .principal(() -> TestConstants.UserData.ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(MessageConstants.INVALID_PASSWORD));
    }

    @Test
    @DisplayName("Should return bad request when password is missing")
    void verifyAdmin_shouldReturnBadRequest_whenPasswordMissing() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        // No password field

        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.VERIFY_ADMIN_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.UserData.ADMIN_EMAIL)
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                        .principal(() -> TestConstants.UserData.ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(MessageConstants.PASSWORD_IS_REQUIRED));
    }

    @Test
    @DisplayName("Should return bad request when password is blank")
    void verifyAdmin_shouldReturnBadRequest_whenPasswordBlank() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("password", "   ");

        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.VERIFY_ADMIN_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.UserData.ADMIN_EMAIL)
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                        .principal(() -> TestConstants.UserData.ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(MessageConstants.PASSWORD_IS_REQUIRED));
    }
}