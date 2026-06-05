package com.mycompany.transfersystem.dto.liquidity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCashTransferOrderRequest {
    @NotNull
    private Long fromBranchId;
    @NotNull
    private Long toBranchId;
    @NotBlank
    private String currency;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    private String notes;
}
