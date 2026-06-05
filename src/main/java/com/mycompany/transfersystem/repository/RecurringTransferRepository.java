package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.RecurringTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface RecurringTransferRepository extends JpaRepository<RecurringTransfer, Long> {
    @Query("SELECT r FROM RecurringTransfer r WHERE r.status = 'ACTIVE' AND r.nextRunAt <= :now")
    List<RecurringTransfer> findActiveByNextRunAtBefore(LocalDateTime now);

    List<RecurringTransfer> findByOwnerUserIdAndStatusNot(Long userId, RecurringTransfer.RecurringStatus status);
}
