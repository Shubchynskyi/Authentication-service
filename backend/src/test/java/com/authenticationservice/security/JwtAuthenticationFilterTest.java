package com.authenticationservice.security;

import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private String validToken;
    private String bearerToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        
        Role userRole = new Role();
        userRole.setName(SecurityConstants.ROLE_USER);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
        
        validToken = "valid.jwt.token";
        bearerToken = "Bearer " + validToken;
    }

    @Test
    @DisplayName("Should authenticate user when valid token is provided")
    void doFilterInternal_shouldAuthenticateUser_whenValidTokenProvided() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateAccessToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromAccess(validToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider).validateAccessToken(validToken);
        verify(jwtTokenProvider).getEmailFromAccess(validToken);
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(filterChain).doFilter(request, response);
        
        // Verify authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should be set");
        assertEquals(testUser.getEmail(), authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(SecurityConstants.ROLE_USER)));
    }

    @Test
    @DisplayName("Should continue filter chain when no token is provided")
    void doFilterInternal_shouldContinueFilterChain_whenNoTokenProvided() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateAccessToken(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(filterChain).doFilter(request, response);
        
        // Verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "Authentication should not be set");
    }

    @Test
    @DisplayName("Should continue filter chain when token is invalid")
    void doFilterInternal_shouldContinueFilterChain_whenTokenInvalid() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateAccessToken(validToken)).thenReturn(false);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider).validateAccessToken(validToken);
        verify(jwtTokenProvider, never()).getEmailFromAccess(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(filterChain).doFilter(request, response);
        
        // Verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "Authentication should not be set");
    }

    @Test
    @DisplayName("Should continue filter chain when user is not found")
    void doFilterInternal_shouldContinueFilterChain_whenUserNotFound() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateAccessToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromAccess(validToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider).validateAccessToken(validToken);
        verify(jwtTokenProvider).getEmailFromAccess(validToken);
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(filterChain).doFilter(request, response);
        
        // Verify no authentication is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "Authentication should not be set");
    }

    @Test
    @DisplayName("Should extract token from Bearer header correctly")
    void doFilterInternal_shouldExtractTokenFromBearerHeader() throws Exception {
        // Arrange
        String token = "extracted.token.here";
        String bearer = "Bearer " + token;
        when(request.getHeader("Authorization")).thenReturn(bearer);
        when(jwtTokenProvider.validateAccessToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromAccess(token)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider).validateAccessToken(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle Authorization header without Bearer prefix")
    void doFilterInternal_shouldHandleHeaderWithoutBearerPrefix() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("notBearerToken");
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateAccessToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle empty Authorization header")
    void doFilterInternal_shouldHandleEmptyAuthorizationHeader() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateAccessToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear security context when exception occurs")
    void doFilterInternal_shouldClearSecurityContext_whenExceptionOccurs() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateAccessToken(validToken)).thenThrow(new RuntimeException("Token validation error"));
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        // Verify security context is cleared
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "Security context should be cleared on exception");
    }

    @Test
    @DisplayName("Should set multiple authorities when user has multiple roles")
    void doFilterInternal_shouldSetMultipleAuthorities_whenUserHasMultipleRoles() throws Exception {
        // Arrange
        Role adminRole = new Role();
        adminRole.setName(SecurityConstants.ROLE_ADMIN);
        testUser.getRoles().add(adminRole);
        
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateAccessToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromAccess(validToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should be set");
        assertEquals(2, authentication.getAuthorities().size(), "Should have 2 authorities");
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(SecurityConstants.ROLE_USER)));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(SecurityConstants.ROLE_ADMIN)));
    }

    @Test
    @DisplayName("Should continue filter chain when exception occurs in token validation")
    void doFilterInternal_shouldContinueFilterChain_whenExceptionInTokenValidation() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateAccessToken(validToken)).thenThrow(new RuntimeException("Validation error"));
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when exception occurs in user lookup")
    void doFilterInternal_shouldContinueFilterChain_whenExceptionInUserLookup() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateAccessToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromAccess(validToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenThrow(new RuntimeException("Database error"));
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        // Verify security context is cleared
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "Security context should be cleared on exception");
    }
}

