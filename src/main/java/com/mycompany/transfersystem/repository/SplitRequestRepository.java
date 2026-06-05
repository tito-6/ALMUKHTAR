package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.SplitRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SplitRequestRepository extends JpaRepository<SplitRequest, Long> {
    List<SplitRequest> findByStatusAndExpiresAtBefore(SplitRequest.SplitStatus status, LocalDateTime now);
    List<SplitRequest> findByInitiatorUser_IdOrderByCreatedAtDesc(Long userId);
}
