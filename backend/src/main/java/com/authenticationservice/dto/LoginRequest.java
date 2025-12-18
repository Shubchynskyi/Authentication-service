package com.authenticationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * If true, issue a longer refresh token (\"remember this device\").
     * If false or null, refresh token uses default policy.
     */
    private Boolean rememberDevice;

    /**
     * Requested remember duration in days. Supported values: 15/30/60/90.
     * Validated in service layer for exact allowed set.
     */
    @Min(value = 1, message = "rememberDays must be positive")
    @Max(value = 365, message = "rememberDays is too large")
    private Integer rememberDays;
}
