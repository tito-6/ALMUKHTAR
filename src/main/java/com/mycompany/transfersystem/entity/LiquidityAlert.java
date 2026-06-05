package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "liquidity_alerts")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiquidityAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;
    @Column(nullable = false, length = 10)
    private String currency;
    @Column(name = "current_balance", precision = 20, scale = 4)
    private BigDecimal currentBalance;
    @Column(name = "threshold_breached", precision = 20, scale = 4)
    private BigDecimal thresholdBreached;
    @Column(length = 500)
    private String message;
    @Builder.Default
    private boolean resolved = false;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    public enum AlertType { LOW_CASH, HIGH_CASH, IMBALANCE, FORECAST_SHORTAGE }
}
