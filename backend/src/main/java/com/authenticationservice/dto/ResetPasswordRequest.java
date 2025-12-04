package com.authenticationservice.dto;

import com.authenticationservice.validation.PasswordValid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "Reset token is required")
    private String token;
    
    @PasswordValid
    private String newPassword;
    
    private String confirmPassword;
}
