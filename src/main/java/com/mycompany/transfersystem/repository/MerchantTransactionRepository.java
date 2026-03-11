package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.MerchantTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MerchantTransactionRepository extends JpaRepository<MerchantTransaction, Long> {

    List<MerchantTransaction> findByMerchant_IdOrderByCreatedAtDesc(Long merchantId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM MerchantTransaction t WHERE t.merchant.id = :merchantId AND t.createdAt >= :since")
    java.math.BigDecimal sumAmountByMerchantSince(@Param("merchantId") Long merchantId, @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(t.merchantNetAmount), 0) FROM MerchantTransaction t WHERE t.merchant.id = :merchantId AND t.createdAt >= :since AND t.createdAt < :until")
    java.math.BigDecimal sumNetByMerchantBetween(@Param("merchantId") Long merchantId, @Param("since") Instant since, @Param("until") Instant until);
}
