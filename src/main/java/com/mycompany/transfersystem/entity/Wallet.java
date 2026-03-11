package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.KycTier;
import com.mycompany.transfersystem.entity.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_number", nullable = false, unique = true, length = 36)
    private String walletNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_tier", nullable = false, length = 20)
    @Builder.Default
    private KycTier kycTier = KycTier.BASIC;

    @Column(name = "daily_limit", precision = 20, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 20, scale = 4)
    private BigDecimal monthlyLimit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
