package com.mycompany.transfersystem.dto.escrow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EscrowDisputeRequest {

    @NotBlank
    @Size(max = 80)
    private String reasonCategory;
}
