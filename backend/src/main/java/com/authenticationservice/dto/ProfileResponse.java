package com.authenticationservice.dto;

import java.util.List;
import com.authenticationservice.model.AuthProvider;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileResponse {
    private String email;
    private String name;
    private List<String> roles;
    private AuthProvider authProvider;
}