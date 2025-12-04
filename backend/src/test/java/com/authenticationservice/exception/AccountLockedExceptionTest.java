package com.authenticationservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountLockedException Tests")
class AccountLockedExceptionTest {

    @Test
    @DisplayName("Should create exception with lock seconds and return correct value")
    void constructor_shouldCreateException_withLockSeconds() {
        // Arrange
        long lockSeconds = 300L;

        // Act
        AccountLockedException exception = new AccountLockedException(lockSeconds);

        // Assert
        assertNotNull(exception);
        assertEquals("Account is temporarily locked", exception.getMessage());
        assertEquals(lockSeconds, exception.getLockSeconds());
    }

    @Test
    @DisplayName("Should create exception with zero lock seconds")
    void constructor_shouldCreateException_withZeroLockSeconds() {
        // Arrange
        long lockSeconds = 0L;

        // Act
        AccountLockedException exception = new AccountLockedException(lockSeconds);

        // Assert
        assertNotNull(exception);
        assertEquals(0L, exception.getLockSeconds());
    }

    @Test
    @DisplayName("Should create exception with large lock seconds value")
    void constructor_shouldCreateException_withLargeLockSeconds() {
        // Arrange
        long lockSeconds = Long.MAX_VALUE;

        // Act
        AccountLockedException exception = new AccountLockedException(lockSeconds);

        // Assert
        assertNotNull(exception);
        assertEquals(Long.MAX_VALUE, exception.getLockSeconds());
    }

    @Test
    @DisplayName("Should create exception with negative lock seconds")
    void constructor_shouldCreateException_withNegativeLockSeconds() {
        // Arrange
        long lockSeconds = -100L;

        // Act
        AccountLockedException exception = new AccountLockedException(lockSeconds);

        // Assert
        assertNotNull(exception);
        assertEquals(-100L, exception.getLockSeconds());
    }
}

