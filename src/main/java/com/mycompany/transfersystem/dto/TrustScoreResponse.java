package com.mycompany.transfersystem.dto;

import com.mycompany.transfersystem.entity.TrustScore;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TrustScoreResponse {
    private Long userId;
    private int score;
    private String tier;
    private BigDecimal feeDiscountPct;

    public static TrustScoreResponse from(TrustScore ts) {
        if (ts == null) {
            return TrustScoreResponse.builder()
                    .score(0)
                    .tier("BRONZE")
                    .feeDiscountPct(BigDecimal.ZERO)
                    .build();
        }
        return TrustScoreResponse.builder()
                .userId(ts.getUser() != null ? ts.getUser().getId() : null)
                .score(ts.getScore())
                .tier(ts.getTier())
                .feeDiscountPct(ts.getFeeDiscountPct())
                .build();
    }
}
