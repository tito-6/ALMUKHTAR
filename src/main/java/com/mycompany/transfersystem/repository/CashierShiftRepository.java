package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CashierShift;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashierShiftRepository extends JpaRepository<CashierShift, Long> {
    Optional<CashierShift> findByCashierIdAndStatus(Long cashierId, CashierShift.ShiftStatus status);
    List<CashierShift> findByBranchIdAndStatus(Long branchId, CashierShift.ShiftStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CashierShift s where s.cashier.id = :cashierId and s.status = 'OPEN'")
    Optional<CashierShift> findOpenByCashierIdForUpdate(@Param("cashierId") Long cashierId);
}
