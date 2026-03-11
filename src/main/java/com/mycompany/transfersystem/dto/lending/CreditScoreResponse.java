package com.mycompany.transfersystem.dto.lending;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class CreditScoreResponse {

    private int creditScore;
    private String riskTier;
    private BigDecimal maxLoanAmountUsd;
    private Instant calculatedAt;
    private Instant nextReviewAt;
    private Map<String, Object> scoreComponents;
}
