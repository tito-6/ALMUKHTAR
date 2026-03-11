package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "escrow_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_user_id", nullable = false)
    private User initiatorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_user_id", nullable = false)
    private User beneficiaryUser;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    private String currency = "USD";

    @Column(length = 200)
    private String title;

    @Column(length = 512)
    private String description;

    @Column(name = "condition_type", nullable = false, length = 30)
    private String conditionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_details", columnDefinition = "jsonb")
    private Map<String, Object> conditionDetails;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "daily_fee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal dailyFeeRate = BigDecimal.ZERO;

    @Column(name = "total_fees_collected", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalFeesCollected = BigDecimal.ZERO;

    @Column(name = "funded_at")
    private Instant fundedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
