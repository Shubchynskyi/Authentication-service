package com.authenticationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessListUpdateResponse {
    private String message;
    private boolean userBlocked;
    private String reason;
}


