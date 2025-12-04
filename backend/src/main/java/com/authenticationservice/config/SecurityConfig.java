package com.authenticationservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.authenticationservice.constants.CorsConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.security.JwtAuthenticationFilter;
import com.authenticationservice.security.RateLimitingFilter;
import com.authenticationservice.service.AuthService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final AuthService authService;

    @Value("${frontend.url}")
    private String frontendUrl;
    
    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(SecurityConstants.API_AUTH_PREFIX).permitAll();
                    auth.requestMatchers(SecurityConstants.API_ADMIN_PREFIX).hasRole("ADMIN");
                    auth.requestMatchers(SecurityConstants.API_PROTECTED_PREFIX).authenticated();
                    auth.requestMatchers(SecurityConstants.ROOT_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.LOGIN_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.REGISTER_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.VERIFY_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.FORGOT_PASSWORD_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.RESET_PASSWORD_PATH).permitAll();
                    auth.requestMatchers("/oauth2/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            try {
                                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                                String email = oauth2User.getAttribute("email");
                                String name = oauth2User.getAttribute("name");

                                // Generate JWT tokens
                                Map<String, String> tokens = authService.handleOAuth2Login(email, name);

                                // Security: Use URL fragment (#) instead of query parameters
                                // Fragments are not sent to server, reducing risk of token exposure in logs
                                // Note: This still has risks (browser history, referrer headers)
                                // Better solution: Use POST redirect or one-time code exchange
                                String accessToken = URLEncoder.encode(tokens.get("accessToken"), StandardCharsets.UTF_8);
                                String refreshToken = URLEncoder.encode(tokens.get("refreshToken"), StandardCharsets.UTF_8);
                                response.sendRedirect(frontendUrl + "/oauth2/success#" +
                                        "accessToken=" + accessToken +
                                        "&refreshToken=" + refreshToken);
                            } catch (Exception e) {
                                // Security: Don't expose error details in URL
                                log.error("OAuth2 login failed", e);
                                response.sendRedirect(frontendUrl + "/oauth2/success?error=authentication_failed");
                            }
                        }))
                .httpBasic(Customizer.withDefaults());

        http.addFilterBefore(rateLimitingFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Build allowed origins list from environment variables
        List<String> allowedOrigins = new java.util.ArrayList<>();
        allowedOrigins.add(frontendUrl);
        
        // Parse additional origins from environment variable (comma-separated)
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            String[] origins = corsAllowedOrigins.split(",");
            for (String origin : origins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    allowedOrigins.add(trimmed);
                }
            }
        }
        
        // Fallback to default localhost URLs if no environment variable is set
        if (allowedOrigins.size() == 1) {
            allowedOrigins.add("http://localhost:3000");
            allowedOrigins.add("http://localhost:5173");
        }
        
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(CorsConstants.ALLOWED_METHODS);
        configuration.setAllowedHeaders(CorsConstants.ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CorsConstants.ALL_PATHS, configuration);
        return source;
    }
}