package com.mycompany.transfersystem.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiToolConfirmationRequest {

    @NotNull
    private Long sessionId;

    @NotBlank
    private String confirmationToken;

    @NotNull
    private Boolean confirm;
}
