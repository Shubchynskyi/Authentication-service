package com.authenticationservice.config;

import com.authenticationservice.service.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    public EmailService emailService() {
        EmailService mock = mock(EmailService.class);
        doNothing().when(mock).sendEmail(anyString(), anyString(), anyString());
        return mock;
    }
}

