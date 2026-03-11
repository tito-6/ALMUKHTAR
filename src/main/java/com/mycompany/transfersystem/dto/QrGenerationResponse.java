package com.mycompany.transfersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrGenerationResponse {
    private String qrImageBase64;
    private String tokenHash;
    private Instant expiresAt;
    private Long transactionId;
}
