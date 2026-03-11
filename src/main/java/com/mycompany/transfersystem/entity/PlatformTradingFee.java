package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.AssetClass;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "platform_trading_fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformTradingFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @Column(name = "fee_rate_bps", nullable = false)
    private Integer feeRateBps;

    @Column(name = "min_fee_usd", precision = 10, scale = 2)
    private BigDecimal minFeeUsd;

    @Column(name = "max_fee_usd", precision = 10, scale = 2)
    private BigDecimal maxFeeUsd;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;
}
