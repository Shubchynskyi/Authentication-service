package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AllowedEmailRepository allowedEmailRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private User testUser;
    private AdminUpdateUserRequest updateRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Arrange: Setup test user
        testUser = createTestUser();
        userRole = createRole(TestConstants.Roles.ROLE_USER);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);

        // Arrange: Setup update request
        updateRequest = createUpdateRequest();

        // Setup service configuration
        ReflectionTestUtils.setField(adminService, "frontendUrl", TestConstants.Urls.FRONTEND_URL);
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {
        @Test
        @DisplayName("Should return page of users when admin is authenticated")
        void getAllUsers_shouldReturnPageOfUsers_whenAdminAuthenticated() {
            // Arrange
            setupAdminAuthentication(TestConstants.UserData.ADMIN_EMAIL);
            Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser));
            when(userRepository.findByEmailNot(anyString(), any(Pageable.class)))
                    .thenReturn(userPage);

            // Act
            Page<UserDTO> users = adminService.getAllUsers(mock(Pageable.class), null);

            // Assert
            assertNotNull(users, "Returned user page should not be null");
            assertEquals(1, users.getTotalElements(), "Page should contain one user");
            assertEquals(testUser.getName(), users.getContent().get(0).getUsername(), "Username should match test user");
            verify(userRepository).findByEmailNot(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should update user when user exists")
        void updateUser_shouldUpdateUser_whenUserExists() {
            // Arrange - Create update request without username to preserve it
            AdminUpdateUserRequest preserveUsernameRequest = createUpdateRequest(
                null, // Don't update username
                TestConstants.UserData.TEST_EMAIL, 
                List.of(TestConstants.Roles.ROLE_USER), 
                true, false, null
            );
            
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User savedUser = invocation.getArgument(0);
                        testUser.setBlocked(savedUser.isBlocked());
                        testUser.setBlockReason(savedUser.getBlockReason());
                        testUser.setEnabled(savedUser.isEnabled());
                        testUser.setName(savedUser.getName());
                        return testUser;
                    });

            // Act
            UserDTO updatedUser = adminService.updateUser(1L, preserveUsernameRequest);

            // Assert
            assertNotNull(updatedUser, "Updated user should not be null");
            assertEquals(TestConstants.UserData.TEST_USERNAME, updatedUser.getUsername(), "Username should be preserved");
            assertEquals(TestConstants.UserData.TEST_EMAIL, updatedUser.getEmail(), "Email should be preserved");
            assertTrue(updatedUser.isEnabled(), "User should be enabled");
            assertFalse(updatedUser.isBlocked(), "User should not be blocked");
            assertNull(updatedUser.getBlockReason(), "Block reason should be null");
            verify(userRepository).findById(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateUser_shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findById(1L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> adminService.updateUser(1L, updateRequest),
                "Should throw RuntimeException when user not found");
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should block user when block requested")
        void updateUser_shouldBlockUser_whenBlockRequested() {
            // Arrange
            updateRequest.setIsBlocked(true);
            updateRequest.setBlockReason("Test block reason");
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                    .thenReturn(testUser);

            // Act
            UserDTO updatedUser = adminService.updateUser(1L, updateRequest);

            // Assert
            assertNotNull(updatedUser, "Updated user should not be null");
            assertTrue(updatedUser.isBlocked(), "User should be blocked");
            assertEquals("Test block reason", updatedUser.getBlockReason(), "Block reason should match");
            verify(userRepository).findById(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should unblock user when unblock requested")
        void updateUser_shouldUnblockUser_whenUnblockRequested() {
            // Arrange
            testUser.setBlocked(true);
            testUser.setBlockReason("Previous reason");
            updateRequest.setIsBlocked(false);
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                    .thenReturn(testUser);

            // Act
            UserDTO updatedUser = adminService.updateUser(1L, updateRequest);

            // Assert
            assertNotNull(updatedUser, "Updated user should not be null");
            assertFalse(updatedUser.isBlocked(), "User should be unblocked");
            assertNull(updatedUser.getBlockReason(), "Block reason should be null");
            verify(userRepository).findById(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when admin tries to block self")
        void updateUser_shouldThrowException_whenAdminTriesToBlockSelf() {
            // Arrange
            Role adminRole = createRole(TestConstants.Roles.ROLE_ADMIN);
            testUser.setEmail(TestConstants.UserData.ADMIN_EMAIL);
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            testUser.setRoles(adminRoles);
            updateRequest.setIsBlocked(true);
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> adminService.updateUser(1L, updateRequest),
                "Should throw RuntimeException when admin tries to block self");
            verify(userRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Whitelist Management Tests")
    class WhitelistManagementTests {
        @Test
        @DisplayName("Should succeed when adding new email to whitelist")
        void addToWhitelist_shouldSucceed_whenEmailNotExists() {
            // Arrange
            String email = "newwhitelist@example.com";
            when(allowedEmailRepository.findByEmail(email))
                    .thenReturn(Optional.empty());
            when(allowedEmailRepository.save(any(AllowedEmail.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act & Assert
            assertDoesNotThrow(() -> adminService.addToWhitelist(email));
            verify(allowedEmailRepository).save(any(AllowedEmail.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists in whitelist")
        void addToWhitelist_shouldThrowException_whenEmailAlreadyExists() {
            // Arrange
            String email = "existing@example.com";
            AllowedEmail existing = new AllowedEmail(email);
            when(allowedEmailRepository.findByEmail(email))
                    .thenReturn(Optional.of(existing));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> adminService.addToWhitelist(email));
            assertEquals(TestConstants.ErrorMessages.EMAIL_ALREADY_IN_WHITELIST, ex.getMessage());
        }

        @Test
        @DisplayName("Should succeed when removing email from whitelist")
        void removeFromWhitelist_shouldSucceed_whenEmailExists() {
            // Arrange
            String email = "remove@example.com";
            AllowedEmail existing = new AllowedEmail(email);
            when(allowedEmailRepository.findByEmail(email))
                    .thenReturn(Optional.of(existing));
            doNothing().when(allowedEmailRepository).delete(existing);

            // Act & Assert
            assertDoesNotThrow(() -> adminService.removeFromWhitelist(email));
            verify(allowedEmailRepository).delete(existing);
        }

        @Test
        @DisplayName("Should throw exception when email not found in whitelist")
        void removeFromWhitelist_shouldThrowException_whenEmailNotFound() {
            // Arrange
            String email = "notfound@example.com";
            when(allowedEmailRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> adminService.removeFromWhitelist(email));
            assertEquals(TestConstants.ErrorMessages.EMAIL_NOT_IN_WHITELIST, ex.getMessage());
        }
    }

    @Nested
    @DisplayName("User Retrieval Tests")
    class UserRetrievalTests {
        @Test
        @DisplayName("Should return user when user exists")
        void getUserById_shouldReturnUser_whenUserExists() {
            // Arrange
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(testUser));

            // Act
            UserDTO userDTO = adminService.getUserById(1L);

            // Assert
            assertNotNull(userDTO);
            assertEquals(testUser.getEmail(), userDTO.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserById_shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findById(1L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> adminService.getUserById(1L));
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, ex.getMessage());
        }
    }

    @Nested
    @DisplayName("User Creation Tests")
    class UserCreationTests {
        @Test
        @DisplayName("Should succeed when all data is valid")
        void createUser_shouldSucceed_whenAllDataValid() {
            // Arrange
            AdminUpdateUserRequest request = createUserCreationRequest();
            when(userRepository.existsByEmail(request.getEmail()))
                    .thenReturn(false);
            when(roleRepository.findByName(TestConstants.Roles.ROLE_USER))
                    .thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode(anyString()))
                    .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User u = invocation.getArgument(0);
                        u.setId(2L);
                        return u;
                    });
            doNothing().when(emailService)
                    .sendEmail(anyString(), anyString(), anyString());

            // Act & Assert
            assertDoesNotThrow(() -> adminService.createUser(request));
            verify(userRepository).save(any(User.class));
            verify(emailService).sendEmail(eq("create@example.com"), anyString(), 
                contains("Your temporary password:"));
        }

        @Test
        @DisplayName("Should throw exception when user already exists")
        void createUser_shouldThrowException_whenUserAlreadyExists() {
            // Arrange
            AdminUpdateUserRequest request = createUserCreationRequest();
            when(userRepository.existsByEmail(request.getEmail()))
                    .thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> adminService.createUser(request));
            assertEquals(TestConstants.ErrorMessages.USER_ALREADY_EXISTS, ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when role not found")
        void createUser_shouldThrowException_whenRoleNotFound() {
            // Arrange
            AdminUpdateUserRequest request = createUserCreationRequest();
            request.setRoles(Collections.singletonList("ROLE_NONEXISTENT"));
            when(userRepository.existsByEmail(request.getEmail()))
                    .thenReturn(false);
            when(roleRepository.findByName("ROLE_NONEXISTENT"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> adminService.createUser(request));
            assertEquals("Role not found: ROLE_NONEXISTENT", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when email sending fails")
        void createUser_shouldThrowException_whenEmailSendingFails() {
            // Arrange
            AdminUpdateUserRequest request = createUserCreationRequest();
            when(userRepository.existsByEmail(request.getEmail()))
                    .thenReturn(false);
            when(roleRepository.findByName(TestConstants.Roles.ROLE_USER))
                    .thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode(anyString()))
                    .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User u = invocation.getArgument(0);
                        u.setId(3L);
                        return u;
                    });
            doThrow(new RuntimeException("Mail send error"))
                    .when(emailService).sendEmail(anyString(), anyString(), anyString());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> adminService.createUser(request));
            assertTrue(ex.getMessage().contains("Failed to create user: Mail send error"));
        }
    }

    @Nested
    @DisplayName("User Deletion Tests")
    class UserDeletionTests {
        @Test
        @DisplayName("Should succeed when user exists")
        void deleteUser_shouldSucceed_whenUserExists() {
            // Arrange
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).deleteById(1L);

            // Act & Assert
            assertDoesNotThrow(() -> adminService.deleteUser(1L));
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void deleteUser_shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findById(1L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> adminService.deleteUser(1L));
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Whitelist Retrieval Tests")
    class WhitelistRetrievalTests {
        @Test
        @DisplayName("Should return list of whitelisted emails")
        void getWhitelist_shouldReturnListOfEmails() {
            // Arrange
            AllowedEmail a1 = new AllowedEmail("a@example.com");
            AllowedEmail a2 = new AllowedEmail("b@example.com");
            when(allowedEmailRepository.findAll())
                    .thenReturn(Arrays.asList(a1, a2));

            // Act
            List<String> whitelist = adminService.getWhitelist();

            // Assert
            assertNotNull(whitelist);
            assertEquals(2, whitelist.size());
            assertTrue(whitelist.contains("a@example.com"));
            assertTrue(whitelist.contains("b@example.com"));
        }
    }

    @Nested
    @DisplayName("Admin Password Verification Tests")
    class AdminPasswordVerificationTests {
        @Test
        @DisplayName("Should return true when credentials are valid")
        void verifyAdminPassword_shouldReturnTrue_whenCredentialsValid() {
            // Arrange
            Role adminRole = createRole(TestConstants.Roles.ROLE_ADMIN);
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            testUser.setRoles(adminRoles);
            when(userRepository.findByEmail(TestConstants.UserData.ADMIN_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", testUser.getPassword()))
                    .thenReturn(true);

            // Act
            boolean result = adminService.verifyAdminPassword(TestConstants.UserData.ADMIN_EMAIL, "correctPassword");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void verifyAdminPassword_shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.verifyAdminPassword(TestConstants.UserData.TEST_EMAIL, "anyPassword"));
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user has insufficient permissions")
        void verifyAdminPassword_shouldThrowException_whenInsufficientPermissions() {
            // Arrange
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(userRole);
            testUser.setRoles(userRoles);
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.verifyAdminPassword(TestConstants.UserData.TEST_EMAIL, "anyPassword"));
            assertEquals(TestConstants.ErrorMessages.INSUFFICIENT_PERMISSIONS, ex.getMessage());
        }

        @Test
        @DisplayName("Should return false when password is invalid")
        void verifyAdminPassword_shouldReturnFalse_whenPasswordInvalid() {
            // Arrange
            Role adminRole = createRole(TestConstants.Roles.ROLE_ADMIN);
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            testUser.setRoles(adminRoles);
            when(userRepository.findByEmail(TestConstants.UserData.ADMIN_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", testUser.getPassword()))
                    .thenReturn(false);

            // Act
            boolean result = adminService.verifyAdminPassword(TestConstants.UserData.ADMIN_EMAIL, "wrongPassword");

            // Assert
            assertFalse(result);
        }
    }

    // ************** Helper Methods **************

    /**
     * Creates a test user with default values
     * 
     * @return User with default test data
     */
    private User createTestUser() {
        return createTestUser(1L, TestConstants.UserData.TEST_USERNAME, 
                             TestConstants.UserData.TEST_EMAIL, true, false, null);
    }
    
    /**
     * Creates a test user with specified parameters
     * 
     * @param id User ID
     * @param name Username
     * @param email User email
     * @param enabled Whether user is enabled
     * @param blocked Whether user is blocked
     * @param blockReason Reason for blocking
     * @return User with specified parameters
     */
    private User createTestUser(Long id, String name, String email, 
                               boolean enabled, boolean blocked, String blockReason) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setEnabled(enabled);
        user.setBlocked(blocked);
        user.setBlockReason(blockReason);
        user.setLockTime(null);
        return user;
    }

    /**
     * Creates a role with specified name
     * 
     * @param roleName Name of the role
     * @return Role with specified name
     */
    private Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    /**
     * Creates an update request with default values
     * 
     * @return AdminUpdateUserRequest with default test data
     */
    private AdminUpdateUserRequest createUpdateRequest() {
        return createUpdateRequest(TestConstants.UserData.NEW_USERNAME, 
                                  TestConstants.UserData.TEST_EMAIL, 
                                  List.of(TestConstants.Roles.ROLE_USER), 
                                  true, false, null);
    }
    
    /**
     * Creates an update request with specified parameters
     * 
     * @param username New username
     * @param email New email
     * @param roles List of roles
     * @param isActive Whether user should be active
     * @param isBlocked Whether user should be blocked
     * @param blockReason Reason for blocking
     * @return AdminUpdateUserRequest with specified parameters
     */
    private AdminUpdateUserRequest createUpdateRequest(String username, String email, 
                                                      List<String> roles, boolean isActive,
                                                      boolean isBlocked, String blockReason) {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setRoles(roles);
        request.setIsAktiv(isActive);
        request.setIsBlocked(isBlocked);
        request.setBlockReason(blockReason);
        return request;
    }

    /**
     * Creates a user creation request with default values
     * 
     * @return AdminUpdateUserRequest for user creation
     */
    private AdminUpdateUserRequest createUserCreationRequest() {
        return createUpdateRequest(TestConstants.UserData.ADMIN_USERNAME,
                                  TestConstants.UserData.CREATE_EMAIL, 
                                  List.of(TestConstants.Roles.ROLE_USER), 
                                  true, false, null);
    }

    /**
     * Sets up admin authentication for testing
     * 
     * @param adminEmail Email of the admin user
     */
    private void setupAdminAuthentication(String adminEmail) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(adminEmail);
        SecurityContextHolder.setContext(securityContext);
    }
}
