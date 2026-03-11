package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSubAccountRequest {
    @NotNull
    private Long parentUserId;
    private Long subUserId;
    private String accountLabel;
    private BigDecimal spendingLimit;
}
