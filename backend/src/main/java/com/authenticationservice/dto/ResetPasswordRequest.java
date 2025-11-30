package com.authenticationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String token;
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!\\-_*?])(?=\\S+$).{8,}$", message = "{validation.password.pattern}")
    private String newPassword;
    private String confirmPassword;
}
