package com.mycompany.transfersystem.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHelperResponse {

    private Long sessionId;
    private String assistantReplyMasked;
    private JsonNode toolResult;
    private String elevenLabsAgentIdHint;
    private String riskDisclaimer;
    private String pendingConfirmationToken;
    private List<String> notes;
}
