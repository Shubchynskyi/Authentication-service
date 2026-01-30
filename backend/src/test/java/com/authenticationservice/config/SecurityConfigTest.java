package com.authenticationservice.config;

import com.authenticationservice.constants.CorsConstants;
import com.authenticationservice.constants.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", TestConstants.Urls.FRONTEND_URL);
    }

    @Nested
    @DisplayName("CORS Configuration Tests")
    class CorsConfigurationTests {
        @Test
        @DisplayName("Should create CORS configuration with frontend URL and default localhost URLs when corsAllowedOrigins is empty")
        void corsConfigurationSource_shouldCreateCorsConfig_withDefaultLocalhostUrls_whenCorsAllowedOriginsIsEmpty() throws Exception {
            // Arrange
            ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");

            // Act
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            assertNotNull(source);
            assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
            
            // Get configuration directly from UrlBasedCorsConfigurationSource using reflection
            UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
            Field corsConfigurationsField = UrlBasedCorsConfigurationSource.class.getDeclaredField("corsConfigurations");
            corsConfigurationsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CorsConfiguration> corsConfigurations = (Map<String, CorsConfiguration>) corsConfigurationsField.get(urlBasedSource);
            assertNotNull(corsConfigurations, "CORS configurations map should not be null");
            assertFalse(corsConfigurations.isEmpty(), "CORS configurations map should not be empty");
            
            // Get configuration - try ALL_PATHS first, then get the first available
            CorsConfiguration config = corsConfigurations.get(CorsConstants.ALL_PATHS);
            if (config == null) {
                config = corsConfigurations.values().iterator().next();
            }

            // Assert
            assertNotNull(config);
            assertNotNull(config.getAllowedOrigins());
            assertTrue(config.getAllowedOrigins().contains(TestConstants.Urls.FRONTEND_URL));
            assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
            assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
            assertEquals(CorsConstants.ALLOWED_METHODS, config.getAllowedMethods());
            assertEquals(CorsConstants.ALLOWED_HEADERS, config.getAllowedHeaders());
            assertNotNull(config.getAllowCredentials());
            assertTrue(config.getAllowCredentials());
        }

        @Test
        @DisplayName("Should create CORS configuration with additional origins from corsAllowedOrigins")
        void corsConfigurationSource_shouldCreateCorsConfig_withAdditionalOrigins() throws Exception {
            // Arrange
            ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "https://example.com, https://test.com");

            // Act
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            assertNotNull(source);
            assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
            
            // Get configuration directly from UrlBasedCorsConfigurationSource using reflection
            UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
            Field corsConfigurationsField = UrlBasedCorsConfigurationSource.class.getDeclaredField("corsConfigurations");
            corsConfigurationsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CorsConfiguration> corsConfigurations = (Map<String, CorsConfiguration>) corsConfigurationsField.get(urlBasedSource);
            assertNotNull(corsConfigurations, "CORS configurations map should not be null");
            assertFalse(corsConfigurations.isEmpty(), "CORS configurations map should not be empty");
            
            // Get configuration - try ALL_PATHS first, then get the first available
            CorsConfiguration config = corsConfigurations.get(CorsConstants.ALL_PATHS);
            if (config == null) {
                config = corsConfigurations.values().iterator().next();
            }

            // Assert
            assertNotNull(config);
            assertNotNull(config.getAllowedOrigins());
            assertTrue(config.getAllowedOrigins().contains(TestConstants.Urls.FRONTEND_URL));
            assertTrue(config.getAllowedOrigins().contains("https://example.com"));
            assertTrue(config.getAllowedOrigins().contains("https://test.com"));
            assertEquals(CorsConstants.ALLOWED_METHODS, config.getAllowedMethods());
            assertEquals(CorsConstants.ALLOWED_HEADERS, config.getAllowedHeaders());
            assertNotNull(config.getAllowCredentials());
            assertTrue(config.getAllowCredentials());
        }

        @Test
        @DisplayName("Should create CORS configuration with trimmed origins from corsAllowedOrigins")
        void corsConfigurationSource_shouldCreateCorsConfig_withTrimmedOrigins() throws Exception {
            // Arrange
            ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", " https://example.com , https://test.com ");

            // Act
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            assertNotNull(source);
            assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
            
            // Get configuration directly from UrlBasedCorsConfigurationSource using reflection
            UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
            Field corsConfigurationsField = UrlBasedCorsConfigurationSource.class.getDeclaredField("corsConfigurations");
            corsConfigurationsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CorsConfiguration> corsConfigurations = (Map<String, CorsConfiguration>) corsConfigurationsField.get(urlBasedSource);
            assertNotNull(corsConfigurations, "CORS configurations map should not be null");
            assertFalse(corsConfigurations.isEmpty(), "CORS configurations map should not be empty");
            
            // Get configuration - try ALL_PATHS first, then get the first available
            CorsConfiguration config = corsConfigurations.get(CorsConstants.ALL_PATHS);
            if (config == null) {
                config = corsConfigurations.values().iterator().next();
            }

            // Assert
            assertNotNull(config);
            assertNotNull(config.getAllowedOrigins());
            assertTrue(config.getAllowedOrigins().contains("https://example.com"));
            assertTrue(config.getAllowedOrigins().contains("https://test.com"));
            assertFalse(config.getAllowedOrigins().contains(" https://example.com "));
        }

        @Test
        @DisplayName("Should create CORS configuration without default localhost URLs when corsAllowedOrigins has multiple origins")
        void corsConfigurationSource_shouldNotAddDefaultLocalhostUrls_whenCorsAllowedOriginsHasMultipleOrigins() throws Exception {
            // Arrange
            ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "https://example.com, https://test.com");

            // Act
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            assertNotNull(source);
            assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
            
            // Get configuration directly from UrlBasedCorsConfigurationSource using reflection
            UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
            Field corsConfigurationsField = UrlBasedCorsConfigurationSource.class.getDeclaredField("corsConfigurations");
            corsConfigurationsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CorsConfiguration> corsConfigurations = (Map<String, CorsConfiguration>) corsConfigurationsField.get(urlBasedSource);
            assertNotNull(corsConfigurations, "CORS configurations map should not be null");
            assertFalse(corsConfigurations.isEmpty(), "CORS configurations map should not be empty");
            
            // Get configuration - try ALL_PATHS first, then get the first available
            CorsConfiguration config = corsConfigurations.get(CorsConstants.ALL_PATHS);
            if (config == null) {
                config = corsConfigurations.values().iterator().next();
            }

            // Assert
            assertNotNull(config);
            assertNotNull(config.getAllowedOrigins());
            assertEquals(3, config.getAllowedOrigins().size()); // frontendUrl + 2 from corsAllowedOrigins
            assertTrue(config.getAllowedOrigins().contains(TestConstants.Urls.FRONTEND_URL));
            assertTrue(config.getAllowedOrigins().contains("https://example.com"));
            assertTrue(config.getAllowedOrigins().contains("https://test.com"));
        }

        @Test
        @DisplayName("Should ignore empty strings in corsAllowedOrigins")
        void corsConfigurationSource_shouldIgnoreEmptyStrings_inCorsAllowedOrigins() throws Exception {
            // Arrange
            ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "https://example.com, , https://test.com");

            // Act
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            assertNotNull(source);
            assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
            
            // Get configuration directly from UrlBasedCorsConfigurationSource using reflection
            UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
            Field corsConfigurationsField = UrlBasedCorsConfigurationSource.class.getDeclaredField("corsConfigurations");
            corsConfigurationsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CorsConfiguration> corsConfigurations = (Map<String, CorsConfiguration>) corsConfigurationsField.get(urlBasedSource);
            assertNotNull(corsConfigurations, "CORS configurations map should not be null");
            assertFalse(corsConfigurations.isEmpty(), "CORS configurations map should not be empty");
            
            // Get configuration - try ALL_PATHS first, then get the first available
            CorsConfiguration config = corsConfigurations.get(CorsConstants.ALL_PATHS);
            if (config == null) {
                config = corsConfigurations.values().iterator().next();
            }

            // Assert
            assertNotNull(config);
            assertNotNull(config.getAllowedOrigins());
            assertTrue(config.getAllowedOrigins().contains("https://example.com"));
            assertTrue(config.getAllowedOrigins().contains("https://test.com"));
            assertFalse(config.getAllowedOrigins().contains(""));
        }
    }

    @Nested
    @DisplayName("Authentication Manager Tests")
    class AuthenticationManagerTests {
        @Test
        @DisplayName("Should create AuthenticationManager from AuthenticationConfiguration")
        void authenticationManager_shouldCreateAuthenticationManager_fromAuthenticationConfiguration() throws Exception {
            // Arrange
            when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

            // Act
            AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

            // Assert
            assertNotNull(result);
            assertEquals(authenticationManager, result);
            verify(authenticationConfiguration).getAuthenticationManager();
        }
    }

    @Nested
    @DisplayName("Security Filter Chain Tests")
    class SecurityFilterChainTests {
        @Test
        @DisplayName("Should create SecurityFilterChain without throwing exception")
        void securityFilterChain_shouldCreateSecurityFilterChain_withoutThrowingException() throws Exception {
            // Arrange
            HttpSecurity http = mock(HttpSecurity.class);
            DefaultSecurityFilterChain securityFilterChain = mock(DefaultSecurityFilterChain.class);

            when(http.cors(any())).thenReturn(http);
            when(http.csrf(any())).thenReturn(http);
            when(http.sessionManagement(any())).thenReturn(http);
            when(http.headers(any())).thenReturn(http);
            when(http.exceptionHandling(any())).thenReturn(http);
            when(http.authorizeHttpRequests(any())).thenReturn(http);
            when(http.oauth2Login(any())).thenReturn(http);
            when(http.httpBasic(any())).thenReturn(http);
            when(http.addFilterBefore(any(), any())).thenReturn(http);
            when(http.addFilterAfter(any(), any())).thenReturn(http);
            when(http.build()).thenReturn(securityFilterChain);

            // Act & Assert
            assertDoesNotThrow(() -> {
                SecurityFilterChain result = securityConfig.securityFilterChain(http);
                assertNotNull(result);
            });

            verify(http).cors(any());
            verify(http).csrf(any());
            verify(http).sessionManagement(any());
            verify(http).headers(any());
            verify(http).exceptionHandling(any());
            verify(http).authorizeHttpRequests(any());
            verify(http).oauth2Login(any());
            verify(http).httpBasic(any());
            verify(http, atLeastOnce()).addFilterBefore(any(), any());
            verify(http).build();
        }
    }
}
