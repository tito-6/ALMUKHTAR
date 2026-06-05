package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BranchVaultBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchVaultBalanceRepository extends JpaRepository<BranchVaultBalance, Long> {
    List<BranchVaultBalance> findByBranch_Id(Long branchId);

    Optional<BranchVaultBalance> findByBranch_IdAndCurrency(Long branchId, String currency);
}
