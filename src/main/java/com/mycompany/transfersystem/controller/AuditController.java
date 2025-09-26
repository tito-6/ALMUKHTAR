package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.AuditLogResponse;
import com.mycompany.transfersystem.entity.AuditLog;
import com.mycompany.transfersystem.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs() {
        List<AuditLog> auditLogs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        List<AuditLogResponse> response = auditLogs.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByUser(@PathVariable Long userId) {
        List<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<AuditLogResponse> response = auditLogs.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/action/{action}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAction(@PathVariable String action) {
        List<AuditLog> auditLogs = auditLogRepository.findByActionOrderByCreatedAtDesc(action);
        List<AuditLogResponse> response = auditLogs.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private AuditLogResponse convertToDto(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.getId(),
            auditLog.getAction(),
            auditLog.getUser().getUsername(),
            auditLog.getUser().getRole().name(),
            auditLog.getEntity(),
            auditLog.getEntityId(),
            auditLog.getCreatedAt()
        );
    }
}