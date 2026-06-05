package com.mycompany.transfersystem.dto.payout;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePayoutReservationRequest {
    @NotNull
    private Long branchId;
    @NotBlank
    private String currency;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private LocalDateTime pickupWindowStart;
    @NotNull
    private LocalDateTime pickupWindowEnd;
    /** Optional; defaults to end of pickup window. */
    private LocalDateTime expiresAt;
}
