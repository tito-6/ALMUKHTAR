package com.mycompany.transfersystem.dto.merchant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantPaymentRequest {

    /** QR code payload from merchant (decoded to get merchantId + signature). */
    private String merchantQrData;

    /** Alternative: pay by merchant ID when no QR (e.g. in-app). */
    private Long merchantId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String description;
    private String currency = "USD";
}
