package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(ABS(t.amount)), 0) FROM WalletTransaction t " +
           "WHERE t.wallet.id = :walletId AND t.currency = :currency " +
           "AND t.amount < 0 AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumDebitsByWalletAndCurrencyBetween(Long walletId, String currency, Instant start, Instant end);
}
