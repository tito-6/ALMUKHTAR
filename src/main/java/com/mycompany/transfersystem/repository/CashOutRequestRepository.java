package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CashOutRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashOutRequestRepository extends JpaRepository<CashOutRequest, Long> {
    List<CashOutRequest> findByBranch_IdAndStatusOrderByCreatedAtAsc(Long branchId, String status);
}
