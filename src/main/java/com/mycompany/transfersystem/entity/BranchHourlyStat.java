package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "branch_hourly_stats")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BranchHourlyStat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "\"hour\"", nullable = false)
    private int hour;
    @Column(name = "transaction_count")
    private int transactionCount;
    @Column(name = "total_volume", precision = 20, scale = 4)
    private BigDecimal totalVolume;
    @Column(name = "avg_wait_minutes", precision = 10, scale = 2)
    private BigDecimal avgWaitMinutes;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
