package com.mycompany.transfersystem.dto.savings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSavingsGoalRequest {
    @NotBlank private String name;
    @NotNull @Positive private BigDecimal targetAmount;
    @NotBlank private String currency;
    private LocalDate deadline;
    private boolean autoSweep;
}
