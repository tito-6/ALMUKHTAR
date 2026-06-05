package com.mycompany.transfersystem.dto.ai;

import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSessionSummaryResponse {

    private Long id;
    private AiAgentRole agentRole;
    private Instant createdAt;
    private String externalConversationId;
}
