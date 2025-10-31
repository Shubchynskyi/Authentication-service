package com.authenticationservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateUserRolesRequest {
    private List<String> roles;
}
