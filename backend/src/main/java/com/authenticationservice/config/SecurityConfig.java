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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.authenticationservice.constants.CorsConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.security.JwtAuthenticationFilter;
import com.authenticationservice.security.RateLimitingFilter;
import com.authenticationservice.security.RefreshTokenCookieService;
import com.authenticationservice.logging.RequestCorrelationFilter;
import com.authenticationservice.logging.HttpRequestLoggingFilter;
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
    private final RequestCorrelationFilter requestCorrelationFilter;
    private final HttpRequestLoggingFilter httpRequestLoggingFilter;
    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    private static final RequestMatcher API_MATCHER = request -> {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        return "/api".equals(requestUri) || requestUri.startsWith("/api/");
    };

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${security.csp:default-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; object-src 'none'}")
    private String contentSecurityPolicy;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Ignore CSRF for all API endpoints
                        // Refresh token cookie (HttpOnly, SameSite=Strict) provides CSRF protection
                        .ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy)))
                .exceptionHandling(handler -> handler
                        // For API requests - return 401, never redirect
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint(),
                                API_MATCHER
                        )
                        // For API requests - return 403 on access denied
                        .defaultAccessDeniedHandlerFor(
                                (request, response, ex) -> {
                                    log.debug("API access denied: {} {} - {}",
                                            request.getMethod(), request.getRequestURI(), ex.getMessage());
                                    response.sendError(HttpStatus.FORBIDDEN.value());
                                },
                                API_MATCHER
                        )
                )
                .authorizeHttpRequests(auth -> {
                    // API endpoints
                    auth.requestMatchers(SecurityConstants.API_AUTH_PREFIX).permitAll();
                    auth.requestMatchers(SecurityConstants.API_PUBLIC_PREFIX).permitAll();
                    auth.requestMatchers(SecurityConstants.API_ADMIN_PREFIX).hasRole("ADMIN");
                    auth.requestMatchers(SecurityConstants.API_PROTECTED_PREFIX).authenticated();

                    // Web/OAuth2 endpoints
                    auth.requestMatchers(SecurityConstants.ROOT_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.LOGIN_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.REGISTER_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.VERIFY_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.FORGOT_PASSWORD_PATH).permitAll();
                    auth.requestMatchers(SecurityConstants.RESET_PASSWORD_PATH).permitAll();
                    auth.requestMatchers("/oauth2/**").permitAll();
                    auth.requestMatchers("/login/oauth2/**").permitAll();

                    // Everything else requires authentication
                    auth.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            try {
                                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                                String email = oauth2User.getAttribute("email");
                                String name = oauth2User.getAttribute("name");

                                Map<String, String> tokens = authService.handleOAuth2Login(email, name);

                                String accessToken = tokens.get(SecurityConstants.ACCESS_TOKEN_KEY);
                                String refreshToken = tokens.get(SecurityConstants.REFRESH_TOKEN_KEY);
                                if (accessToken == null || refreshToken == null) {
                                    throw new IllegalStateException("OAuth2 token generation failed");
                                }

                                String refreshCookie = refreshTokenCookieService.createRefreshTokenCookie(refreshToken).toString();
                                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie);

                                String encodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
                                response.sendRedirect(frontendUrl + "/oauth2/success#" +
                                        "accessToken=" + encodedAccessToken);
                            } catch (com.authenticationservice.exception.InvalidCredentialsException e) {
                                log.warn("OAuth2 login failed: invalid credentials");
                                String encodedError = URLEncoder.encode("invalid_credentials", StandardCharsets.UTF_8);
                                response.sendRedirect(frontendUrl + "/oauth2/success?error=" + encodedError);
                            } catch (Exception e) {
                                log.error("OAuth2 login failed", e);
                                response.sendRedirect(frontendUrl + "/oauth2/success?error=authentication_failed");
                            }
                        }))
                .httpBasic(Customizer.withDefaults());

        // Add filters
        http.addFilterBefore(requestCorrelationFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(httpRequestLoggingFilter, RequestCorrelationFilter.class);
        http.addFilterBefore(rateLimitingFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.debug("API auth entry point: {} {} - {}",
                    request.getMethod(), request.getRequestURI(), authException.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOrigins = getAllowedOrigins();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(CorsConstants.ALLOWED_METHODS);
        configuration.setAllowedHeaders(CorsConstants.ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CorsConstants.ALL_PATHS, configuration);
        return source;
    }

    private List<String> getAllowedOrigins() {
        List<String> allowedOrigins = new java.util.ArrayList<>();
        allowedOrigins.add(frontendUrl);

        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            String[] origins = corsAllowedOrigins.split(",");
            for (String origin : origins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    allowedOrigins.add(trimmed);
                }
            }
        }

        if (allowedOrigins.size() == 1) {
            allowedOrigins.add("http://localhost:3000");
            allowedOrigins.add("http://localhost:5173");
        }
        return allowedOrigins;
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }
}
