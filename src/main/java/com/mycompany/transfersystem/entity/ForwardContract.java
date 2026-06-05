package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "forward_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForwardContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "from_currency", nullable = false, length = 5)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 5)
    private String toCurrency;

    @Column(name = "notional_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal notionalAmount;

    @Column(name = "locked_rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal lockedRate;

    @Column(name = "contract_fee", nullable = false, precision = 20, scale = 4)
    private BigDecimal contractFee = BigDecimal.ZERO;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "spread_profit", precision = 20, scale = 4)
    private BigDecimal spreadProfit;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
