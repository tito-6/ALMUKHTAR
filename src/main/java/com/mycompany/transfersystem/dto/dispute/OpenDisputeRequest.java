package com.mycompany.transfersystem.dto.dispute;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenDisputeRequest {
    @NotNull private Long transactionId;
    @NotNull private String category;
    private String description;
}
