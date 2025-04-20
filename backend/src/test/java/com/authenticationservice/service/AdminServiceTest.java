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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        userRole = createUserRole();
        testUser.setRoles(Set.of(userRole));

        // Arrange: Setup update request
        updateRequest = createUpdateRequest();

        // Setup service configuration
        ReflectionTestUtils.setField(adminService, "frontendUrl", TestConstants.FRONTEND_URL);
    }

    // ************** User Management Tests **************

    @Test
    void getAllUsers_shouldReturnPageOfUsers_whenAdminAuthenticated() {
        // Arrange
        setupAdminAuthentication();
        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser));
        when(userRepository.findByEmailNot(anyString(), any(Pageable.class)))
                .thenReturn(userPage);

        // Act
        Page<UserDTO> users = adminService.getAllUsers(mock(Pageable.class), null);

        // Assert
        assertNotNull(users);
        assertEquals(1, users.getTotalElements());
        assertEquals(testUser.getName(), users.getContent().get(0).getUsername());
        verify(userRepository).findByEmailNot(anyString(), any(Pageable.class));
    }

    @Test
    void updateUser_shouldUpdateUser_whenUserExists() {
        // Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    testUser.setBlocked(savedUser.isBlocked());
                    testUser.setBlockReason(savedUser.getBlockReason());
                    testUser.setEnabled(savedUser.isEnabled());
                    return testUser;
                });

        // Act
        UserDTO updatedUser = adminService.updateUser(1L, updateRequest);

        // Assert
        assertNotNull(updatedUser);
        assertEquals(TestConstants.TEST_USERNAME, updatedUser.getUsername());
        assertEquals(TestConstants.TEST_EMAIL, updatedUser.getEmail());
        assertTrue(updatedUser.isEnabled());
        assertFalse(updatedUser.isBlocked());
        assertNull(updatedUser.getBlockReason());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> adminService.updateUser(1L, updateRequest));
        verify(userRepository).findById(1L);
    }

    @Test
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
        assertNotNull(updatedUser);
        assertTrue(updatedUser.isBlocked());
        assertEquals("Test block reason", updatedUser.getBlockReason());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
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
        assertNotNull(updatedUser);
        assertFalse(updatedUser.isBlocked());
        assertNull(updatedUser.getBlockReason());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowException_whenAdminTriesToBlockSelf() {
        // Arrange
        Role adminRole = createAdminRole();
        testUser.setEmail(TestConstants.ADMIN_EMAIL);
        testUser.setRoles(Set.of(adminRole));
        updateRequest.setIsBlocked(true);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> adminService.updateUser(1L, updateRequest));
        verify(userRepository).findById(1L);
    }

    // ************** Whitelist Management Tests **************

    @Test
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
    void addToWhitelist_shouldThrowException_whenEmailAlreadyExists() {
        // Arrange
        String email = "existing@example.com";
        AllowedEmail existing = new AllowedEmail(email);
        when(allowedEmailRepository.findByEmail(email))
                .thenReturn(Optional.of(existing));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> adminService.addToWhitelist(email));
        assertEquals("Email already exists in whitelist", ex.getMessage());
    }

    @Test
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
    void removeFromWhitelist_shouldThrowException_whenEmailNotFound() {
        // Arrange
        String email = "notfound@example.com";
        when(allowedEmailRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> adminService.removeFromWhitelist(email));
        assertEquals("Email not found in whitelist", ex.getMessage());
    }

    // ************** User Retrieval Tests **************

    @Test
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
    void getUserById_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> adminService.getUserById(1L));
        assertEquals("User not found", ex.getMessage());
    }

    // ************** User Creation Tests **************

    @Test
    void createUser_shouldSucceed_whenAllDataValid() {
        // Arrange
        AdminUpdateUserRequest request = createUserCreationRequest();
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);
        when(roleRepository.findByName(TestConstants.ROLE_USER))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString()))
                .thenReturn(TestConstants.ENCODED_PASSWORD);
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
    void createUser_shouldThrowException_whenUserAlreadyExists() {
        // Arrange
        AdminUpdateUserRequest request = createUserCreationRequest();
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> adminService.createUser(request));
        assertEquals("User with this email already exists", ex.getMessage());
    }

    @Test
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
    void createUser_shouldThrowException_whenEmailSendingFails() {
        // Arrange
        AdminUpdateUserRequest request = createUserCreationRequest();
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);
        when(roleRepository.findByName(TestConstants.ROLE_USER))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString()))
                .thenReturn(TestConstants.ENCODED_PASSWORD);
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

    // ************** User Deletion Tests **************

    @Test
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
    void deleteUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> adminService.deleteUser(1L));
        assertEquals("User not found", ex.getMessage());
    }

    // ************** Whitelist Retrieval Tests **************

    @Test
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

    // ************** Admin Password Verification Tests **************

    @Test
    void verifyAdminPassword_shouldReturnTrue_whenCredentialsValid() {
        // Arrange
        Role adminRole = createAdminRole();
        testUser.setRoles(Set.of(adminRole));
        when(userRepository.findByEmail(TestConstants.ADMIN_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", testUser.getPassword()))
                .thenReturn(true);

        // Act
        boolean result = adminService.verifyAdminPassword(TestConstants.ADMIN_EMAIL, "correctPassword");

        // Assert
        assertTrue(result);
    }

    @Test
    void verifyAdminPassword_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> adminService.verifyAdminPassword(TestConstants.TEST_EMAIL, "anyPassword"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void verifyAdminPassword_shouldThrowException_whenInsufficientPermissions() {
        // Arrange
        testUser.setRoles(Set.of(userRole));
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> adminService.verifyAdminPassword(TestConstants.TEST_EMAIL, "anyPassword"));
        assertEquals("Insufficient permissions", ex.getMessage());
    }

    @Test
    void verifyAdminPassword_shouldReturnFalse_whenPasswordInvalid() {
        // Arrange
        Role adminRole = createAdminRole();
        testUser.setRoles(Set.of(adminRole));
        when(userRepository.findByEmail(TestConstants.ADMIN_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword()))
                .thenReturn(false);

        // Act
        boolean result = adminService.verifyAdminPassword(TestConstants.ADMIN_EMAIL, "wrongPassword");

        // Assert
        assertFalse(result);
    }

    // ************** Helper Methods **************

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setName(TestConstants.TEST_USERNAME);
        user.setEmail(TestConstants.TEST_EMAIL);
        user.setEnabled(true);
        user.setBlocked(false);
        user.setBlockReason(null);
        return user;
    }

    private Role createUserRole() {
        Role role = new Role();
        role.setName(TestConstants.ROLE_USER);
        return role;
    }

    private Role createAdminRole() {
        Role role = new Role();
        role.setName(TestConstants.ROLE_ADMIN);
        return role;
    }

    private AdminUpdateUserRequest createUpdateRequest() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername("newusername");
        request.setEmail("new@example.com");
        request.setRoles(Arrays.asList(TestConstants.ROLE_USER));
        request.setIsAktiv(true);
        request.setIsBlocked(false);
        request.setBlockReason(null);
        return request;
    }

    private AdminUpdateUserRequest createUserCreationRequest() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("create@example.com");
        request.setUsername("Create User");
        request.setRoles(Arrays.asList(TestConstants.ROLE_USER));
        request.setIsAktiv(true);
        request.setIsBlocked(false);
        return request;
    }

    private void setupAdminAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TestConstants.ADMIN_EMAIL);
        SecurityContextHolder.setContext(securityContext);
    }
}
