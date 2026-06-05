package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CashDrawerMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashDrawerMovementRepository extends JpaRepository<CashDrawerMovement, Long> {
    List<CashDrawerMovement> findByDrawerIdOrderByCreatedAtAsc(Long drawerId);

    List<CashDrawerMovement> findByDrawerIdAndApprovalStatus(Long drawerId, CashDrawerMovement.ApprovalStatus status);
}
