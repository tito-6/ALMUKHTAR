package com.mycompany.transfersystem.dto.cashier;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class OpenShiftRequest {
    @NotNull
    private Long branchId;
    private Map<String, BigDecimal> openingBalances;
    private String notes;
}
