package com.mycompany.transfersystem.dto.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCampaignRequest {
    @NotBlank private String name;
    @NotNull private String targetAudience;
    @NotBlank private String messageTemplate;
}
