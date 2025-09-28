package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.BranchFeeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchFeeRateRepository extends JpaRepository<BranchFeeRate, Long> {
    Optional<BranchFeeRate> findFirstByBranch(Branch branch);
    Optional<BranchFeeRate> findFirstByBranch_Id(Long branchId);
}
