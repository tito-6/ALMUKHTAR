package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BranchCashReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface BranchCashReservationRepository extends JpaRepository<BranchCashReservation, Long> {

    @Query("select coalesce(sum(r.amount), 0) from BranchCashReservation r where r.branch.id = :branchId and r.status = :status")
    BigDecimal sumReservedAmountByBranch(@Param("branchId") Long branchId,
                                         @Param("status") BranchCashReservation.ReservationStatus status);

    Optional<BranchCashReservation> findByTransactionId(Long transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from BranchCashReservation r where r.transaction.id = :transactionId")
    Optional<BranchCashReservation> findByTransactionIdForUpdate(@Param("transactionId") Long transactionId);
}
