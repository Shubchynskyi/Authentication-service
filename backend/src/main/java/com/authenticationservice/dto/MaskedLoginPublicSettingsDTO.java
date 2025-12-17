package com.authenticationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaskedLoginPublicSettingsDTO {
    private Boolean enabled;
    private Integer templateId;
}


