package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeUpdateRequest {
    
    @NotNull(message = "New rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rate must be non-negative")
    private BigDecimal newRate;


}
