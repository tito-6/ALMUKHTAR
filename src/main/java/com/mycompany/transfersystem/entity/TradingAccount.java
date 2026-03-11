package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.TradingAccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trading_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private TradingAccountType accountType = TradingAccountType.INDIVIDUAL;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "buying_power_usd", nullable = false, precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal buyingPowerUsd = BigDecimal.ZERO;

    @Column(name = "total_portfolio_value", nullable = false, precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal totalPortfolioValue = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
