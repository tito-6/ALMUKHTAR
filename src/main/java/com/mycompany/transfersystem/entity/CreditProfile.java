package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "credit_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "credit_score", nullable = false)
    private int creditScore = 0;

    @Column(name = "risk_tier", nullable = false, length = 20)
    private String riskTier = "INELIGIBLE";

    @Column(name = "max_loan_amount_usd", nullable = false, precision = 20, scale = 4)
    private BigDecimal maxLoanAmountUsd = BigDecimal.ZERO;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_components", columnDefinition = "jsonb")
    private Map<String, Object> scoreComponents;
}
