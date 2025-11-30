package com.authenticationservice.controller;

import com.authenticationservice.config.TestPropertyConfigurator;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.yml")
@Import(com.authenticationservice.config.TestConfig.class)
@DisplayName("AdminController Integration Tests")
class AdminControllerIntegrationTest {

    @Container
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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AllowedEmailRepository allowedEmailRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminAccessToken;

    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();
        allowedEmailRepository.deleteAll();
        // Don't delete roles - DatabaseInitializer creates them on startup
        // Find existing roles or create if they don't exist
        Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(SecurityConstants.ROLE_USER);
                    return roleRepository.save(role);
                });

        Role adminRole = roleRepository.findByName(SecurityConstants.ROLE_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(SecurityConstants.ROLE_ADMIN);
                    return roleRepository.save(role);
                });

        // Create admin user
        adminUser = new User();
        adminUser.setEmail(TestConstants.UserData.ADMIN_EMAIL);
        adminUser.setName(TestConstants.TestData.ADMIN_NAME);
        adminUser.setPassword(passwordEncoder.encode(TestConstants.TestData.ADMIN_PASSWORD));
        adminUser.setEnabled(true);
        adminUser.setBlocked(false);
        adminUser.setEmailVerified(true);
        adminUser.setLockTime(null);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminRoles.add(userRole);
        adminUser.setRoles(adminRoles);
        userRepository.save(adminUser);

        // Create regular user
        regularUser = new User();
        regularUser.setEmail(TestConstants.UserData.TEST_EMAIL);
        regularUser.setName(TestConstants.UserData.TEST_USERNAME);
        regularUser.setPassword(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD));
        regularUser.setEnabled(true);
        regularUser.setBlocked(false);
        regularUser.setEmailVerified(true);
        regularUser.setLockTime(null);
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        regularUser.setRoles(userRoles);
        userRepository.save(regularUser);

        // Create another user to test pagination
        User secondUser = new User();
        secondUser.setEmail(TestConstants.TestData.SECOND_USER_EMAIL);
        secondUser.setName(TestConstants.TestData.SECOND_USER_NAME);
        secondUser.setPassword(passwordEncoder.encode(TestConstants.TestData.SECOND_USER_PASSWORD));
        secondUser.setEnabled(true);
        secondUser.setBlocked(false);
        secondUser.setEmailVerified(true);
        secondUser.setLockTime(null);
        Set<Role> secondUserRoles = new HashSet<>();
        secondUserRoles.add(userRole);
        secondUser.setRoles(secondUserRoles);
        userRepository.save(secondUser);
        
        // Force flush to ensure users are persisted
        userRepository.flush();

        // Generate admin access token
        adminAccessToken = jwtTokenProvider.generateAccessToken(adminUser);
    }

    @Test
    @DisplayName("Should get all users successfully")
    void getAllUsers_shouldGetAllUsersSuccessfully() throws Exception {
        // getAllUsers excludes the current user (admin), so we should get regularUser and secondUser (2 users)
        // Act & Assert
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_URL)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should return forbidden for non-admin user")
    void getAllUsers_shouldReturnForbiddenForNonAdmin() throws Exception {
        // Arrange
        String userToken = jwtTokenProvider.generateAccessToken(regularUser);

        // Act & Assert
        mockMvc.perform(get(ApiConstants.ADMIN_BASE_URL + ApiConstants.USERS_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
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
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should add email to whitelist successfully")
    void addToWhitelist_shouldAddEmailSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post(ApiConstants.ADMIN_BASE_URL + ApiConstants.WHITELIST_ADD_URL)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .param("email", TestConstants.TestData.NEW_WHITELIST_EMAIL))
                .andExpect(status().isOk());
    }
}

