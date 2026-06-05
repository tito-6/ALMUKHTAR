package com.mycompany.transfersystem.dto.liquidity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BranchCashAdjustmentRequest {

    @NotNull
    private Long branchId;

    @NotBlank
    private String currency;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal availableBalance;

    @DecimalMin("0.00")
    private BigDecimal lowCashThreshold;

    @DecimalMin("0.00")
    private BigDecimal highCashThreshold;

    private String note;
}
