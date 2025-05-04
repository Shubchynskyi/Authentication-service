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
        // Arrange: Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName(TestConstants.UserData.TEST_USERNAME);
        testUser.setRoles(Set.of(new Role(TestConstants.Roles.ROLE_USER)));
        testUser.setEmail(TestConstants.UserData.TEST_EMAIL);
        testUser.setPassword(TestConstants.UserData.ENCODED_PASSWORD);
        testUser.setEnabled(true);

        // Arrange: Setup update request
        updateRequest = new ProfileUpdateRequest();
        updateRequest.setName(TestConstants.UserData.NEW_USERNAME);
        updateRequest.setCurrentPassword(TestConstants.UserData.CURRENT_PASSWORD);
        updateRequest.setPassword(TestConstants.UserData.NEW_PASSWORD);
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
            assertEquals(TestConstants.UserData.NEW_USERNAME, savedUser.getName());
            assertEquals(expectedNewEncodedPassword, savedUser.getPassword());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateProfile_shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest));
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND, ex.getMessage());
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
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
                    () -> profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest));
            assertEquals(TestConstants.ErrorMessages.INCORRECT_PASSWORD, ex.getMessage());
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(passwordEncoder).matches(TestConstants.UserData.CURRENT_PASSWORD, testUser.getPassword());
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
            assertNotNull(profile);
            assertEquals(TestConstants.UserData.TEST_USERNAME, profile.getName());
            assertEquals(TestConstants.UserData.TEST_EMAIL, profile.getEmail());
            assertNotNull(profile.getRoles());
            assertTrue(profile.getRoles().contains(TestConstants.Roles.ROLE_USER));
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
                profileService.getProfile(TestConstants.UserData.TEST_EMAIL)
            );
            assertEquals(TestConstants.ErrorMessages.USER_NOT_FOUND_RUSSIAN, exception.getMessage());
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("Partial Update Tests")
    class PartialUpdateTests {
        @Test
        @DisplayName("Should update name only when password is null")
        void updateProfile_shouldUpdateNameOnly_whenPasswordIsNull() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            updateRequest.setPassword(null);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(userRepository).save(any(User.class));
            assertEquals(TestConstants.UserData.NEW_USERNAME, testUser.getName());
            assertEquals(TestConstants.UserData.ENCODED_PASSWORD, testUser.getPassword());
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Should update password only when name is null")
        void updateProfile_shouldUpdatePasswordOnly_whenNameIsNull() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            updateRequest.setName(null);
            when(passwordEncoder.matches(TestConstants.UserData.CURRENT_PASSWORD, TestConstants.UserData.ENCODED_PASSWORD))
                    .thenReturn(true);
            when(passwordEncoder.encode(TestConstants.UserData.NEW_PASSWORD))
                    .thenReturn(TestConstants.UserData.NEW_ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(passwordEncoder).matches(TestConstants.UserData.CURRENT_PASSWORD, TestConstants.UserData.ENCODED_PASSWORD);
            verify(passwordEncoder).encode(TestConstants.UserData.NEW_PASSWORD);
            verify(userRepository).save(any(User.class));
            assertEquals(TestConstants.UserData.TEST_USERNAME, testUser.getName());
            assertEquals(TestConstants.UserData.NEW_ENCODED_PASSWORD, testUser.getPassword());
        }

        @Test
        @DisplayName("Should not update fields when no changes provided")
        void updateProfile_shouldNotUpdateFields_whenNoChangesProvided() {
            // Arrange
            when(userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL))
                    .thenReturn(Optional.of(testUser));
            updateRequest.setName("   ");
            updateRequest.setPassword("   ");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            profileService.updateProfile(TestConstants.UserData.TEST_EMAIL, updateRequest);

            // Assert
            verify(userRepository).findByEmail(TestConstants.UserData.TEST_EMAIL);
            verify(userRepository).save(any(User.class));
            assertEquals(TestConstants.UserData.TEST_USERNAME, testUser.getName());
            assertEquals(TestConstants.UserData.ENCODED_PASSWORD, testUser.getPassword());
            verifyNoInteractions(passwordEncoder);
        }
    }
}
