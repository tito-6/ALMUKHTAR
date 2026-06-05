package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.TopupRequest;
import com.mycompany.transfersystem.entity.enums.TopupRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TopupRequestRepository extends JpaRepository<TopupRequest, Long> {

    List<TopupRequest> findByBranchIdAndStatusOrderByCreatedAtAsc(Long branchId, TopupRequestStatus status);

    long countByBranch_IdAndCashier_IdAndStatusAndCompletedAtAfter(
            Long branchId, Long cashierId, TopupRequestStatus status, Instant after);
}
