package com.mycompany.transfersystem.dto.wallet;

import com.mycompany.transfersystem.entity.CashOutRequest;

import java.math.BigDecimal;
import java.time.Instant;

public record CashOutResponse(
        Long id,
        Long walletId,
        Long branchId,
        Long cashierId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant completedAt
) {
    public static CashOutResponse from(CashOutRequest request) {
        return new CashOutResponse(
                request.getId(),
                request.getWallet().getId(),
                request.getBranch().getId(),
                request.getCashier() != null ? request.getCashier().getId() : null,
                request.getAmount(),
                request.getCurrency(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getCompletedAt());
    }
}
