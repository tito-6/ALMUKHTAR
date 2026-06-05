package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BranchCashMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BranchCashMovementRepository extends JpaRepository<BranchCashMovement, Long> {
    List<BranchCashMovement> findByBranchIdAndCurrencyAndCreatedAtAfter(Long branchId, String currency, LocalDateTime since);
}
