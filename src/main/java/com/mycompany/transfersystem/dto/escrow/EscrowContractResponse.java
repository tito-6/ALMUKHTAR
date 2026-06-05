package com.mycompany.transfersystem.dto.escrow;

import com.mycompany.transfersystem.entity.EscrowContract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EscrowContractResponse(
        Long id,
        Long initiatorUserId,
        Long beneficiaryUserId,
        BigDecimal amount,
        String currency,
        String title,
        String description,
        String conditionType,
        LocalDate releaseDate,
        String status,
        BigDecimal totalFeesCollected,
        Instant fundedAt,
        Instant releasedAt,
        Instant expiresAt,
        Instant createdAt
) {
    public static EscrowContractResponse from(EscrowContract contract) {
        return new EscrowContractResponse(
                contract.getId(),
                contract.getInitiatorUser().getId(),
                contract.getBeneficiaryUser().getId(),
                contract.getAmount(),
                contract.getCurrency(),
                contract.getTitle(),
                contract.getDescription(),
                contract.getConditionType(),
                contract.getReleaseDate(),
                contract.getStatus(),
                contract.getTotalFeesCollected(),
                contract.getFundedAt(),
                contract.getReleasedAt(),
                contract.getExpiresAt(),
                contract.getCreatedAt());
    }
}
