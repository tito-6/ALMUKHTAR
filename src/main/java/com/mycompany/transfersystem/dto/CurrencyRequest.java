package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRequest {
    
    @NotBlank(message = "Currency code is required")
    @Size(min = 2, max = 10, message = "Currency code must be between 2 and 10 characters")
    private String code;
    
    @NotBlank(message = "Currency name is required")
    @Size(max = 100, message = "Currency name must not exceed 100 characters")
    private String name;
    
    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.0000000001", inclusive = true, message = "Exchange rate must be positive")
    private BigDecimal exchangeRateToUsd;
    
    @DecimalMin(value = "0.0000000001", inclusive = true, message = "Forex buying rate must be positive")
    private BigDecimal forexBuyingToUsd;
    
    @DecimalMin(value = "0.0000000001", inclusive = true, message = "Forex selling rate must be positive")
    private BigDecimal forexSellingToUsd;
    
    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    private String symbol;
    
    private Boolean isActive = true;
    
    private Boolean isManual = false;
    
    private String sourceApi;
}