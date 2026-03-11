package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface WalletBalanceRepository extends JpaRepository<WalletBalance, Long> {

    List<WalletBalance> findByWalletIdOrderByCurrencyCode(Long walletId);

    Optional<WalletBalance> findByWalletIdAndCurrencyCode(Long walletId, String currencyCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.wallet.id = :walletId AND wb.currencyCode = :currencyCode")
    Optional<WalletBalance> findByWalletIdAndCurrencyCodeForUpdate(
            @Param("walletId") Long walletId,
            @Param("currencyCode") String currencyCode);
}
