package com.authenticationservice.dto;

import com.authenticationservice.validation.PasswordValid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    private String email;
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