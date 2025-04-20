package com.authenticationservice.service;

import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.ProfileResponse;
import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ProfileServiceTest {

    private static final String NEW_USERNAME = "newusername";
    private static final String NEW_PASSWORD = "newPassword";
    private static final String CURRENT_PASSWORD = "currentPassword";
    private static final String NEW_ENCODED_PASSWORD = "newEncodedPassword";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final String INCORRECT_PASSWORD_MESSAGE = "Incorrect current password";
    private static final String USER_NOT_FOUND_RUSSIAN = "Пользователь не найден";

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
        testUser = createTestUser();
        updateRequest = createUpdateRequest();
    }

    @Test
    void getProfile_shouldReturnUserProfile_whenUserExists() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        ProfileResponse profile = profileService.getProfile(TestConstants.TEST_EMAIL);

        // Assert
        assertNotNull(profile);
        assertEquals(TestConstants.TEST_USERNAME, profile.getName());
        assertEquals(TestConstants.TEST_EMAIL, profile.getEmail());
        assertNotNull(profile.getRoles());
        assertTrue(profile.getRoles().contains(TestConstants.ROLE_USER));
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
    }

    @Test
    void getProfile_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            profileService.getProfile(TestConstants.TEST_EMAIL)
        );
        assertEquals(USER_NOT_FOUND_RUSSIAN, exception.getMessage());
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
    }

    @Test
    void updateProfile_shouldUpdateNameAndPassword_whenAllDataValid() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, TestConstants.ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        profileService.updateProfile(TestConstants.TEST_EMAIL, updateRequest);

        // Assert
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
        verify(passwordEncoder).matches(CURRENT_PASSWORD, TestConstants.ENCODED_PASSWORD);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(any(User.class));
        assertEquals(NEW_USERNAME, testUser.getName());
        assertEquals(NEW_ENCODED_PASSWORD, testUser.getPassword());
    }

    @Test
    void updateProfile_shouldThrowException_whenInvalidCurrentPassword() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, TestConstants.ENCODED_PASSWORD)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            profileService.updateProfile(TestConstants.TEST_EMAIL, updateRequest)
        );
        assertEquals(INCORRECT_PASSWORD_MESSAGE, exception.getMessage());
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
        verify(passwordEncoder).matches(CURRENT_PASSWORD, TestConstants.ENCODED_PASSWORD);
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    void updateProfile_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            profileService.updateProfile(TestConstants.TEST_EMAIL, updateRequest)
        );
        assertEquals(USER_NOT_FOUND_MESSAGE, exception.getMessage());
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    void updateProfile_shouldUpdateNameOnly_whenPasswordIsNull() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.of(testUser));
        updateRequest.setPassword(null);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        profileService.updateProfile(TestConstants.TEST_EMAIL, updateRequest);

        // Assert
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
        verify(userRepository).save(any(User.class));
        assertEquals(NEW_USERNAME, testUser.getName());
        assertEquals(TestConstants.ENCODED_PASSWORD, testUser.getPassword());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateProfile_shouldUpdatePasswordOnly_whenNameIsNull() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.of(testUser));
        updateRequest.setName(null);
        when(passwordEncoder.matches(CURRENT_PASSWORD, TestConstants.ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        profileService.updateProfile(TestConstants.TEST_EMAIL, updateRequest);

        // Assert
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
        verify(passwordEncoder).matches(CURRENT_PASSWORD, TestConstants.ENCODED_PASSWORD);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(any(User.class));
        assertEquals(TestConstants.TEST_USERNAME, testUser.getName());
        assertEquals(NEW_ENCODED_PASSWORD, testUser.getPassword());
    }

    @Test
    void updateProfile_shouldNotUpdateFields_whenNoChangesProvided() {
        // Arrange
        when(userRepository.findByEmail(TestConstants.TEST_EMAIL)).thenReturn(Optional.of(testUser));
        updateRequest.setName("   ");
        updateRequest.setPassword("   ");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        profileService.updateProfile(TestConstants.TEST_EMAIL, updateRequest);

        // Assert
        verify(userRepository).findByEmail(TestConstants.TEST_EMAIL);
        verify(userRepository).save(any(User.class));
        assertEquals(TestConstants.TEST_USERNAME, testUser.getName());
        assertEquals(TestConstants.ENCODED_PASSWORD, testUser.getPassword());
        verifyNoInteractions(passwordEncoder);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setName(TestConstants.TEST_USERNAME);
        user.setEmail(TestConstants.TEST_EMAIL);
        user.setPassword(TestConstants.ENCODED_PASSWORD);
        user.setEnabled(true);
        user.setBlocked(false);

        Role userRole = new Role();
        userRole.setName(TestConstants.ROLE_USER);
        user.setRoles(Set.of(userRole));

        return user;
    }

    private ProfileUpdateRequest createUpdateRequest() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setCurrentPassword(CURRENT_PASSWORD);
        request.setPassword(NEW_PASSWORD);
        request.setName(NEW_USERNAME);
        return request;
    }
}
