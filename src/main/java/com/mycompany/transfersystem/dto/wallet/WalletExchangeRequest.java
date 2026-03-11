package com.mycompany.transfersystem.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletExchangeRequest {

    @NotNull
    private String fromCurrency;

    @NotNull
    private String toCurrency;

    @NotNull
    @DecimalMin("0.0001")
    private BigDecimal amount;
}
