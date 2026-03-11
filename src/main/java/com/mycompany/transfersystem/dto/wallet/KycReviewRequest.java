package com.mycompany.transfersystem.dto.wallet;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycReviewRequest {

    @NotNull
    private Boolean approved;

    private String notes;
    private String rejectionReason;
}
