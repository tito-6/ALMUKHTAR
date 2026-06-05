package com.mycompany.transfersystem.dto.trading;

import com.mycompany.transfersystem.entity.enums.PriceAlertKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePriceAlertRequest {
    @NotBlank
    private String symbol;
    @NotNull
    private PriceAlertKind kind;
    private BigDecimal thresholdPrice;
    private BigDecimal thresholdPercent;
    private boolean notifyInApp = true;
    private boolean notifyWhatsApp;
}
