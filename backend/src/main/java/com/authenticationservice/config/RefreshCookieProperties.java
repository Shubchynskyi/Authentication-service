package com.authenticationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.refresh-cookie")
public class RefreshCookieProperties {
    private String name = "refreshToken";
    private String path = "/api/auth";
    private boolean secure = true;
    private String sameSite = "Strict";
    private String domain;
    private boolean httpOnly = true;
}
