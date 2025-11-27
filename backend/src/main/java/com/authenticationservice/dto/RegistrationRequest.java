package com.authenticationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    private String email;
    private String name;
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Password must be at least 8 characters long and contain at least one digit, one uppercase letter, one lowercase letter, and one special character")
    private String password;

    public RegistrationRequest() {
    }

    public RegistrationRequest(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }

}