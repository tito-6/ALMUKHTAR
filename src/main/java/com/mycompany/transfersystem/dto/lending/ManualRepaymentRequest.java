package com.mycompany.transfersystem.dto.lending;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ManualRepaymentRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    /** Optional: specific schedule id for partial; if null, apply to next due. */
    private Long scheduleId;
}
