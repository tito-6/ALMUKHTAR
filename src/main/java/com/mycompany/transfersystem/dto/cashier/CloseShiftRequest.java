package com.mycompany.transfersystem.dto.cashier;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class CloseShiftRequest {
    @NotNull
    private Long shiftId;
    private Map<String, BigDecimal> countedBalances;
    private String notes;
}
