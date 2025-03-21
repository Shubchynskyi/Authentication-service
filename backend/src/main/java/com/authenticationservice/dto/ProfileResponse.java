package com.authenticationservice.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileResponse {
    private String email;
    private String name;
    private List<String> roles;
}