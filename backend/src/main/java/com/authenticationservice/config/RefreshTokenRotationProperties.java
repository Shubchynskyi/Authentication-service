package com.authenticationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.refresh-rotation")
public class RefreshTokenRotationProperties {
    private boolean enabled = true;
    private boolean revokeOnReuse = true;
    private int maxFamiliesPerUser = 5;
    private boolean cleanupEnabled = true;
    private int cleanupExpiredAfterDays = 30;
}
