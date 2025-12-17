package com.authenticationservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaskedLoginSettingsDTO {
    @NotNull
    private Boolean enabled;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer templateId;

    @NotBlank
    private String password;
}

