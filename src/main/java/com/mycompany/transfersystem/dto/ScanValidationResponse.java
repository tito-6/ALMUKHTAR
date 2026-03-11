package com.mycompany.transfersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanValidationResponse {
    private boolean success;
    private Long transactionId;
    private String receiverName;
    private BigDecimal amount;
    private String message;
}
