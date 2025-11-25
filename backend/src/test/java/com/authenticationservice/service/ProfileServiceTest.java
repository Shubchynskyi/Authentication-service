package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.ProfileResponse;
import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.model.Role;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Tests")
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private User testUser;
    private ProfileUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Create a fresh test user for each test to ensure isolation
        testUser = createTestUser();
        
        // Create a fresh update request for each test
        updateRequest = createUpdateRequest(
            TestConstants.UserData.NEW_USERNAME,
            TestConstants.UserData.CURRENT_PASSWORD,
            TestConstants.UserData.NEW_PASSWORD
        );
    }
    
    /**
     * Creates a test user with default values
     * 
     * @return User with test data
     */
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setName(TestConstants.UserData.TEST_USERNAME);
        Set<Role> roles = new HashSet<>();
        roles.add(new Role(TestConstants.Roles.ROLE_USER));
        user.setRoles(roles);
        user.setEmail(TestConstants.UserData.TEST_EMAIL);
        user.setPassword(TestConstants.UserData.ENCODED_PASSWORD);
        user.setEnabled(true);
        return user;
    }
    
    /**
     * Creates a profile update request with specified parameters
     * 
     * @param name New username or null
     * @param currentPassword Current password or null
     * @param newPassword New password or null
     * @return Configured ProfileUpdateRequest
     */
    private ProfileUpdateRequest createUpdateRequest(String name, String currentPassword, String newPassword) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(name);
        request.setCurrentPassword(currentPassword);
        request.setPassword(newPassword);
        return request;
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {
        @Test
        @DisplayName("Should update username and password when all data is valid")
        void updateProfile_shouldUpdateUsernameAndPassword_whenAllDataValid() {
            // Arrange
            String expectedNewEncodedPassword = TestConstants.UserData.EXPECTED_NEW_ENCODED_PASSWORD;
            org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);

            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TestConstants.UserData.CURRENT_PASSWORD, TestConstants.UserData.ENCODED_PASSWORD))
                    .thenReturn(true);
            when(passwordEncoder.encode(TestConstants.UserData.NEW_PASSWORD))
                    .thenReturn(expectedNewEncodedPassword);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(passwordEncoder).matches(TestConstants.UserData.CURRENT_PASSWORD, TestConstants.UserData.ENCODED_PASSWORD);
            verify(passwordEncoder).encode(TestConstants.UserData.NEW_PASSWORD);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertEquals(TestConstants.UserData.NEW_USERNAME, savedUser.getName(), 
                         "Username should be updated to new value");
            assertEquals(expectedNewEncodedPassword, savedUser.getPassword(), 
                         "Password should be updated to newly encoded value");
            assertTrue(savedUser.isEnabled(), "User should remain enabled");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateProfile_shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest),
                    "Should throw RuntimeException when user not found");
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, ex.getMessage(),
                         "Exception message should indicate user not found");
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when current password is incorrect")
        void updateProfile_shouldThrowException_whenCurrentPasswordIncorrect() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TestConstants.UserData.CURRENT_PASSWORD, testUser.getPassword()))
                    .thenReturn(false);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest),
                    "Should throw RuntimeException when password is incorrect");
            assertEquals(TestConstants.ErrorMessages.INCORRECT_PASSWORD, ex.getMessage(),
                         "Exception message should indicate incorrect password");
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(passwordEncoder).matches(TestConstants.UserData.CURRENT_PASSWORD, testUser.getPassword());
            verifyNoMoreInteractions(passwordEncoder);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Profile Retrieval Tests")
    class ProfileRetrievalTests {
        @Test
        @DisplayName("Should return user profile when user exists")
        void getProfile_shouldReturnUserProfile_whenUserExists() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));

            // Act
            ProfileResponse profile = profileService.getProfile(TestConstants.UserData.TEST_EMAIL);

            // Assert
            assertNotNull(profile, "Returned profile should not be null");
            assertEquals(TestConstants.UserData.TEST_USERNAME, profile.getName(), 
                         "Profile name should match test user name");
            assertEquals(TestConstants.UserData.TEST_EMAIL, profile.getEmail(), 
                         "Profile email should match test user email");
            assertNotNull(profile.getRoles(), "Roles list should not be null");
            assertTrue(profile.getRoles().contains(TestConstants.Roles.ROLE_USER), 
                       "Roles should include USER role");
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getProfile_shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                profileService.getProfile(TestConstants.UserData.TEST_EMAIL),
                "Should throw RuntimeException when user not found");
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, exception.getMessage(),
                         "Exception message should be in Russian as required");
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("Partial Update Tests")
    class PartialUpdateTests {
        @Test
        @DisplayName("Should update name only when password is null")
        void updateProfile_shouldUpdateNameOnly_whenPasswordIsNull() {
            // Arrange - Create a new request with null password for this specific test
            ProfileUpdateRequest nameOnlyRequest = createUpdateRequest(
                TestConstants.UserData.NEW_USERNAME, null, null
            );
            
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, nameOnlyRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(userRepository).save(any(User.class));
            assertEquals(TestConstants.UserData.NEW_USERNAME, testUser.getName(),
                         "Username should be updated");
            assertEquals(TestConstants.UserData.ENCODED_PASSWORD, testUser.getPassword(),
                         "Password should remain unchanged");
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Should update password only when name is null")
        void updateProfile_shouldUpdatePasswordOnly_whenNameIsNull() {
            // Arrange - Create a new request with null name for this specific test
            ProfileUpdateRequest passwordOnlyRequest = createUpdateRequest(
                null, TestConstants.UserData.CURRENT_PASSWORD, TestConstants.UserData.NEW_PASSWORD
            );
            
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TestConstants.UserData.CURRENT_PASSWORD, TestConstants.UserData.ENCODED_PASSWORD))
                    .thenReturn(true);
            when(passwordEncoder.encode(TestConstants.UserData.NEW_PASSWORD))
                    .thenReturn(TestConstants.UserData.NEW_ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, passwordOnlyRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(passwordEncoder).matches(TestConstants.UserData.CURRENT_PASSWORD, TestConstants.UserData.ENCODED_PASSWORD);
            verify(passwordEncoder).encode(TestConstants.UserData.NEW_PASSWORD);
            verify(userRepository).save(any(User.class));
            assertEquals(TestConstants.UserData.TEST_USERNAME, testUser.getName(),
                         "Username should remain unchanged");
            assertEquals(TestConstants.UserData.NEW_ENCODED_PASSWORD, testUser.getPassword(),
                         "Password should be updated");
        }

        @Test
        @DisplayName("Should not update fields when username is empty")
        void updateProfile_shouldNotUpdateFields_whenUsernameEmpty() {
            // Arrange - Create a request with empty username
            ProfileUpdateRequest emptyNameRequest = createUpdateRequest(
                "   ", null, null
            );
            
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, emptyNameRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(userRepository).save(any(User.class));
            assertEquals(TestConstants.UserData.TEST_USERNAME, testUser.getName(),
                         "Username should remain unchanged when empty input provided");
            assertEquals(TestConstants.UserData.ENCODED_PASSWORD, testUser.getPassword(),
                         "Password should remain unchanged");
            verifyNoInteractions(passwordEncoder);
        }
        
        @Test
        @DisplayName("Should not update fields when password is empty")
        void updateProfile_shouldNotUpdateFields_whenPasswordEmpty() {
            // Arrange - Create a request with empty password
            ProfileUpdateRequest emptyPasswordRequest = createUpdateRequest(
                null, null, "   "
            );
            
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, emptyPasswordRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(userRepository).save(any(User.class));
            assertEquals(TestConstants.UserData.TEST_USERNAME, testUser.getName(),
                         "Username should remain unchanged");
            assertEquals(TestConstants.UserData.ENCODED_PASSWORD, testUser.getPassword(),
                         "Password should remain unchanged when empty input provided");
            verifyNoInteractions(passwordEncoder);
        }
    }
}
