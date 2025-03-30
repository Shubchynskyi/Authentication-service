package com.authenticationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {
    private boolean enabled;
    private String email;
    private String username;
} 