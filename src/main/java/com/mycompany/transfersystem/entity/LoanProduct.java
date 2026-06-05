package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "min_term_days", nullable = false)
    private int minTermDays;

    @Column(name = "max_term_days", nullable = false)
    private int maxTermDays;

    @Column(name = "apr_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal aprRate;

    @Column(name = "origination_fee_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal originationFeePct = BigDecimal.ZERO;

    @Column(name = "late_fee_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    @Column(name = "risk_tier", nullable = false, length = 10)
    private String riskTier;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
