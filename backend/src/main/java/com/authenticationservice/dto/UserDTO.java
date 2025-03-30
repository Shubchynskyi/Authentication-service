package com.authenticationservice.dto;

import com.authenticationservice.model.User;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private boolean enabled;
    private boolean blocked;
    private boolean emailVerified;
    private String lastLoginAt;
    private int failedLoginAttempts;

    public static UserDTO fromUser(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRoles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));
        dto.setEnabled(user.isEnabled());
        dto.setBlocked(user.isBlocked());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setLastLoginAt(user.getLastLoginAt() != null ? 
            user.getLastLoginAt().toString() : null);
        dto.setFailedLoginAttempts(user.getFailedLoginAttempts());
        return dto;
    }
}
