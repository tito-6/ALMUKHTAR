package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.AssetClass;
import com.mycompany.transfersystem.entity.enums.TradingRiskArchetype;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trading_risk_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingRiskProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "archetype", nullable = false, length = 20)
    @Builder.Default
    private TradingRiskArchetype archetype = TradingRiskArchetype.CONSERVATIVE;

    @Column(name = "max_single_order_usd", nullable = false, precision = 20, scale = 4)
    private BigDecimal maxSingleOrderUsd;

    @Column(name = "daily_trading_cap_usd", nullable = false, precision = 20, scale = 4)
    private BigDecimal dailyTradingCapUsd;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "trading_risk_profile_asset_classes", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", length = 20)
    @Builder.Default
    private Set<AssetClass> allowedAssetClasses = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
