package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.PlatformRevenueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PlatformRevenueEntryRepository extends JpaRepository<PlatformRevenueEntry, Long> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM PlatformRevenueEntry e WHERE e.createdAt >= :from AND e.createdAt < :to")
    BigDecimal sumAmountBetween(Instant from, Instant to);

    List<PlatformRevenueEntry> findByEventTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            String eventType, Instant from, Instant to);
}
