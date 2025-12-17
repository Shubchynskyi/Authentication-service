package com.authenticationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MaskedLoginPublicSettingsResponse {
    private Boolean enabled;
    private Integer templateId;
}

