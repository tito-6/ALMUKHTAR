package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.AssetClass;
import com.mycompany.transfersystem.entity.enums.TradableAssetRiskLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tradable_assets", indexes = {
        @Index(name = "idx_tradable_assets_symbol", columnList = "symbol"),
        @Index(name = "idx_tradable_assets_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradableAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** When null, rule applies to all tenants (subject to country/role filters). */
    @Column(name = "tenant_id")
    private Long tenantId;

    /** ISO country code filter; null = all countries. */
    @Column(name = "country_code", length = 3)
    private String countryCode;

    /**
     * Comma-separated {@link com.mycompany.transfersystem.entity.enums.UserRole} names;
     * blank = any role.
     */
    @Column(name = "allowed_roles_csv", length = 500)
    private String allowedRolesCsv;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String market = "US";

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "min_order_value", precision = 20, scale = 4)
    private BigDecimal minOrderValue;

    @Column(name = "max_order_value", precision = 20, scale = 4)
    private BigDecimal maxOrderValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    @Builder.Default
    private TradableAssetRiskLevel riskLevel = TradableAssetRiskLevel.MEDIUM;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
