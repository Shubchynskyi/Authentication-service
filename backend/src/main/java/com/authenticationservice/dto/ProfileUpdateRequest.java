package com.authenticationservice.dto;

import com.authenticationservice.validation.PasswordValid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String name;
    @PasswordValid
    private String password;
    private String currentPassword;
}