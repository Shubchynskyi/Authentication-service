package com.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeAccessModeRequest {
    @NotBlank
    private String mode;

    @NotBlank
    private String password;

    @NotBlank
    private String otpCode;

    private String reason;
}

