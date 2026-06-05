package com.mycompany.transfersystem.dto.trading;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TradingAssistantResponse {
    private String answer;
    private Map<String, Object> draftPriceAlert;
    private Map<String, Object> draftOrder;
    @Builder.Default
    private boolean requiresConfirmation = true;
    private String disclaimer;
}
