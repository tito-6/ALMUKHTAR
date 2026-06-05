package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CashierShiftBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashierShiftBalanceRepository extends JpaRepository<CashierShiftBalance, Long> {
    List<CashierShiftBalance> findByShiftId(Long shiftId);
    Optional<CashierShiftBalance> findByShiftIdAndCurrency(Long shiftId, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CashierShiftBalance b where b.shift.id = :shiftId and b.currency = :currency")
    Optional<CashierShiftBalance> findByShiftIdAndCurrencyForUpdate(@Param("shiftId") Long shiftId,
                                                                    @Param("currency") String currency);
}
