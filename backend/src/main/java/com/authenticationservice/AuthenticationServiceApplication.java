package com.authenticationservice;

import com.authenticationservice.service.AdminInitializationService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import com.authenticationservice.config.AdminConfig;

@SpringBootApplication
@EnableConfigurationProperties(AdminConfig.class)
public class AuthenticationServiceApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(AuthenticationServiceApplication.class, args);
        
        AdminInitializationService adminService = context.getBean(AdminInitializationService.class);
        adminService.initializeAdmin();
    }
}