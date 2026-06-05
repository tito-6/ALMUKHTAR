package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BranchCashInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchCashInventoryRepository extends JpaRepository<BranchCashInventory, Long> {

    List<BranchCashInventory> findByBranchId(Long branchId);

    Optional<BranchCashInventory> findByBranchIdAndCurrency(Long branchId, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from BranchCashInventory i where i.branch.id = :branchId and i.currency = :currency")
    Optional<BranchCashInventory> findByBranchIdAndCurrencyForUpdate(@Param("branchId") Long branchId,
                                                                     @Param("currency") String currency);

    @Query("select i from BranchCashInventory i where i.availableBalance <= i.lowCashThreshold and i.lowCashThreshold > 0")
    List<BranchCashInventory> findLowCashInventories();
}
