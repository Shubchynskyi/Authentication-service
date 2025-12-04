package com.authenticationservice.dto;

import com.authenticationservice.validation.PasswordValid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @PasswordValid
    private String password;

    public RegistrationRequest() {
    }

    public RegistrationRequest(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }

}