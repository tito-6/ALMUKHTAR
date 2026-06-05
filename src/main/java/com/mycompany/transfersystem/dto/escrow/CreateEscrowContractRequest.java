package com.mycompany.transfersystem.dto.escrow;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateEscrowContractRequest {
    @NotNull
    private Long beneficiaryUserId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotBlank
    private String currency;
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String conditionType;
    private LocalDate releaseDate;
}
