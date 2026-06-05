package com.mycompany.transfersystem.dto.split;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateSplitRequest {
    @NotBlank private String title;
    @NotNull @Positive private BigDecimal totalAmount;
    @NotBlank private String currency;
    private LocalDateTime expiresAt;
    @NotNull private List<Long> participantUserIds;
}
