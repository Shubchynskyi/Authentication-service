package com.authenticationservice.dto;

import com.authenticationservice.validation.PasswordValid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String token;
    @PasswordValid
    private String newPassword;
    private String confirmPassword;
}
