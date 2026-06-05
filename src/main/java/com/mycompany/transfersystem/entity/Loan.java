package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "principal_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal principalAmount;

    @Column(nullable = false, length = 5)
    private String currency = "USD";

    @Column(name = "disbursed_at", nullable = false)
    private Instant disbursedAt;

    @Column(name = "term_days", nullable = false)
    private int termDays;

    @Column(name = "apr_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal aprRate;

    @Column(name = "origination_fee", nullable = false, precision = 20, scale = 4)
    private BigDecimal originationFee = BigDecimal.ZERO;

    @Column(name = "monthly_payment", nullable = false, precision = 20, scale = 4)
    private BigDecimal monthlyPayment;

    @Column(name = "outstanding_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal outstandingBalance;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
