package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByStatus(Dispute.DisputeStatus status);

    List<Dispute> findByAssignedBranchIdAndStatusIn(Long assignedBranchId, List<Dispute.DisputeStatus> statuses);

    Page<Dispute> findByReporterUserId(Long userId, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.slaDeadline < :now AND d.status IN ('OPEN','UNDER_REVIEW')")
    List<Dispute> findBySlaDeadlineBefore(LocalDateTime now);

    Page<Dispute> findAll(Pageable pageable);
}
