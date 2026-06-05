package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Order;
import com.mycompany.transfersystem.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByTradingAccount_IdOrderByCreatedAtDesc(Long tradingAccountId, Pageable pageable);

    List<Order> findByTradingAccount_IdAndStatus(Long tradingAccountId, OrderStatus status);

    @Query("""
            SELECT COALESCE(SUM(o.quantity * o.filledPrice), 0)
            FROM Order o
            WHERE o.tradingAccount.id = :accountId
              AND o.status = 'FILLED'
              AND o.createdAt >= :from
            """)
    BigDecimal sumFilledNotionalUsdSince(@Param("accountId") Long accountId, @Param("from") Instant from);

    long countByTradingAccount_IdAndCreatedAtBetween(Long tradingAccountId, Instant start, Instant end);

    @Query("""
            SELECT COALESCE(SUM(o.platformFee), 0)
            FROM Order o
            WHERE o.tradingAccount.id = :accountId
              AND o.status = 'FILLED'
              AND o.createdAt >= :from AND o.createdAt < :to
            """)
    BigDecimal sumPlatformFeesBetween(@Param("accountId") Long accountId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);
}
