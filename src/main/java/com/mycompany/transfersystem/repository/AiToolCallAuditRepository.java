package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.AiToolCallAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiToolCallAuditRepository extends JpaRepository<AiToolCallAudit, Long> {

    long countBySession_Id(Long sessionId);
}
