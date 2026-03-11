package com.mycompany.transfersystem.dto;

import com.mycompany.transfersystem.entity.Badge;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BadgeResponse {
    private Long id;
    private String badgeType;
    private Instant awardedAt;

    public static BadgeResponse from(Badge b) {
        return BadgeResponse.builder()
                .id(b.getId())
                .badgeType(b.getBadgeType())
                .awardedAt(b.getAwardedAt())
                .build();
    }
}
