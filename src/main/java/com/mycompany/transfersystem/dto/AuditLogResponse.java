package com.mycompany.transfersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String action;
    private String username;
    private String userRole;
    private String entity;
    private Long entityId;
    private LocalDateTime createdAt;
}