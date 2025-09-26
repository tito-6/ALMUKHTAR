package com.mycompany.transfersystem.dto;

import com.mycompany.transfersystem.entity.enums.FundStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundRequest {
    
    @NotBlank(message = "Fund name is required")
    private String name;
    
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be non-negative")
    private BigDecimal balance;
    
    @NotNull(message = "Status is required")
    private FundStatus status;
}