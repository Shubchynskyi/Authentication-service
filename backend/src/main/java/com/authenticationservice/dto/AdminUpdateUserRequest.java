package com.authenticationservice.dto;

import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String name;
    private String password;
    private Boolean emailVerified;
    private String roles; // todo Добавили роли (строка, разделенная запятыми)
}