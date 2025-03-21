package com.authenticationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String name;
    private String password; // Новый пароль (может быть пустым)
    private String currentPassword; // Текущий пароль (обязателен при смене пароля)
}