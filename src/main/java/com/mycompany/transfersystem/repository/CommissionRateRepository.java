package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommissionRateRepository extends JpaRepository<CommissionRate, Long> {
    Optional<CommissionRate> findByBranchAndCommissionScope(Branch branch, CommissionScope commissionScope);
    Optional<CommissionRate> findByBranch_IdAndCommissionScope(Long branchId, CommissionScope commissionScope);
    List<CommissionRate> findByBranch(Branch branch);
    List<CommissionRate> findByBranch_Id(Long branchId);
    List<CommissionRate> findByCommissionScope(CommissionScope commissionScope);
}
