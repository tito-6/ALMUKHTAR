package com.mycompany.transfersystem.dto.recurring;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateRecurringTransferRequest {
    @NotNull private Long destinationUserId;
    @NotNull @Positive private BigDecimal amount;
    @NotNull private String currency;
    @NotNull private String frequency;
    private LocalDateTime firstRunAt;
}
