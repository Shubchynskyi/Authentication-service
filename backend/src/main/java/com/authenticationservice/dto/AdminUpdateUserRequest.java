package com.authenticationservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class AdminUpdateUserRequest {
    private String username;
    private String email;
    private String password;  
    
    private List<String> roles;    
    
    private Boolean isAktiv;     
    private Boolean isBlocked;   
    
    private Integer failedLoginAttempts;  
    private String lastLoginAttempt;      
    private String lastLoginAt;           
    
    private String blockReason;
}