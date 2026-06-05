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

    /** Optional explicit cashier device id; defaults to deviceId when blank. */
    private String cashierDeviceId;

    /** Monotonic per device for replay protection. */
    private Long deviceSequenceNumber;

    /** SHA-256 of canonical payload bytes the device signed. */
    private String signedPayloadHash;

    /** HMAC-SHA256(deviceSecret, signedPayloadHash) hex, when offline.device.hmac-secret is configured. */
    private String deviceSignature;
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
