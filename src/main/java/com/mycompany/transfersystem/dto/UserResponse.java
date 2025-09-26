package com.mycompany.transfersystem.dto;

import com.mycompany.transfersystem.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private UserRole role;
    private Long fundId;
    private String fundName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}