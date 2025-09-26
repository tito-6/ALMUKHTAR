package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.AuditLog;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String action, User user, String entity, Long entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setUser(user);
        auditLog.setEntity(entity);
        auditLog.setEntityId(entityId);
        
        auditLogRepository.save(auditLog);
    }
}