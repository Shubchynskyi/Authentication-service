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

import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final AuthService authService;

    @Value("${frontend.url}")
    private String frontendUrl;

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

                                // Redirect to frontend with tokens
                                response.sendRedirect(frontendUrl + "/oauth2/success?" +
                                        "accessToken=" + tokens.get("accessToken") +
                                        "&refreshToken=" + tokens.get("refreshToken"));
                            } catch (Exception e) {
                                // Redirect to frontend with error
                                response.sendRedirect(frontendUrl + "/oauth2/success?error=" +
                                        URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
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
        // Allow both local dev (5173) and Docker (3000) frontend URLs
        // TODO: move to environment variables
        configuration.setAllowedOrigins(List.of(
                frontendUrl,
                "http://localhost:3000",
                "http://localhost:5173",
                CorsConstants.FRONTEND_URL));
        configuration.setAllowedMethods(CorsConstants.ALLOWED_METHODS);
        configuration.setAllowedHeaders(CorsConstants.ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CorsConstants.ALL_PATHS, configuration);
        return source;
    }
}