package com.mycompany.transfersystem.dto.family;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateFamilyGroupRequest {
    @NotBlank private String name;
    @Positive private BigDecimal monthlySpendingLimit;
    @NotBlank private String currency;
}
