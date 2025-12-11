package com.authenticationservice.dto;

import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.validation.PasswordValid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "Reset token is required")
    private String token;
    
    @NotBlank(message = "{validation.password.required}")
    @PasswordValid
    private String newPassword;
    
    @NotBlank(message = "{validation.password.required}")
    private String confirmPassword;

    @AssertTrue(message = MessageConstants.PASSWORDS_DO_NOT_MATCH)
    public boolean isPasswordsMatch() {
        if (newPassword == null || confirmPassword == null) {
            return false;
        }
        return newPassword.equals(confirmPassword);
    }
}
