package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class OfflineTransactionRequest {
    @NotBlank
    private String deviceId;
    @NotNull
    private Long fundId;
    @NotNull
    private Long senderId;
    @NotNull
    private Long receiverId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private Instant offlineTimestamp;
    private String idempotencyKey;
}
