package com.mycompany.transfersystem.dto.trading;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TradingAssistantChatRequest {
    @NotBlank
    private String message;
}
