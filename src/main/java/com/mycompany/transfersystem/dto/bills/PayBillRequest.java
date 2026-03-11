package com.mycompany.transfersystem.dto.bills;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayBillRequest {

    @NotNull
    @Positive
    private Long providerId;

    @NotNull
    private String accountReference;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String currency = "USD";
}
